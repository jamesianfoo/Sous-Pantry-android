/**
 * POST /api/shopping/generate   — restock list from pantry
 * POST /api/shopping/staples    — staples list for empty pantry
 *
 * Returns: [{ name, category, quantity, priority, reason }]
 */
const express = require('express');
const { callClaude, parseJsonArray } = require('../services/claude');

const router = express.Router();

// ── Restock from pantry ───────────────────────────────────────────────────────

router.post('/generate', async (req, res, next) => {
  try {
    const { pantryItems = [], existingList = [], dietaryContext = '', learnedContext = '' } = req.body;
    if (!pantryItems.length) return res.status(400).json({ error: 'pantryItems required for restock' });

    const pantry   = pantryItems.map(i => `- ${i.name}`).join('\n');
    const existing = existingList.length
      ? `\nAlready on shopping list:\n${existingList.map(i => `- ${i.name}`).join('\n')}`
      : '';

    const prompt = `${learnedContext}${dietaryContext ? dietaryContext + '\n' : ''}You are Sous Pantry's AI chef for an Australian household. Based on the pantry below, suggest a practical weekly shopping list of items that are low, missing, or commonly needed alongside what's in stock. Aim for 10-15 items. Prioritise items that unlock the most meals.${existing}

Pantry:
${pantry}

Return a JSON array only — no markdown:
[{"name":"item name","category":"grocery category","quantity":"e.g. 1 bunch or null","priority":"Essential|Nice to Have","reason":"one short reason"}]`;

    const text = await callClaude({ prompt, maxTokens: 2000 });
    res.json(parseJsonArray(text));
  } catch (err) {
    next(err);
  }
});

// ── Staples for new users ─────────────────────────────────────────────────────

router.post('/staples', async (req, res, next) => {
  try {
    const { existingList = [], dietaryContext = '', learnedContext = '' } = req.body;

    const existing = existingList.length
      ? `\nAlready on list:\n${existingList.map(i => `- ${i.name}`).join('\n')}`
      : '';

    const prompt = `${learnedContext}${dietaryContext ? dietaryContext + '\n' : ''}You are Sous Pantry's AI chef. This user has an empty pantry. Generate a practical Australian household starter pack of 15-20 pantry staples — everyday essentials like eggs, flour, rice, pasta, canned tomatoes, olive oil, butter, onions, garlic, and common spices.${existing}

Return a JSON array only — no markdown:
[{"name":"item name","category":"grocery category","quantity":"e.g. 1 dozen or null","priority":"Essential|Nice to Have","reason":"one short reason"}]`;

    const text = await callClaude({ prompt, maxTokens: 2000 });
    res.json(parseJsonArray(text));
  } catch (err) {
    next(err);
  }
});

module.exports = router;
