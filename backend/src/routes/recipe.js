/**
 * POST /api/recipe/scan-text  — OCR text from a recipe photo → structured recipe.
 *
 * The Android app runs ML Kit text recognition on-device (more reliable than
 * sending a 12MP photo to Claude vision), then posts the raw text here. Claude
 * turns it into a clean, structured recipe.
 *
 * Returns: { name, description, cuisine, difficulty, prepTime, cookTime,
 *            servings, ingredients: [string], steps: [string] }
 */
const express = require('express');
const { callClaude, parseJsonObject } = require('../services/claude');

const router = express.Router();

router.post('/scan-text', async (req, res, next) => {
  try {
    const { text } = req.body;
    if (!text || !text.trim()) {
      return res.status(400).json({ error: 'text is required' });
    }

    const prompt = `The following text was extracted via OCR from a photo of a recipe
(could be a cookbook page, a handwritten card, a printout, or a screenshot).
Reconstruct it into a clean, structured recipe.

Rules:
- Fix OCR artefacts and obvious typos.
- Infer a sensible title if one isn't clearly present.
- Separate ingredients from method steps.
- Each ingredient is a single line like "200g chicken breast" or "2 cloves garlic, minced".
- Each step is one instruction. Split run-on paragraphs into discrete steps.
- prepTime / cookTime as short strings like "15 min" or "" if unknown.
- difficulty is one of "Easy", "Medium", "Hard" (best guess).
- cuisine is a single word if identifiable, otherwise "".
- servings is an integer (default 2 if unknown).
- description is one short sentence, or "".

Recipe OCR text:
${text}

Return a JSON object only — no markdown, no explanation:
{
  "name": "Recipe title",
  "description": "One short sentence or empty",
  "cuisine": "Italian or empty",
  "difficulty": "Easy | Medium | Hard",
  "prepTime": "15 min or empty",
  "cookTime": "30 min or empty",
  "servings": 2,
  "ingredients": ["200g chicken breast", "2 cloves garlic, minced"],
  "steps": ["Preheat the oven to 200C.", "Season the chicken."]
}`;

    const out    = await callClaude({ prompt, maxTokens: 2000 });
    const recipe = parseJsonObject(out);

    if (!recipe || !recipe.name) {
      return res.status(422).json({ error: 'Could not read a recipe from that image.' });
    }
    res.json(recipe);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
