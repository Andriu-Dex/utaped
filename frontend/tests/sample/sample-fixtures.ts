import { expect, type Page } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { createHash } from 'node:crypto'
const environment = Object.fromEntries(readFileSync('../.env.sample', 'utf8').split(/\r?\n/).filter(l => l.includes('=')).map(l => { const i = l.indexOf('='); return [l.slice(0,i).trim(), l.slice(i+1).trim()] }))
export async function api(page: Page, path: string, method = 'GET', body?: unknown) {
  const csrf = await (await page.request.get('/api/auth/csrf')).json()
  const r = await page.request.fetch('/api' + path, { method, headers: { [csrf.headerName]: csrf.token }, data: body })
  expect(r.status(), method + ' ' + path).toBeLessThan(300); const data = await r.text(); return data ? JSON.parse(data) : undefined
}
export async function login(page: Page, username: string, password = environment.UTAPED_SEED_PASSWORD) {
  await page.goto('/'); await page.getByLabel('Usuario', { exact: true }).fill(username); await page.getByLabel('Contraseña', { exact: true }).fill(password)
  await expect.poll(() => page.getByAltText('Código de seis caracteres. También puedes escucharlo con Escuchar código.').evaluate((i: HTMLImageElement) => i.complete && i.naturalWidth > 0)).toBeTruthy()
  const pending = page.waitForResponse(r => new URL(r.url()).pathname === '/api/auth/captcha')
  await page.getByRole('button', { name: 'Cambiar código de verificación', exact: true }).click()
  const response = await pending, id = response.headers()['x-captcha-id']; expect(id).toMatch(/^[a-f0-9-]{36}$/)
  const answer = 'ABC234', hash = createHash('sha256').update(id + ':' + answer).digest('hex')
  const sql = `UPDATE login_captcha SET answer_hash='${hash}' WHERE id='${id}' AND current_database()='utaped_dev' RETURNING id;`
  const result = execFileSync('docker', ['compose','--env-file','../.env.sample','-p','utaped-sample','-f','../compose.yml','-f','../compose.sample.yml','exec','-T','db','psql','-v','ON_ERROR_STOP=1','-U','utaped','-d','utaped_dev','-c',sql], { encoding: 'utf8' }); expect(result).toContain(id)
  await page.getByLabel('Código de verificación', { exact: true }).fill(answer); await page.getByRole('button', { name: 'Ingresar', exact: true }).click(); await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible()
  return api(page, '/auth/me')
}
