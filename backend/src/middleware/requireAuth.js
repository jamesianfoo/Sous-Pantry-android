/**
 * requireAuth — Supabase JWT verification middleware.
 *
 * Expects `Authorization: Bearer <supabase-access-token>` on every request.
 * Populates `req.user` on success; otherwise responds 401 and short-circuits.
 *
 * Used to protect every AI proxy endpoint (/api/ai/*) and the promo-redemption
 * endpoint so the public internet can't burn through our Anthropic credit
 * or flip promotional entitlements.
 */

const supabase = require('../lib/supabase');

async function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing auth token' });
  }

  const token = authHeader.slice('Bearer '.length);

  try {
    const { data: { user }, error } = await supabase.auth.getUser(token);
    if (error || !user) {
      return res.status(401).json({ error: 'Invalid or expired token' });
    }
    req.user = user;
    next();
  } catch (err) {
    console.error('[requireAuth] verification failed:', err);
    return res.status(401).json({ error: 'Auth verification failed' });
  }
}

module.exports = requireAuth;
