/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */

/*
 * Run with:  node --test docs/consent-callback-page/
 *
 * No dependencies and no framework — node:test is built in. This page is the single point of failure
 * for the whole consent journey on every platform, and it shipped untested; the deployed version has
 * been relaying zero parameters since June because it read location.search in a fragment flow.
 */

const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const { readParams, appUrl, loopbackUrl } = require('./bridge.js');

const HYBRID_FRAGMENT = '#code=abc123&id_token=ey.J.sig&state=st-1';

test('reads the fragment, which is where the hybrid flow actually replies', () => {
  assert.equal(readParams(HYBRID_FRAGMENT, ''), 'code=abc123&id_token=ey.J.sig&state=st-1');
});

test('REGRESSION: a fragment-only response is not silently dropped', () => {
  // The deployed page did `window.location.search || ""`, so this returned '' and it relayed a URL
  // with no parameters at all. Every consent died here.
  assert.notEqual(readParams(HYBRID_FRAGMENT, ''), '');
});

test('falls back to the query if a response ever arrives that way', () => {
  assert.equal(readParams('', '?code=abc&state=st-1'), 'code=abc&state=st-1');
});

test('prefers the fragment when both are present', () => {
  assert.equal(readParams('#code=from-fragment', '?code=from-query'), 'code=from-fragment');
});

test('returns empty when the bank sent nothing', () => {
  assert.equal(readParams('', ''), '');
  assert.equal(readParams('#', '?'), '');
});

test('mobile hand-off targets the scheme and host the app registers', () => {
  assert.equal(
    appUrl('code=abc&state=st-1'),
    'org.mifosx.openbanking://callback?code=abc&state=st-1',
  );
});

test('desktop hand-off rewrites the fragment to a query, because servers cannot see fragments', () => {
  assert.equal(
    loopbackUrl('code=abc&state=st-1'),
    'http://127.0.0.1:8765/callback?code=abc&state=st-1',
  );
});

test('an error response relays through untouched', () => {
  const raw = readParams('#error=access_denied&state=st-1', '');
  assert.equal(appUrl(raw), 'org.mifosx.openbanking://callback?error=access_denied&state=st-1');
});

test('the id_token survives the hand-off intact', () => {
  // The callback validates the nonce inside this JWT; losing or mangling it fails a real consent.
  const raw = readParams(HYBRID_FRAGMENT, '');
  assert.ok(appUrl(raw).includes('id_token=ey.J.sig'));
  assert.ok(loopbackUrl(raw).includes('id_token=ey.J.sig'));
});

test('the page wires up both hand-offs and a manual fallback', () => {
  const html = fs.readFileSync(path.join(__dirname, 'index.html'), 'utf8');

  assert.ok(html.includes('bridge.js'), 'page must load the tested logic');
  assert.ok(html.includes('127.0.0.1'), 'desktop loopback relay missing');
  assert.ok(html.includes('location.hash'), 'fragment read missing — the bug that broke everything');
  assert.ok(html.includes('id="retry"'), 'manual fallback missing: browsers block non-gesture scheme hand-off');
});

test('the constants match the app, which is what makes the hand-off land', () => {
  const { SCHEME, LOOPBACK_PORT } = require('./bridge.js');
  assert.equal(SCHEME, 'org.mifosx.openbanking'); // cmp-android ConsentRedirectIntent.kt
  assert.equal(LOOPBACK_PORT, 8765); // cmp-desktop ConsentRedirectListener.PORT
});
