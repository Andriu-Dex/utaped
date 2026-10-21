import { expect, type Page } from '@playwright/test'
import { execFileSync } from 'node:child_process'
import { createHash } from 'node:crypto'

// Isolated Compose database fixture. No answer or test bypass is exposed by the application.
export async function solveCaptcha(page: Page) {
  await expect(page.getByRole('button', { name: 'Ingresar', exact: true })).toBeEnabled()
  const responsePromise = page.waitForResponse(response => new URL(response.url()).pathname === '/api/auth/captcha')
  await page.getByRole('button', { name: 'Cambiar código de verificación', exact: true }).click()
  const response = await responsePromise
  expect(response.status()).toBe(200)
  const id = response.headers()['x-captcha-id']
  expect(id).toMatch(/^[a-f0-9-]{36}$/)
  const answer = 'ABC234'; const hash = createHash('sha256').update(id + ':' + answer).digest('hex')
  const sql = `UPDATE login_captcha SET answer_hash='${hash}' WHERE id='${id}' AND current_database()='utaped' RETURNING id`
  const result = execFileSync('docker', ['compose', '-p', 'utaped-e2e', '--env-file', '../.env.e2e', '-f', '../compose.yml', 'exec', '-T', 'db', 'psql', '-v', 'ON_ERROR_STOP=1', '-U', 'utaped', '-d', 'utaped', '-c', sql], { encoding: 'utf8' })
  expect(result).toContain(id)
  await page.getByLabel('Código de verificación', { exact: true }).fill(answer)
  await expect(page.getByRole('button', { name: 'Ingresar', exact: true })).toBeEnabled()
}
