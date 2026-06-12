/**
 * /api/ai/*  — consolidated Anthropic proxy.
 *
 * Every endpoint here is protected by Supabase JWT auth (requireAuth) so
 * the iOS client can call Claude WITHOUT shipping an Anthropic API key.
 * The key lives only in this server's .env.
 *
 * Endpoints:
 *   POST /api/ai/meal-suggestions      meal ideas from pantry + dietary
 *   POST /api/ai/parse-receipt         OCR + categorise a receipt photo
 *   POST /api/ai/scan-pantry           identify items in a pantry photo
 *   POST /api/ai/shopping-suggestions  restock list from pantry + recent meals
 *   POST /api/ai/generate-staples      Aussie household starter staples
 *
 * Keep this file focused on Anthropic calls only. If you need RevenueCat,
 * Supabase data, or other 3rd-party APIs, put them in their own route file.
 */

const express     = require('express');
const Anthropic   = require('@anthropic-ai/sdk');
const requireAuth = require('../middleware/requireAuth');

const router = express.Router();

const anthropic = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });

// ── Meal suggestions ─────────────────────────────────────────────
// POST /api/ai/meal-suggestions
// Body: { pantryItems: [], dietary: string, restrictions: [] }
router.post('/meal-suggestions', requireAuth, async (req, res) => {
  try {
    const { pantryItems, dietary, restrictions } = req.body;

    const message = await anthropic.messages.create({
      model: 'claude-haiku-4-5-20251001',
      max_tokens: 1024,
      system:
        `You are Sous, an AI cooking assistant. ` +
        `Suggest meals using the provided pantry items. ` +
        `Dietary preference: ${dietary || 'none'}. ` +
        `Restrictions: ${restrictions?.join(', ') || 'none'}. ` +
        `Respond in JSON only.`,
      messages: [{
        role: 'user',
        content:
          `Pantry items: ${JSON.stringify(pantryItems)}. ` +
          `Suggest 6 meals with match percentage, ` +
          `ingredients needed, and cook time.`
      }]
    });

    res.json({ result: message.content[0].text });
  } catch (err) {
    console.error('meal-suggestions error:', err);
    res.status(500).json({ error: 'AI request failed' });
  }
});

// ── Receipt parsing ───────────────────────────────────────────────
// POST /api/ai/parse-receipt
// Body: { imageBase64: string, mimeType: string }
router.post('/parse-receipt', requireAuth, async (req, res) => {
  try {
    const { imageBase64, mimeType } = req.body;

    const message = await anthropic.messages.create({
      model: 'claude-sonnet-4-6',
      max_tokens: 1024,
      messages: [{
        role: 'user',
        content: [
          {
            type: 'image',
            source: { type: 'base64', media_type: mimeType, data: imageBase64 }
          },
          {
            type: 'text',
            text:
              'Extract all grocery items from this receipt. ' +
              'Return JSON array of { name, quantity, unit, category }.'
          }
        ]
      }]
    });

    res.json({ result: message.content[0].text });
  } catch (err) {
    console.error('parse-receipt error:', err);
    res.status(500).json({ error: 'AI request failed' });
  }
});

// ── Pantry photo scan ─────────────────────────────────────────────
// POST /api/ai/scan-pantry
// Body: { imageBase64: string, mimeType: string }
router.post('/scan-pantry', requireAuth, async (req, res) => {
  try {
    const { imageBase64, mimeType } = req.body;

    const message = await anthropic.messages.create({
      model: 'claude-sonnet-4-6',
      max_tokens: 1024,
      messages: [{
        role: 'user',
        content: [
          {
            type: 'image',
            source: { type: 'base64', media_type: mimeType, data: imageBase64 }
          },
          {
            type: 'text',
            text:
              'Identify all visible food items in this pantry photo. ' +
              'Return JSON array of { name, quantity, unit, category }.'
          }
        ]
      }]
    });

    res.json({ result: message.content[0].text });
  } catch (err) {
    console.error('scan-pantry error:', err);
    res.status(500).json({ error: 'AI request failed' });
  }
});

// ── Shopping list suggestions ─────────────────────────────────────
// POST /api/ai/shopping-suggestions
// Body: { pantryItems: [], recentMeals: [] }
router.post('/shopping-suggestions', requireAuth, async (req, res) => {
  try {
    const { pantryItems, recentMeals } = req.body;

    const message = await anthropic.messages.create({
      model: 'claude-haiku-4-5-20251001',
      max_tokens: 512,
      messages: [{
        role: 'user',
        content:
          `Current pantry: ${JSON.stringify(pantryItems)}. ` +
          `Recent meals: ${JSON.stringify(recentMeals)}. ` +
          `Suggest items to restock. Return JSON array of ` +
          `{ name, quantity, unit, reason }.`
      }]
    });

    res.json({ result: message.content[0].text });
  } catch (err) {
    console.error('shopping-suggestions error:', err);
    res.status(500).json({ error: 'AI request failed' });
  }
});

// ── Staples generation ────────────────────────────────────────────
// POST /api/ai/generate-staples
// Body: { dietary: string, restrictions: [] }
router.post('/generate-staples', requireAuth, async (req, res) => {
  try {
    const { dietary, restrictions } = req.body;

    const message = await anthropic.messages.create({
      model: 'claude-haiku-4-5-20251001',
      max_tokens: 512,
      messages: [{
        role: 'user',
        content:
          `Generate a list of pantry staples for an Australian household. ` +
          `Dietary: ${dietary || 'none'}. ` +
          `Restrictions: ${restrictions?.join(', ') || 'none'}. ` +
          `Return JSON array of { name, category }.`
      }]
    });

    res.json({ result: message.content[0].text });
  } catch (err) {
    console.error('generate-staples error:', err);
    res.status(500).json({ error: 'AI request failed' });
  }
});

module.exports = router;
