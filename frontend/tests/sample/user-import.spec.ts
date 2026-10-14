import { withConfirmation } from '../confirmation'
import { test, expect, type Page } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { createHash, randomUUID } from 'node:crypto'
const environment = Object.fromEntries(readFileSync('../.env.sample', 'utf8').split(/\r?\n/).filter(l => l.includes('=')).map(l => { const i = l.indexOf('='); return [l.slice(0,i).trim(), l.slice(i+1).trim()] }))
async function api(page: Page, path: string, method = 'GET', body?: unknown) {
  const csrf = await (await page.request.get('/api/auth/csrf')).json()
  const r = await page.request.fetch('/api' + path, { method, headers: { [csrf.headerName]: csrf.token }, data: body })
  expect(r.status(), method + ' ' + path).toBeLessThan(300); const data = await r.text(); return data ? JSON.parse(data) : undefined
}
async function login(page: Page, username: string, password = environment.UTAPED_SEED_PASSWORD) {
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
test('CSV preview, correction, single credential download and forced password change', async ({ browser }) => {
  const contexts = await Promise.all([browser.newContext({baseURL:process.env.SAMPLE_BASE_URL || 'http://127.0.0.1:64431',viewport:{width:1440,height:1000}}),browser.newContext({baseURL:process.env.SAMPLE_BASE_URL || 'http://127.0.0.1:64431'})]);const admin=await contexts[0].newPage(),member=await contexts[1].newPage();
  const suffix=randomUUID().slice(0,8),username='import.'+suffix,email=username+'@example.invalid';
  try {
    await login(admin,'dev.admin');await admin.getByRole('button',{name:'Usuarios',exact:true}).click();await admin.getByRole('button',{name:'Abrir importador',exact:true}).click();
    const input=admin.getByLabel('Archivo CSV de usuarios');const header='email,username,first_names,last_names\n';
    await input.setInputFiles({name:'errores.csv',mimeType:'text/csv',buffer:Buffer.from(header+`${email},${username},Ana,Perez\n${email},${username},Luis,Lopez\n`)});await admin.getByRole('button',{name:'Analizar archivo',exact:true}).click();await expect(admin.getByText('Correo repetido en este archivo.',{exact:true})).toBeVisible();await expect(admin.getByRole('button',{name:'Crear cuentas y descargar credenciales',exact:true})).toBeDisabled();
    await withConfirmation(admin, true, async () => { await admin.getByRole('button',{name:'Preparar otro archivo',exact:true}).click(); });await input.setInputFiles({name:'usuarios.csv',mimeType:'text/csv',buffer:Buffer.from(header+`${email},${username},Ana,Perez\n`)});await admin.getByRole('button',{name:'Analizar archivo',exact:true}).click();await expect(admin.getByText('Lista',{exact:true})).toBeVisible();
    await withConfirmation(admin, false, async () => { await admin.getByRole('button',{name:'Grupos y pertenencias',exact:true}).click(); });await expect(admin.getByRole('heading',{name:'Importar usuarios',exact:true})).toBeVisible();
    await admin.evaluate(()=>window.scrollTo(0,0));await admin.screenshot({path:'test-results/user-import-desktop.png',fullPage:true});await admin.getByRole('button',{name:'Modo oscuro',exact:true}).click();await admin.setViewportSize({width:820,height:1180});await admin.screenshot({path:'test-results/user-import-tablet-dark.png',fullPage:true});expect(await admin.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
    await admin.getByLabel('Revisé las filas y guardaré el archivo de credenciales en un lugar privado para entregarlas a cada titular.').check();const pending=admin.waitForEvent('download');await withConfirmation(admin, true, async () => { await admin.getByRole('button',{name:'Crear cuentas y descargar credenciales',exact:true}).click(); });const result=await pending;expect(result.suggestedFilename()).toBe('credenciales-importadas.csv');const stream=await result.createReadStream();let content='';for await(const chunk of stream!) content+=chunk.toString();const password=content.split(/\r?\n/)[1].split(',')[2];await result.delete();content='';
    await expect(admin.getByRole('heading',{name:'1 cuenta creada',exact:true})).toBeVisible();await expect(admin.getByRole('button',{name:'Crear cuentas y descargar credenciales',exact:true})).toHaveCount(0);
    await login(member,username,password);await expect(member.getByRole('heading',{name:'Acceso temporal'})).toBeVisible();await member.getByLabel('Contraseña actual',{exact:true}).fill(password);await member.getByLabel('Nueva contraseña',{exact:true}).fill('E2E-only-import-changed-password-2026');await member.getByLabel('Confirmar contraseña',{exact:true}).fill('E2E-only-import-changed-password-2026');await member.getByRole('button',{name:'Guardar contraseña',exact:true}).click();await expect(member.getByRole('button',{name:'Documentos',exact:true})).toBeVisible();expect((await member.request.get('/api/admin/user-imports/'+randomUUID())).status()).toBe(403);
  } finally { for(const context of contexts) await context.close() }
})
