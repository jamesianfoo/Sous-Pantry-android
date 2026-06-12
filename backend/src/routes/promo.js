/**
 * POST /api/redeem-promo
 *
 * Body: { code: string, appUserId: string }
 *
 * Validates the promo code against promoCodes.json and redeemedCodes.json,
 * then grants a promotional RevenueCat "pro" entitlement for `grantDays` days.
 *
 * All disk writes are serialised through a single in-process mutex to
 * avoid races under concurrent requests.
 */

const express = require('express');
const fs      = require('fs').promises;
const path    = require('path');
const axios   = require('axios');

const router = express.Router();

// ── File paths ───────────────────────────────────────────────────────────────
const PROMO_CODES_FILE    = path.join(__dirname, '..', '..', 'promoCodes.json');
const REDEEMED_CODES_FILE = path.join(__dirname, '..', '..', 'redeemedCodes.json');

// ── Simple async mutex (serialises all redemption work) ──────────────────────
let mutex = Promise.resolve();
function withLock(fn) {
  const next = mutex.then(fn, fn);
  // Swallow rejections on the chain so one failure doesn't poison later waiters.
  mutex = next.catch(() => {});
  return next;
}

// ── File I/O helpers ─────────────────────────────────────────────────────────
async function readJson(file) {
  const raw = await fs.readFile(file, 'utf8');
  return JSON.parse(raw);
}
async function writeJson(file, data) {
  await fs.writeFile(file, JSON.stringify(data, null, 2) + '\n', 'utf8');
}

// ── RevenueCat grant ─────────────────────────────────────────────────────────
async function grantRevenueCatEntitlement(appUserId, grantDays) {
  const secret = process.env.REVENUECAT_SECRET_KEY;
  if (!secret) throw new Error('REVENUECAT_SECRET_KEY not configured');

  const endMs = Date.now() + grantDays * 86400000;
  const url = `https://api.revenuecat.com/v1/subscribers/${encodeURIComponent(appUserId)}/entitlements/pro/promotional`;

  const response = await axios.post(
    url,
    { duration: 'custom', end_time_ms: endMs },
    {
      headers: {
        Authorization: `Bearer ${secret}`,
        'Content-Type': 'application/json',
      },
      validateStatus: () => true, // don't throw on non-2xx — we check status ourselves
    }
  );

  if (response.status !== 200 && response.status !== 201) {
    const msg = typeof response.data === 'object' ? JSON.stringify(response.data) : response.data;
    throw new Error(`RevenueCat API responded ${response.status}: ${msg}`);
  }
}

// ── POST /api/redeem-promo ───────────────────────────────────────────────────
router.post('/', async (req, res) => {
  const rawCode      = (req.body?.code || '').toString().trim().toUpperCase();
  const appUserId    = (req.body?.appUserId || '').toString().trim();

  if (!rawCode || !appUserId) {
    return res.status(400).json({
      error: 'missing_fields',
      message: 'Both code and appUserId are required.',
    });
  }

  try {
    const result = await withLock(async () => {
      // 1. Load codes & find the requested code (case-insensitive)
      const codes = await readJson(PROMO_CODES_FILE);
      const idx   = codes.findIndex(c => c.code.toUpperCase() === rawCode);
      if (idx === -1) {
        return { status: 404, body: { error: 'invalid_code', message: "That code doesn't exist." } };
      }
      const entry = codes[idx];

      // 2. Active?
      if (entry.active !== true) {
        return { status: 400, body: { error: 'code_inactive', message: 'This code is no longer active.' } };
      }

      // 3. Expired?
      if (entry.expiresAt && Date.parse(entry.expiresAt) < Date.now()) {
        return { status: 400, body: { error: 'code_expired', message: 'This code has expired.' } };
      }

      // 4. Already redeemed by this user?
      const redeemed = await readJson(REDEEMED_CODES_FILE);
      const already  = redeemed.some(r =>
        r.appUserId === appUserId && r.code.toUpperCase() === rawCode
      );
      if (already) {
        return { status: 400, body: { error: 'already_redeemed', message: "You've already used this code." } };
      }

      // 5. One-time code already used?
      if (entry.type === 'one_time' && (entry.useCount || 0) >= 1) {
        return { status: 400, body: { error: 'code_used', message: 'This code has already been used.' } };
      }

      // 6. Reusable code with a cap?
      if (entry.type === 'reusable' && entry.maxUses !== null && entry.maxUses !== undefined
          && (entry.useCount || 0) >= entry.maxUses) {
        return { status: 400, body: { error: 'code_limit_reached', message: 'This code has reached its usage limit.' } };
      }

      const grantDays = Number(entry.grantDays) || 30;

      // 7. Grant the RevenueCat entitlement BEFORE touching disk.
      try {
        await grantRevenueCatEntitlement(appUserId, grantDays);
      } catch (err) {
        console.error('[redeem-promo] RevenueCat grant failed:', err.message);
        return { status: 500, body: { error: 'grant_failed', message: 'Something went wrong. Please try again.' } };
      }

      // 8. Persist: append redemption + increment useCount. Writes are serialised by the mutex.
      redeemed.push({
        code: entry.code,
        appUserId,
        redeemedAt: new Date().toISOString(),
      });
      codes[idx] = { ...entry, useCount: (entry.useCount || 0) + 1 };
      await writeJson(REDEEMED_CODES_FILE, redeemed);
      await writeJson(PROMO_CODES_FILE, codes);

      return {
        status: 200,
        body: {
          success: true,
          grantDays,
          message: `Your code has been applied. Enjoy ${grantDays} days of Pro!`,
        },
      };
    });

    return res.status(result.status).json(result.body);
  } catch (err) {
    console.error('[redeem-promo] unhandled error:', err);
    return res.status(500).json({ error: 'grant_failed', message: 'Something went wrong. Please try again.' });
  }
});

module.exports = router;
