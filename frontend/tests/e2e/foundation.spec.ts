import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'

// This file is generated locally for the isolated validation stack, never committed.
const environment = Object.fromEntries(readFileSync('../.env.e2e', 'utf8').split(/\r?\n/).filter(line => line.includes('=')).map(line => {
  const index = line.indexOf('='); return [line.slice(0,index),line.slice(index+1)]
}))
const adminPassword = 'E2e-only-admin-password-2026'
const userPassword = 'E2e-only-user-password-2026'

test('local identity, administration, context isolation, recovery and logout', async ({ page, request }) => {
  test.setTimeout(60000)
  await expect.poll(async () => {
    try { return (await request.get('http://127.0.0.1:18080/actuator/health')).status() } catch { return 0 }
  }, { timeout: 30000 }).toBe(200)
  const suffix = Date.now().toString()
  const email = `e2e-${suffix}@example.invalid`
  const displayName = `Usuario E2E ${suffix}`
  const group = `Grupo de prueba ${suffix}`
  await page.goto('/')
  await page.getByLabel('Correo institucional').fill(environment.BOOTSTRAP_ADMIN_EMAIL)
  await page.getByLabel('Contraseña', { exact: true }).fill(environment.BOOTSTRAP_ADMIN_PASSWORD)
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  await expect.poll(async () => await page.getByRole('alert').isVisible() || await page.getByRole('button', { name: 'Cerrar sesión' }).isVisible()).toBeTruthy()
  // Re-running the isolated stack can retain the first changed administrator password.
  if (await page.getByRole('alert').isVisible()) {
    await page.getByLabel('Contraseña', { exact: true }).fill(adminPassword)
    await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  }
  await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible()
  if (await page.getByRole('heading', { name: 'Cambie su contraseña temporal' }).isVisible()) {
    await page.getByLabel('Contraseña actual').fill(environment.BOOTSTRAP_ADMIN_PASSWORD)
    await page.getByLabel('Nueva contraseña', { exact: true }).fill(adminPassword)
    await page.getByLabel('Confirmar contraseña').fill(adminPassword)
    await page.getByRole('button', { name: 'Guardar contraseña' }).click()
    await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible()
    await page.getByLabel('Correo institucional').fill(environment.BOOTSTRAP_ADMIN_EMAIL)
    await page.getByLabel('Contraseña', { exact: true }).fill(adminPassword)
    await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  }
  await page.getByRole('button', { name: 'Administración', exact: true }).click()
  await page.getByLabel('Nombre', { exact: true }).fill(displayName)
  await page.getByLabel('Correo', { exact: true }).fill(email)
  await page.getByLabel('Contraseña temporal').fill('E2e-only-temporary-password')
  await page.getByRole('button', { name: 'Crear usuario', exact: true }).click()
  await expect(page.getByRole('cell', { name: email, exact: true })).toBeVisible()
  await page.getByLabel('Nombre del grupo').fill(group)
  await page.getByRole('button', { name: 'Crear grupo', exact: true }).click()
  await expect(page.getByLabel('Grupo', { exact: true })).toContainText(group)
  await page.getByLabel('Grupo', { exact: true }).selectOption({ label: group })
  await page.getByLabel('Usuario', { exact: true }).selectOption({ label: displayName })
  await page.getByRole('button', { name: 'Asignar integrante' }).click()
  await expect(page.getByRole('status')).toHaveText('Cambios guardados.')
  await page.getByLabel('Nombre del período').fill(`Período ${suffix}`)
  for (const [label,date] of [['Inicio del período','2026-07-01'],['Fin del período','2026-12-31'],['Inicio de elaboración','2026-07-01'],['Fin de elaboración','2026-07-31'],['Inicio de revisión','2026-08-01'],['Fin de revisión','2026-08-31']]) await page.getByLabel(label).fill(date)
  await page.getByRole('button', { name: 'Crear período', exact: true }).click()
  await expect(page.getByRole('status')).toHaveText('Cambios guardados.')
  await page.getByRole('button', { name: 'Cerrar sesión' }).click()
  await page.getByLabel('Correo institucional').fill(email)
  await page.getByLabel('Contraseña', { exact: true }).fill('E2e-only-temporary-password')
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Cambie su contraseña temporal' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Administración', exact: true })).toHaveCount(0)
  await page.getByLabel('Contraseña actual').fill('E2e-only-temporary-password')
  await page.getByLabel('Nueva contraseña', { exact: true }).fill(userPassword)
  await page.getByLabel('Confirmar contraseña').fill(userPassword)
  await page.getByRole('button', { name: 'Guardar contraseña' }).click()
  await page.getByLabel('Correo institucional').fill(email)
  await page.getByLabel('Contraseña', { exact: true }).fill(userPassword)
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Bienvenido, ' + displayName })).toBeVisible()
  await page.getByLabel('Contexto de grupo').selectOption({ label: group })
  await expect(page.getByText(displayName + ' · Miembro', { exact: true })).toBeVisible()
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Bienvenido, ' + displayName })).toBeVisible()
  await page.getByRole('button', { name: 'Cerrar sesión' }).click()
  await page.getByRole('button', { name: '¿Olvidó su contraseña?' }).click()
  await page.getByLabel('Correo institucional').fill(email)
  await page.getByRole('button', { name: 'Enviar enlace' }).click()
  await expect(page.getByRole('status')).toContainText('Si el correo está registrado')
  await expect.poll(async () => {
    const result = await (await request.get('http://127.0.0.1:18025/api/v1/messages')).json()
    return result.messages.some((m: { To: { Address: string }[] }) => m.To.some(to => to.Address === email))
  }).toBeTruthy()
  const messages = await (await request.get('http://127.0.0.1:18025/api/v1/messages')).json()
  const message = messages.messages.find((m: { To: { Address: string }[] }) => m.To.some(to => to.Address === email))
  expect(message).toBeTruthy()
  const detail = await (await request.get('http://127.0.0.1:18025/api/v1/message/' + message.ID)).json()
  const link = String(detail.Text).match(/http[^\s]+#reset=[^\s]+/)![0]
  const recovery = new URL(link)
  await page.goto('/' + recovery.hash)
  await page.getByLabel('Nueva contraseña', { exact: true }).fill('E2e-only-recovered-password')
  await page.getByRole('button', { name: 'Guardar contraseña' }).click()
  await expect(page.getByRole('status')).toContainText('Contraseña actualizada')
  await page.getByLabel('Correo institucional').fill(email)
  await page.getByLabel('Contraseña', { exact: true }).fill('E2e-only-recovered-password')
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Bienvenido, ' + displayName })).toBeVisible()
  await page.screenshot({ path: 'test-results/home-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.screenshot({ path: 'test-results/home-mobile.png', fullPage: true })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy()
})
