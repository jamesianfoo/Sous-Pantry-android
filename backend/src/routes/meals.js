/**
 * Meal-related routes
 *
 * POST /api/meals/generate      — 10 meals from pantry (MealPlanService)
 * POST /api/meals/trending      — 3 trending social recipes
 * POST /api/meals/pantry        — 3 pantry-based picks
 * POST /api/meals/adventurous   — 2 adventurous recipes (50-70% pantry match)
 * POST /api/meals/suggested     — 4 curated highly-rated recipes
 */
const express = require('express');
const { callClaude, parseJsonArray } = require('../services/claude');

const router = express.Router();

// ── Helpers ───────────────────────────────────────────────────────────────────

function pantryList(items) {
  return (items || []).map(i => `- ${i.name}${i.brand ? ` (${i.brand})` : ''}${i.category ? ` [${i.category}]` : ''}`).join('\n');
}

function dietaryPrefix(ctx) {
  return ctx ? `${ctx}\n` : '';
}

// ── 10-meal plan ──────────────────────────────────────────────────────────────

router.post('/generate', async (req, res, next) => {
  try {
    const { pantryItems = [], dietaryContext = '', learnedContext = '' } = req.body;
    if (!pantryItems.length) return res.status(400).json({ error: 'pantryItems is required' });

    const prompt = `${learnedContext}${dietaryPrefix(dietaryContext)}You are Sous Pantry's AI chef for an Australian household.
Based on this pantry, suggest exactly 10 diverse meals with the following difficulty distribution:
- 5 Easy recipes
- 3 Medium recipes
- 2 Hard recipes

Use real, authentic recipes inspired by recipetineats.com and gatherandfeast.com.
All ingredients MUST include quantities (e.g. "2 cloves garlic, minced", "400g chicken thighs").
The "usedPantryItems" field must ONLY list exact names of pantry items the user already has.
Include 4–8 clear step-by-step cooking instructions per recipe.
Vary cuisines: Australian, Asian, European, Middle Eastern, and Latin American.
For "imageQuery", provide 2-3 keywords for a food photo (e.g. "pasta carbonara").

Pantry:
${pantryList(pantryItems)}

Return a JSON array only — no markdown, no explanation:
[{"title":"","description":"one sentence","cuisine":"","prepTime":"e.g. 30 mins","difficulty":"Easy|Medium|Hard","ingredients":["quantity + ingredient"],"usedPantryItems":["exact pantry item names"],"instructions":["Step 1: ..."],"imageQuery":"dish keywords"}]`;

    const text = await callClaude({ prompt, maxTokens: 6000 });
    res.json(parseJsonArray(text));
  } catch (err) {
    next(err);
  }
});

// ── Trending social ───────────────────────────────────────────────────────────

router.post('/trending', async (req, res, next) => {
  try {
    const { dietaryContext = '' } = req.body;

    const prompt = `${dietaryPrefix(dietaryContext)}Generate exactly 3 recipes currently viral on TikTok and Instagram in Australia. Use popular dishes like smash burgers, Dubai chocolate bark, baked feta pasta, birria tacos, or Korean corn dogs. Mix platforms. Use realistic trending stats.
For "imageQuery", provide 2-3 keywords for a food photo (e.g. "smash burger").

Return a JSON array only — no markdown:
[{"title":"","description":"one sentence","cuisine":"","platform":"TikTok|Instagram","prepTime":"","difficulty":"Easy|Medium|Hard","trendingStats":"e.g. 2.3M views","ingredients":["quantity + ingredient"],"imageQuery":"dish keywords"}]`;

    const text = await callClaude({ prompt, maxTokens: 1200 });
    res.json(parseJsonArray(text));
  } catch (err) {
    next(err);
  }
});

// ── Pantry-based picks ────────────────────────────────────────────────────────

router.post('/pantry', async (req, res, next) => {
  try {
    const { pantryItems = [], dietaryContext = '', learnedContext = '' } = req.body;

    const list = pantryItems.length
      ? pantryItems.map(i => `- ${i.name}`).join('\n')
      : 'Empty pantry';

    const prompt = `${learnedContext}${dietaryPrefix(dietaryContext)}You are Sous Pantry's AI chef for an Australian household. Suggest exactly 3 meals the user can realistically cook using mostly what they already have. Use real recipes inspired by recipetineats.com and gatherandfeast.com. All ingredients MUST include quantities. The "usedPantryItems" field must ONLY list exact pantry item names from the list above. Include 4–6 step-by-step cooking instructions per recipe. For "imageQuery", provide 2-3 keywords for a food photo.

Pantry:
${list}

Return a JSON array only — no markdown:
[{"title":"","description":"one sentence","cuisine":"","prepTime":"","difficulty":"Easy|Medium|Hard","ingredients":["quantity + ingredient"],"usedPantryItems":["pantry item names"],"instructions":["Step 1: ..."],"imageQuery":"dish keywords"}]`;

    const text = await callClaude({ prompt, maxTokens: 2000 });
    res.json(parseJsonArray(text));
  } catch (err) {
    next(err);
  }
});

// ── Adventurous (50–70% pantry match) ────────────────────────────────────────

router.post('/adventurous', async (req, res, next) => {
  try {
    const { pantryItems = [], dietaryContext = '', learnedContext = '' } = req.body;

    const list = pantryItems.length
      ? pantryItems.map(i => `- ${i.name}`).join('\n')
      : 'Empty pantry';

    const prompt = `${learnedContext}${dietaryPrefix(dietaryContext)}You are Sous Pantry's AI chef. Suggest exactly 2 exciting, adventurous recipes that use 50-70% of ingredients from this pantry. Be precise about which ingredients they have versus what they need to buy. Use real recipes from recipetineats.com and gatherandfeast.com. For "imageQuery", provide 2-3 keywords for a food photo.

Pantry:
${list}

Return a JSON array only — no markdown:
[{"title":"","description":"one sentence","cuisine":"","prepTime":"","difficulty":"Easy|Medium|Hard","ingredients":[""],"pantryIngredients":["ingredients the user already has"],"missingIngredients":["ingredients to buy"],"matchPercent":65,"imageQuery":"dish keywords"}]`;

    const text = await callClaude({ prompt, maxTokens: 1400 });
    res.json(parseJsonArray(text));
  } catch (err) {
    next(err);
  }
});

// ── Curated suggested (post-wizard) ──────────────────────────────────────────

router.post('/suggested', async (req, res, next) => {
  try {
    const { dietaryContext = '' } = req.body;

    const prompt = `${dietaryPrefix(dietaryContext)}You are a world-class food editor. Suggest exactly 4 highly-rated recipes that home cooks in Australia love. Draw inspiration from recipetineats.com and gatherandfeast.com. Mix difficulty: 2 Easy, 1 Medium, 1 Hard. All ingredients MUST include quantities. Include 4–6 step-by-step cooking instructions. For "imageQuery", provide 2-3 keywords for a food photo.

Return a JSON array only — no markdown:
[{"title":"","description":"one sentence on why it's loved","cuisine":"","prepTime":"","difficulty":"Easy|Medium|Hard","ingredients":["quantity + ingredient"],"usedPantryItems":[],"instructions":["Step 1: ..."],"imageQuery":"dish keywords"}]`;

    const text = await callClaude({ prompt, maxTokens: 2800 });
    res.json(parseJsonArray(text));
  } catch (err) {
    next(err);
  }
});

module.exports = router;
