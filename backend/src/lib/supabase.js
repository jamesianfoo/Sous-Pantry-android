/**
 * Supabase admin client.
 *
 * Uses the service_role key, so NEVER import this module from any code path
 * that could be reached by an unauthenticated client — only from server-side
 * middleware / routes after auth has been established.
 *
 * Dashboard path to the keys:  Project Settings → API
 *  - SUPABASE_URL              Project URL
 *  - SUPABASE_SERVICE_ROLE_KEY service_role secret (keep OFF the iOS app)
 */

const { createClient } = require('@supabase/supabase-js');

if (!process.env.SUPABASE_URL || !process.env.SUPABASE_SERVICE_ROLE_KEY) {
  console.warn(
    '[supabase] SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY is missing — ' +
    'requireAuth will reject every request until .env is populated.'
  );
}

const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);

module.exports = supabase;
