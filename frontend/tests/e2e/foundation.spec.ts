import { withConfirmation } from '../confirmation'
import { openAction } from '../open-action'
import { solveCaptcha } from './login'
import { test, expect } from '@playwright/test'
import { readFileSync, writeFileSync, mkdtempSync, rmSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { X509Certificate } from 'node:crypto'
import { tmpdir } from 'node:os'
import { join, resolve, dirname, basename } from 'node:path'

function personalCertificate() {
  const root = resolve(tmpdir()); const directory = mkdtempSync(join(root, 'utaped-signing-'))
  if (dirname(resolve(directory)) !== root || !basename(directory).startsWith('utaped-signing-')) throw new Error('Invalid test fixture directory')
  const key = join(directory, 'key.pem'); const certificate = join(directory, 'public.pem'); const container = join(directory, 'certificate.p12')
  const password = 'E2e-only-signing-password'
  try {
    const config = join(directory, 'openssl.cnf')
    writeFileSync(config, '[req]\ndistinguished_name=dn\n[dn]\n')
    execFileSync('openssl', ['req', '-config', config, '-x509', '-newkey', 'rsa:2048', '-nodes', '-keyout', key, '-out', certificate, '-days', '1', '-subj', '/CN=E2E Personal signer', '-addext', 'basicConstraints=critical,CA:FALSE', '-addext', 'keyUsage=critical,digitalSignature'], { stdio: 'ignore' })
    execFileSync('openssl', ['pkcs12', '-export', '-inkey', key, '-in', certificate, '-out', container, '-passout', 'pass:' + password], { stdio: 'ignore' })
    return { buffer: readFileSync(container), fingerprint: new X509Certificate(readFileSync(certificate)).fingerprint256.replaceAll(':', '').toLowerCase(), password }
  } finally { rmSync(directory, { recursive: true, force: true }) }
}

// This file is generated locally for the isolated validation stack, never committed.
const environment = Object.fromEntries(readFileSync('../.env.e2e', 'utf8').split(/\r?\n/).filter(line => line.includes('=')).map(line => {
  const index = line.indexOf('='); return [line.slice(0,index),line.slice(index+1)]
}))
const adminPassword = 'E2e-only-admin-password-2026'
const userPassword = 'E2e-only-user-password-2026'

test('local identity, administration, context isolation, recovery and logout', async ({ page, request }) => {
  test.setTimeout(180000)
  await expect.poll(async () => {
    try { return (await request.get((process.env.E2E_API_URL || 'http://127.0.0.1:28081') + '/actuator/health')).status() } catch { return 0 }
  }, { timeout: 30000 }).toBe(200)
  const suffix = Date.now().toString()
  const email = `e2e-${suffix}@example.invalid`
  const displayName = `Usuario E2E ${suffix}`
  const group = `Grupo de prueba ${suffix}`
  const certificate = personalCertificate()
  await page.goto('/')
  await page.getByRole('textbox', { name: 'Usuario', exact: true }).fill(environment.BOOTSTRAP_ADMIN_EMAIL)
  await page.getByLabel('Contraseña', { exact: true }).fill(environment.BOOTSTRAP_ADMIN_PASSWORD)
  await solveCaptcha(page); await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  await expect.poll(async () => await page.getByRole('alert').isVisible() || await page.getByRole('button', { name: 'Cerrar sesión' }).isVisible()).toBeTruthy()
  // Re-running the isolated stack can retain the first changed administrator password.
  if (await page.getByRole('alert').isVisible()) {
    await page.getByLabel('Contraseña', { exact: true }).fill(adminPassword)
    await solveCaptcha(page); await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  }
  await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible()
  if (await page.getByRole('heading', { name: 'Acceso temporal' }).isVisible()) {
    await page.getByLabel('Contraseña actual').fill(environment.BOOTSTRAP_ADMIN_PASSWORD)
    await page.getByLabel('Nueva contraseña', { exact: true }).fill(adminPassword)
    await page.getByLabel('Confirmar contraseña').fill(adminPassword)
    await page.getByRole('button', { name: 'Guardar contraseña' }).click()
  }
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Usuarios', exact: true }).click()
  await page.getByRole('button', { name: '+ Agregar Usuario', exact: true }).click()
  await page.getByLabel('Nombres', { exact: true }).fill('Usuario E2E')
  await page.getByLabel('Apellidos', { exact: true }).fill(suffix)
  await page.getByLabel('Correo', { exact: true }).fill(email)
  await page.getByLabel('Contraseña temporal').fill('E2e-only-temporary-password')
  await page.getByRole('button', { name: 'Crear usuario', exact: true }).click()
  await expect(page.getByRole('cell', { name: email, exact: true })).toBeVisible()
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Grupos y pertenencias', exact: true }).click()
  await openAction(page, 'Crear grupo');await page.getByLabel('Nombre del grupo').fill(group)
  await page.getByRole('button', { name: 'Crear grupo', exact: true }).click()
  await openAction(page, 'Asignar integrante'); await expect(page.getByLabel('Grupo', { exact: true })).toContainText(group)
  await openAction(page, 'Asignar integrante');await page.getByLabel('Grupo', { exact: true }).selectOption({ label: group })
  await page.getByLabel('Buscar usuario para asignar').fill(email)
  await expect(page.getByLabel('Usuario', { exact: true })).toContainText(displayName)
  await page.getByLabel('Usuario', { exact: true }).selectOption({ label: displayName })
  await page.getByRole('button', { name: 'Asignar integrante', exact: true }).click()
  await expect(page.getByRole('status')).toHaveText('Cambios guardados.')
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Períodos académicos', exact: true }).click()
  await openAction(page, 'Crear período');await page.getByLabel('Nombre del período').fill(`Período ${suffix}`)
  for (const [label,date] of [['Inicio del período','2026-07-01'],['Fin del período','2026-12-31'],['Inicio de elaboración',new Date(Date.now()-86400000).toISOString().slice(0,10)],['Fin de elaboración',new Date(Date.now()+86400000).toISOString().slice(0,10)],['Inicio de revisión','2026-08-01'],['Fin de revisión','2026-08-31']]) await page.getByLabel(label).fill(date)
  await page.getByRole('button', { name: 'Crear período', exact: true }).click()
  await expect(page.getByRole('status')).toHaveText('Cambios guardados.')
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Recursos y medios', exact: true }).click()
  await expect(page.getByLabel('Nombre del período')).toHaveCount(0)
  await expect(page.getByLabel('Nombre del grupo')).toHaveCount(0)
  await openAction(page, 'Crear o editar elemento');await page.getByLabel('Denominación del catálogo').fill('Cambios pendientes')

  await withConfirmation(page, false, async () => { await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Usuarios', exact: true }).click() });
  await expect(page.getByLabel('Denominación del catálogo')).toHaveValue('Cambios pendientes')
  await openAction(page, 'Crear o editar elemento');await page.getByLabel('Denominación del catálogo').fill('Recurso E2E ' + suffix)
  await page.getByRole('button', { name: 'Crear elemento', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Editar elemento Recurso E2E ' + suffix, exact: true })).toBeVisible()
  await page.getByLabel('Tipo de catálogo').selectOption('MEANS')
  await openAction(page, 'Crear o editar elemento');await page.getByLabel('Denominación del catálogo').fill('Medio E2E ' + suffix)
  await page.getByRole('button', { name: 'Crear elemento', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Editar elemento Medio E2E ' + suffix, exact: true })).toBeVisible()
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Catálogo de actividades', exact: true }).click()
  await page.getByLabel('Grupo del catálogo').selectOption({ label: group })
  await openAction(page, 'Crear o editar actividad');await page.getByLabel('Título de actividad del catálogo').fill('Actividad obligatoria E2E ' + suffix)
  await page.getByLabel('Obligatoria para este grupo').check()
  await page.getByRole('button', { name: 'Crear actividad del catálogo', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Editar actividad Actividad obligatoria E2E ' + suffix, exact: true })).toBeVisible()
  await openAction(page, 'Editar denominación colectiva');await page.getByLabel('Denominación colectiva del grupo').fill('Integrantes del grupo E2E')
  await page.getByRole('button', { name: 'Guardar denominación colectiva', exact: true }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Configuración de planificación guardada.' })).toBeVisible()
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Certificados', exact: true }).click()
  const certificateAdmin = page.getByRole('region', { name: 'Vinculación de certificados', exact: true })
  await certificateAdmin.getByLabel('Buscar firmante', { exact: true }).fill(email)
  await expect(certificateAdmin.getByLabel('Cuenta del firmante')).toContainText(email)
  await certificateAdmin.getByLabel('Cuenta del firmante').selectOption({ label: displayName + ' · ' + email })
  await openAction(certificateAdmin, 'Vincular certificado público');await certificateAdmin.getByLabel('Huella SHA-256 pública').fill(certificate.fingerprint)
  await certificateAdmin.getByLabel('Referencia de comprobación de identidad').fill('Comprobación de certificado público generado para E2E.')
  await certificateAdmin.getByLabel('He comprobado que el certificado público corresponde a esta cuenta.').check()
  await certificateAdmin.getByRole('button', { name: 'Vincular huella pública', exact: true }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Huella pública vinculada.' })).toHaveText('Huella pública vinculada.')

  await page.getByRole('button', { name: 'Cerrar sesión' }).click()
  await page.getByRole('textbox', { name: 'Usuario', exact: true }).fill(email)
  await page.getByLabel('Contraseña', { exact: true }).fill('E2e-only-temporary-password')
  await solveCaptcha(page); await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Acceso temporal' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Usuarios', exact: true })).toHaveCount(0)
  await page.getByLabel('Contraseña actual').fill('E2e-only-temporary-password')
  await page.getByLabel('Nueva contraseña', { exact: true }).fill(userPassword)
  await page.getByLabel('Confirmar contraseña').fill(userPassword)
  await page.getByRole('button', { name: 'Guardar contraseña' }).click()
  await expect(page.getByRole('heading', { name: 'Bienvenido, ' + displayName })).toBeVisible()
  expect((await page.request.get('/api/admin/users/directory')).status()).toBe(403)
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
    const result = await (await request.get((process.env.E2E_MAIL_URL || 'http://127.0.0.1:28025') + '/api/v1/messages')).json()
    return result.messages.some((m: { To: { Address: string }[] }) => m.To.some(to => to.Address === email))
  }).toBeTruthy()
  const messages = await (await request.get((process.env.E2E_MAIL_URL || 'http://127.0.0.1:28025') + '/api/v1/messages')).json()
  const message = messages.messages.find((m: { To: { Address: string }[] }) => m.To.some(to => to.Address === email))
  expect(message).toBeTruthy()
  const detail = await (await request.get((process.env.E2E_MAIL_URL || 'http://127.0.0.1:28025') + '/api/v1/message/' + message.ID)).json()
  const link = String(detail.Text).match(/http[^\s]+#reset=[^\s]+/)![0]
  const recovery = new URL(link)
  await page.goto('/' + recovery.hash)
  await page.getByLabel('Nueva contraseña', { exact: true }).fill('E2e-only-recovered-password')
  await page.getByLabel('Confirmar contraseña', { exact: true }).fill('E2e-only-recovered-password')
  await page.getByRole('button', { name: 'Guardar contraseña' }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Contraseña actualizada' })).toContainText('Contraseña actualizada')
  await page.getByRole('textbox', { name: 'Usuario', exact: true }).fill(email)
  await page.getByLabel('Contraseña', { exact: true }).fill('E2e-only-recovered-password')
  await solveCaptcha(page); await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Bienvenido, ' + displayName })).toBeVisible()
  await page.screenshot({ path: 'test-results/home-desktop.png', fullPage: true })
  await page.getByRole('button', { name: 'Documentos', exact: true }).click()
  await openAction(page, 'Crear documento'); await expect(page.getByText('No tiene ámbitos disponibles para crear un Plan.', { exact: true })).toBeVisible()
  certificate.buffer.fill(0)
})
