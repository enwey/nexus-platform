import test from 'node:test'
import assert from 'node:assert/strict'

import {
  normalizeApiBaseUrl as normalizeDevApiBaseUrl,
  resolveApiBaseUrl as resolveDevApiBaseUrl
} from '../dev-portal/src/config/apiBaseUrl.js'
import {
  normalizeApiBaseUrl as normalizeOpsApiBaseUrl,
  resolveApiBaseUrl as resolveOpsApiBaseUrl
} from '../ops-portal/src/config/apiBaseUrl.js'

test('dev portal resolves explicit production API base URL', () => {
  assert.equal(
    resolveDevApiBaseUrl({ VITE_PLATFORM_API_BASE_URL: 'https://api.example.com/api/v1/' }),
    'https://api.example.com/api/v1'
  )
})

test('ops portal resolves legacy API env key when primary key is absent', () => {
  assert.equal(
    resolveOpsApiBaseUrl({ VITE_API_BASE_URL: 'https://ops.example.com/api/v1/' }),
    'https://ops.example.com/api/v1'
  )
})

test('same-origin root-relative API base URL is allowed', () => {
  assert.equal(resolveDevApiBaseUrl({ VITE_API_BASE_URL: '/api/v1/' }), '/api/v1')
  assert.equal(resolveOpsApiBaseUrl({ VITE_PLATFORM_API_BASE_URL: '/api/v1' }), '/api/v1')
})

test('missing API base URL fails explicitly instead of falling back to localhost', () => {
  assert.throws(() => resolveDevApiBaseUrl({}), /Missing API base URL/)
  assert.throws(() => resolveOpsApiBaseUrl({}), /Missing API base URL/)
})

test('invalid API base URL is rejected', () => {
  assert.throws(
    () => resolveDevApiBaseUrl({ VITE_PLATFORM_API_BASE_URL: 'api.internal.local' }),
    /absolute http\(s\) URL or a root-relative path/
  )
})

test('normalization strips trailing slashes only', () => {
  assert.equal(normalizeDevApiBaseUrl(' https://example.com/api/v1/ '), 'https://example.com/api/v1')
  assert.equal(normalizeOpsApiBaseUrl(''), '')
})
