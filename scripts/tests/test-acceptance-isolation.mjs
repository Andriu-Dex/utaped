import { test } from 'node:test'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('../../', import.meta.url))
const frontend = fileURLToPath(new URL('../../frontend/', import.meta.url))
const isolated = {
  SAMPLE_COMPOSE_PROJECT: 'utaped-acceptance',
  SAMPLE_ENV_FILE: root + '.env.acceptance',
  SAMPLE_BASE_URL: 'http://127.0.0.1:24431',
}
const check = overrides => spawnSync(process.execPath,
  ['--input-type=module', '-e', "import('./tests/sample/acceptance-environment.ts')"],
  { cwd: frontend, env: { ...process.env, ...isolated, ...overrides }, encoding: 'utf8' })

for (const [name, overrides] of [
  ['manual project', { SAMPLE_COMPOSE_PROJECT: 'utaped-sample' }],
  ['manual URL', { SAMPLE_BASE_URL: 'http://127.0.0.1:64431' }],
  ['manual credentials', { SAMPLE_ENV_FILE: root + '.env.sample' }],
  ['missing runner configuration', { SAMPLE_COMPOSE_PROJECT: '' }],
]) {
  test('rejects ' + name + ' before requests or database access', () => {
    const result = check(overrides)
    assert.notEqual(result.status, 0)
    assert.match(result.stderr, /manual development environments|isolated loopback port/)
  })
}
test('accepts only the isolated runner configuration without reading credentials', () => {
  const result = check({})
  assert.equal(result.status, 0, result.stderr)
})
