/**
 * POST /api/receipt/image   — base64 image → OCR via Tesseract-like prompt → line items
 * POST /api/receipt/text    — raw page text (eReceipt web sync) → line items
 *
 * Returns: [{ name, quantity, category }]
 */
const express = require('express');
const { callClaude, parseJsonArray } = require('../services/claude');
const { CATEGORY_ORDER } = require('../services/categories');

const router = express.Router();

// ── Shared parsing logic ──────────────────────────────────────────────────────

async function parseReceiptText(receiptText) {
  const categoryList = CATEGORY_ORDER.join(', ');

  const prompt = `The following text was extracted from an Australian grocery receipt.
Identify every food and grocery item purchased.

Rules:
- Include all food, drinks, cleaning products, and household grocery items.
- EXCLUDE: store name, date, time, cashier name, prices, subtotals, GST, loyalty/rewards points, payment method, bag fees, non-grocery services.
- Fix OCR artefacts in product names (e.g. "Wh0le M1lk" → "Whole Milk").
- Expand abbreviations (e.g. "ORG FF MILK 2L" → "Organic Full Fat Milk").
- For quantity: capture weight or volume if shown (e.g. "500g", "2L", "6pk") — otherwise null.
- For category, choose the single best match from: ${categoryList}.
- Use Australian supermarket product naming conventions.

Receipt text:
${receiptText}

Return a JSON array only — no markdown, no explanation:
[{"name":"Full product name","quantity":"500g or null","category":"Category name"}]`;

  const text = await callClaude({ prompt, maxTokens: 2000 });
  return parseJsonArray(text);
}

// ── Routes ────────────────────────────────────────────────────────────────────

// eReceipt web sync — raw page text already extracted by WebView JS
router.post('/text', async (req, res, next) => {
  try {
    const { text } = req.body;
    if (!text || !text.trim()) {
      return res.status(400).json({ error: 'text is required' });
    }

    const items = await parseReceiptText(text);
    if (!items.length) {
      return res.status(422).json({ error: 'No grocery items found in the provided text' });
    }

    res.json(items);
  } catch (err) {
    next(err);
  }
});

// Camera receipt — base64 image; Claude reads it like a document
router.post('/image', async (req, res, next) => {
  try {
    const { image, mediaType = 'image/jpeg' } = req.body;
    if (!image) return res.status(400).json({ error: 'image (base64) is required' });

    const categoryList = CATEGORY_ORDER.join(', ');

    // Pass to Claude vision — ask it to both read AND parse the receipt
    const content = [
      {
        type: 'image',
        source: { type: 'base64', media_type: mediaType, data: image },
      },
      {
        type: 'text',
        text: `This is an Australian grocery receipt.
Extract every food and grocery item purchased.

Rules:
- Include all food, drinks, cleaning products, and household grocery items.
- EXCLUDE: store name, date, time, prices, subtotals, GST, loyalty points, payment method, bag fees.
- Expand abbreviations to full descriptive names.
- For quantity: capture weight or volume if shown — otherwise null.
- For category, choose from: ${categoryList}.

Return a JSON array only — no markdown:
[{"name":"Full product name","quantity":"500g or null","category":"Category name"}]`,
      },
    ];

    const text = await callClaude({ content, maxTokens: 2000 });
    const items = parseJsonArray(text);

    if (!items.length) {
      return res.status(422).json({ error: 'No grocery items found on the receipt' });
    }

    res.json(items);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
