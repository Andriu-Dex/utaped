import { withConfirmation } from '../confirmation'
import { openAction } from '../open-action'
import { solveCaptcha } from './login'
import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'

const environment = Object.fromEntries(readFileSync('../.env.e2e', 'utf8').split(/\r?\n/).filter(s => s.includes('=')).map(s => { const i = s.indexOf('='); return [s.slice(0, i), s.slice(i + 1)] }))
const date = (days: number) => new Date(Date.now() + days * 86400000).toISOString().slice(0, 10)

test('page reload preserves active document editor and step without resetting to inicio', async ({ page }) => {
  test.setTimeout(90000)
  await page.goto('/')
  await page.getByRole('textbox', { name: 'Usuario', exact: true }).fill(environment.BOOTSTRAP_ADMIN_EMAIL)
  await page.getByLabel('Contraseña', { exact: true }).fill('E2e-only-admin-password-2026')
  await solveCaptcha(page)
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  await expect.poll(async () => await page.getByRole('alert').isVisible() || await page.getByRole('button', { name: 'Cerrar sesión' }).isVisible()).toBeTruthy()
  if (await page.getByRole('alert').isVisible()) {
    await page.getByLabel('Contraseña', { exact: true }).fill(environment.BOOTSTRAP_ADMIN_PASSWORD)
    await solveCaptcha(page)
    await page.getByRole('button', { name: 'Ingresar', exact: true }).click()
  }
  await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible()
  // Fresh disposable databases require first access before any administrative setup.
  if (await page.getByRole('heading', { name: 'Acceso temporal' }).isVisible()) {
    await page.getByLabel('Contraseña actual').fill(environment.BOOTSTRAP_ADMIN_PASSWORD)
    await page.getByLabel('Nueva contraseña', { exact: true }).fill('E2e-only-admin-password-2026')
    await page.getByLabel('Confirmar contraseña').fill('E2e-only-admin-password-2026')
    await page.getByRole('button', { name: 'Guardar contraseña' }).click()
  }
  await expect(page.getByRole('navigation', { name: 'Navegación principal' })).toBeVisible()

  const csrf = await (await page.request.get('/api/auth/csrf')).json()
  const headers = { [csrf.headerName]: csrf.token }
  const identity = await (await page.request.get('/api/auth/me')).json()

  // Setup isolated scope where identity is member and principal responsible
  const suffix = Date.now().toString()
  const group = await (await page.request.post('/api/admin/groups', {
    headers,
    data: { name: 'Grupo Reload ' + suffix, groupType: 'COMMISSION' }
  })).json()

  await page.request.post('/api/admin/groups/' + group.id + '/members', {
    headers,
    data: { userId: identity.id, role: 'COORDINATOR' }
  })

  const period = await (await page.request.post('/api/admin/periods', {
    headers,
    data: {
      name: 'Período Reload ' + suffix,
      startsOn: date(-3),
      endsOn: date(90),
      preparationStartsOn: date(-3),
      preparationEndsOn: date(30),
      reviewStartsOn: date(-3),
      reviewEndsOn: date(90)
    }
  })).json()

  const scope = await (await page.request.post('/api/admin/organization/scopes', {
    headers,
    data: { groupId: group.id, periodId: period.id, careerId: null }
  })).json()

  await page.request.put('/api/admin/organization/scopes/' + scope.id + '/members', {
    headers,
    data: { userId: identity.id, membershipRole: 'MEMBER', positionLabel: '', active: true, rowVersion: 0 }
  })

  await page.request.put('/api/admin/organization/scopes/' + scope.id + '/principal', {
    headers,
    data: { userId: identity.id, rowVersion: 0 }
  })

  // Switch workspace to documents
  await page.reload()
  await page.getByLabel('Espacio de trabajo', { exact: true }).selectOption('documents')

  // 1. Navigate to Documentos
  await page.getByRole('button', { name: 'Documentos', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Gestión Documental Académica', exact: true })).toBeVisible()

  // 2. Reload while on Documentos list -> should stay on Documentos, not Inicio
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Gestión Documental Académica', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: /Bienvenido,/ })).toHaveCount(0)

  // 3. Create a document in our scope
  await openAction(page, 'Crear documento')
  await page.getByLabel('Ámbito del nuevo Plan').selectOption(scope.id)
  await page.getByRole('button', { name: 'Crear borrador', exact: true }).click()

  // Verify we are inside the document editor
  await expect(page.getByRole('button', { name: 'Volver a documentos', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Información general del Plan', exact: true })).toBeVisible()

  // 4. Reload on Step 1 (Información general) -> should stay on Step 1
  await page.reload()
  await expect(page.getByRole('button', { name: 'Volver a documentos', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Información general del Plan', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: /Bienvenido,/ })).toHaveCount(0)

  // 5. Navigate to Step 2 (Contenido)
  await page.screenshot({ path: 'test-results/staged-plan-general-desktop.png', animations: 'disabled' })
  await page.setViewportSize({ width: 820, height: 1180 })
  await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/staged-plan-general-tablet-dark.png', animations: 'disabled' })
  await page.getByRole('button', { name: 'Modo claro', exact: true }).click()
  await page.setViewportSize({ width: 1280, height: 720 })
  await page.getByRole('button', { name: /2\s*Contenido/ }).click()
  await expect(page.getByRole('heading', { name: 'Contenido y redacción académica', exact: true })).toBeVisible()

  // 6. Reload on Step 2 -> should stay on Step 2!
  await page.reload()
  await expect(page.getByRole('button', { name: 'Volver a documentos', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Contenido y redacción académica', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: /2\s*Contenido/ })).toHaveAttribute('aria-current', 'step')
  await expect(page.getByRole('heading', { name: /Bienvenido,/ })).toHaveCount(0)

  // 7. Navigate to Step 3 (Actividades y matriz)
  await page.evaluate(() => {
    const current = location.href, general = new URL(current)
    general.searchParams.set('step', 'general')
    history.pushState(null, '', general.href)
    history.pushState(null, '', current)
  })
  await page.goBack()
  await expect(page.getByRole('heading', { name: 'Información general del Plan', exact: true })).toBeVisible()
  await page.getByRole('button', { name: /3\s*Actividades y matriz/ }).click()
  await expect(page.getByRole('heading', { name: 'Actividades y matriz T1', exact: true })).toBeVisible()

  // 8. Reload on Step 3 -> should stay on Step 3!
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Actividades y matriz T1', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: /Bienvenido,/ })).toHaveCount(0)

  // 9. Return to Plan and then return to documents list
  await page.getByRole('button', { name: 'Volver al Plan', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Volver a documentos', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Volver a documentos', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Gestión Documental Académica', exact: true })).toBeVisible()

  // 10. Reload on documents list -> stays on documents list
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Gestión Documental Académica', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: /Bienvenido,/ })).toHaveCount(0)

  // A browser history transition must not silently discard an editor draft.
  await page.getByRole('button', { name: 'Editar Plan de Trabajo: Grupo Reload ' + suffix + ' · Versión 1.0', exact: true }).click()
  await page.getByRole('button', { name: /2\s*Contenido/ }).click()
  await page.getByRole('textbox', { name: 'Justificación', exact: true }).fill('Texto local que debe conservarse')
  await page.evaluate(() => {
    const current = location.href
    history.pushState(null, '', '?view=home&workspace=documents')
    history.pushState(null, '', current)
  })
  await withConfirmation(page, false, async () => { await page.goBack() })
  await expect(page.getByRole('textbox', { name: 'Justificación', exact: true })).toHaveValue('Texto local que debe conservarse')
  await expect(page).toHaveURL(/step=content/)
  await page.getByRole('textbox', { name: 'Justificación', exact: true }).fill('')
  await page.getByRole('button', { name: 'Volver a documentos', exact: true }).click()

  // 11. Navigate to Inicio
  await page.getByRole('button', { name: 'Inicio', exact: true }).click()
  await expect(page.getByRole('heading', { name: /Bienvenido,/ })).toBeVisible()

  // 12. Reload on Inicio -> stays on Inicio
  await page.reload()
  await expect(page.getByRole('heading', { name: /Bienvenido,/ })).toBeVisible()
})
