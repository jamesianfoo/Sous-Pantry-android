require('dotenv').config({ override: true });
const express = require('express');
const cors    = require('cors');

const identifyRoutes  = require('./routes/identify');
const receiptRoutes   = require('./routes/receipt');
const mealsRoutes     = require('./routes/meals');
const shoppingRoutes  = require('./routes/shopping');
const barcodeRoutes   = require('./routes/barcode');
const promoRoutes     = require('./routes/promo');
const aiRoutes        = require('./routes/ai');

const requireAuth     = require('./middleware/requireAuth');

const app  = express();
const PORT = process.env.PORT || 3000;

// ── Middleware ────────────────────────────────────────────────────────────────
app.use(cors());
app.use(express.json({ limit: '20mb' }));   // large for base64 images

// ── Routes ────────────────────────────────────────────────────────────────────
//
// Legacy routes (identify/receipt/meals/shopping/barcode) still hit Anthropic
// via services/claude.js. They are left in place for backward compatibility
// while the iOS app migrates over to /api/ai/*. New AI calls from the client
// MUST go through /api/ai/* — those routes are gated by Supabase JWT auth
// so the Anthropic key is never exposed to the client.
app.use('/api/identify',      identifyRoutes);
app.use('/api/receipt',       receiptRoutes);
app.use('/api/meals',         mealsRoutes);
app.use('/api/shopping',      shoppingRoutes);
app.use('/api/barcode',       barcodeRoutes);

// Authenticated routes — require a valid Supabase access token.
app.use('/api/ai',            aiRoutes);                // requireAuth applied per-route inside
app.use('/api/redeem-promo',  requireAuth, promoRoutes);

// ── Health check ──────────────────────────────────────────────────────────────
app.get('/health', (_req, res) => res.json({ status: 'ok', version: '1.0.0' }));

// ── Error handler ─────────────────────────────────────────────────────────────
app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ error: err.message || 'Internal server error' });
});

app.listen(PORT, () => {
  console.log(`Sous Pantry API running on http://localhost:${PORT}`);
});
