/**
 * GET /api/barcode/:code
 *
 * Proxies Open Food Facts so neither client needs to call the public API directly.
 * Returns: { name, brand, category, quantity, imageUrl } or 404 if not found.
 */
const express = require('express');
const axios   = require('axios');
const { CATEGORY_ORDER } = require('../services/categories');

const router = express.Router();

const OFF_BASE = 'https://world.openfoodfacts.org/api/v2/product';

router.get('/:code', async (req, res, next) => {
  try {
    const { code } = req.params;
    if (!/^\d{8,14}$/.test(code)) {
      return res.status(400).json({ error: 'Invalid barcode format' });
    }

    const { data } = await axios.get(`${OFF_BASE}/${code}.json`, {
      params: { fields: 'product_name,brands,categories_tags,quantity,image_url' },
      timeout: 10_000,
    });

    if (data.status !== 1 || !data.product) {
      return res.status(404).json({ error: 'Product not found' });
    }

    const p = data.product;

    // Attempt to map OFF's category tags to our 14-category list
    const rawCats = (p.categories_tags || []).map(t =>
      t.replace(/^en:/, '').replace(/-/g, ' ')
    ).join(' ');

    const category = mapCategory(rawCats);

    res.json({
      name:     p.product_name || null,
      brand:    p.brands       || null,
      category: category       || null,
      quantity: p.quantity     || null,
      imageUrl: p.image_url    || null,
    });
  } catch (err) {
    if (err.response?.status === 404) {
      return res.status(404).json({ error: 'Product not found' });
    }
    next(err);
  }
});

// ── Category mapping ──────────────────────────────────────────────────────────
// Mirror of PantryItem.normalizeCategory in Swift

function mapCategory(raw) {
  const r = raw.toLowerCase();
  if (/\b(fruit|apple|banana|berry|citrus|mango|grape|melon|pear|stone fruit)\b/.test(r)) return 'Fruits';
  if (/\b(vegetable|veggie|salad|herb|leafy|broccoli|carrot|potato|onion|tomato|capsicum|zucchini|mushroom|lettuce|spinach|kale|celery|cucumber|pea|bean|corn)\b/.test(r)) return 'Vegetables';
  if (/\b(meat|beef|chicken|pork|lamb|turkey|fish|seafood|salmon|tuna|prawn|mince|steak|sausage|bacon|deli|ham|salami)\b/.test(r)) return 'Meat & Seafood';
  if (/\b(dairy|milk|cheese|yogurt|yoghurt|cream|butter|egg|margarine)\b/.test(r)) return 'Dairy & Eggs';
  if (/\b(bread|loaf|bakery|bun|roll|muffin|croissant|sourdough|toast|wrap|pita)\b/.test(r)) return 'Bakery & Bread';
  if (/\b(pasta|rice|flour|sugar|oil|vinegar|canned|tin|bean|lentil|chickpea|sauce|stock|broth|soup|salt|spice|herb|seasoning|grain|noodle|dried|pantry)\b/.test(r)) return 'Pantry & Dry Goods';
  if (/\b(frozen|ice cream|gelato)\b/.test(r)) return 'Frozen';
  if (/\b(snack|chip|crisp|biscuit|chocolate|lolly|candy|confection|popcorn|nut|trail mix)\b/.test(r)) return 'Snacks & Confectionery';
  if (/\b(drink|beverage|juice|water|soft drink|soda|cola|tea|coffee|energy drink|sports drink|alcohol|beer|wine|spirit)\b/.test(r)) return 'Beverages';
  if (/\b(sauce|condiment|ketchup|mustard|mayo|mayonnaise|relish|chutney|dressing|marinade|jam|honey|spread|peanut butter|vegemite|miso|soy sauce|hot sauce|bbq)\b/.test(r)) return 'Condiments & Sauces';
  if (/\b(cereal|oat|porridge|muesli|granola|breakfast)\b/.test(r)) return 'Breakfast & Cereals';
  if (/\b(baby|infant|toddler|formula|puree|nappy|diaper)\b/.test(r)) return 'Baby & Toddler';
  if (/\b(vitamin|supplement|protein|health|wellness|medicine|pharmacy)\b/.test(r)) return 'Health & Wellness';
  if (/\b(clean|detergent|dishwash|laundry|bleach|toilet|tissue|paper towel|bin bag|household|spray|wipe)\b/.test(r)) return 'Cleaning & Household';
  return null;
}

module.exports = router;
