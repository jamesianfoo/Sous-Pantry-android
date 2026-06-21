/**
 * Thin wrapper around the Anthropic Messages API.
 * All routes use this instead of calling Anthropic directly,
 * so the API key never leaves the server.
 */
const axios = require('axios');

const ENDPOINT = 'https://api.anthropic.com/v1/messages';

/**
 * @param {object} opts
 * @param {string} opts.prompt       - User message text
 * @param {number} [opts.maxTokens]  - Default 2000
 * @param {Array}  [opts.content]    - Override the full content array (for vision)
 * @returns {Promise<string>}        - The text of the first content block
 */
async function callClaude({ prompt, maxTokens = 2000, content }) {
  const key = process.env.ANTHROPIC_API_KEY;
  if (!key || key === 'sk-ant-api03-YOUR_KEY_HERE') {
    throw Object.assign(new Error('ANTHROPIC_API_KEY not configured'), { status: 503 });
  }

  const messages = content
    ? [{ role: 'user', content }]
    : [{ role: 'user', content: prompt }];

  const { data } = await axios.post(
    ENDPOINT,
    { model: 'claude-sonnet-4-6', max_tokens: maxTokens, messages },
    {
      headers: {
        'Content-Type':    'application/json',
        'x-api-key':       key,
        'anthropic-version': '2023-06-01',
      },
      timeout: 180_000,
    }
  );

  return data.content?.[0]?.text ?? '';
}

/**
 * Parse a JSON array out of Claude's response,
 * tolerating markdown fences that Claude sometimes emits.
 */
function parseJsonArray(text) {
  const clean = text.replace(/^```[a-z]*\n?/i, '').replace(/```$/i, '').trim();
  return JSON.parse(clean);
}

/**
 * Parse a JSON object out of Claude's response,
 * tolerating markdown fences that Claude sometimes emits.
 */
function parseJsonObject(text) {
  const clean = text.replace(/^```[a-z]*\n?/i, '').replace(/```$/i, '').trim();
  return JSON.parse(clean);
}

module.exports = { callClaude, parseJsonArray, parseJsonObject };
