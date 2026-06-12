/**
 * POST /api/identify
 *
 * Body: { image: "<base64 string>", mediaType: "image/jpeg" }
 * Returns: { name, brand, category, quantity, expiryDate, confidence }
 *
 * Used by the Scanner feature when a user photographs a grocery item.
 */
const express = require('express');
const { callClaude, parseJsonArray } = require('../services/claude');
const { CATEGORY_ORDER } = require('../services/categories');

const router = express.Router();

router.post('/', async (req, res, next) => {
  try {
    const { image, mediaType = 'image/jpeg' } = req.body;
    if (!image) return res.status(400).json({ error: 'image (base64) is required' });

    const categoryList = CATEGORY_ORDER.join(', ');

    const content = [
      {
        type: 'image',
        source: { type: 'base64', media_type: mediaType, data: image },
      },
      {
        type: 'text',
        text: `You are a grocery recognition assistant for an Australian household app.
Identify the food or grocery item in this image.

Rules:
- Provide the full product name (e.g. "Woolworths Full Cream Milk 2L" not "milk").
- For category, choose the single best match from: ${categoryList}.
- For quantity, capture weight or volume if visible (e.g. "500g", "2L") — otherwise null.
- For expiry_date, read any use-by/best-before date visible — return as "YYYY-MM-DD" or null.
- For confidence, return a float 0–1 (1 = certain).
- If you cannot identify a grocery item with confidence ≥ 0.5, return null for name.

Return a JSON object only — no markdown:
{"name":"Full product name or null","brand":"Brand name or null","category":"Category","quantity":"500g or null","expiry_date":"YYYY-MM-DD or null","confidence":0.92}`,
      },
    ];

    const text = await callClaude({ content, maxTokens: 500 });

    let item;
    try {
      item = JSON.parse(text.replace(/^```[a-z]*\n?/i, '').replace(/```$/i, '').trim());
    } catch {
      return res.status(422).json({ error: 'Could not parse AI response' });
    }

    res.json(item);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
