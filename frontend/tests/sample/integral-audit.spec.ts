import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { api, login } from './sample-fixtures'

test('review detail failures end loading, allow retry and do not expose stale decisions', async ({ page }) => {
  const actor = await login(page, 'dev.reviewer')
  const reviewId = 'f0000000-0000-4000-8000-000000000001'
  let fail = true
  await page.route('**/api/reviews/inbox?*', route => route.fulfill({ json: { items: [{ id: reviewId, reviewId, documentId: reviewId, title: 'Revisión de recuperación', stageLabel: 'Revisión técnica', requiresSignature: true, status: 'ACTIVE', roundNumber: 1, groupName: 'Comisión', periodName: '2026' }], total: 1, page: 0, size: 20, metrics: { active: 1, completed: 0, returned: 0 } } }))
  await page.route('**/api/reviews/' + reviewId, route => fail ? route.fulfill({ status: 503, json: { message: 'Consulta temporalmente no disponible' } }) : route.fulfill({ json: { review: { id: reviewId, status: 'IN_REVIEW', round_number: 1 }, tasks: [{ id: reviewId, actor_id: actor.id, actor_name: actor.displayName, stage_label: 'Revisión técnica', requires_signature: true, status: 'ACTIVE', row_version: 0 }], observations: [], pageCount: 1, pdfHash: '0'.repeat(64), canAct: true } }))
  await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({ contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64') }))
  await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
  await page.getByRole('button', { name: /Revisar Revisión de recuperación/ }).click()
  await expect(page.getByText('Consulta temporalmente no disponible', { exact: true })).toBeVisible()
  await expect(page.getByText('Cargando revisión…', { exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Preparar firma de revisión', exact: true })).toHaveCount(0)
  fail = false
  await page.getByRole('button', { name: 'Reintentar revisión', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Preparar firma de revisión', exact: true })).toBeVisible()
  await expect(page.getByText('Consulta temporalmente no disponible', { exact: true })).toHaveCount(0)
})

test('administrative modules remain readable and navigable across PC tablet and themes', async ({ page }) => {
  await login(page, 'dev.admin')
  const errors: string[] = []
  page.on('pageerror', error => errors.push(error.message))
  const modules = ['Usuarios', 'Grupos y pertenencias', 'Carreras y ámbitos', 'Períodos académicos', 'Plantillas', 'Catálogo de actividades', 'Recursos y medios', 'Feriados y restricciones', 'Cargos institucionales', 'Flujos de aprobación', 'Reportes de gestión', 'Auditoría', 'Certificados']
  for (const [width, height, theme] of [[1440, 900, 'light'], [1920, 1080, 'dark'], [1024, 768, 'light'], [820, 1180, 'dark']] as const) {
    await page.setViewportSize({ width, height })
    await page.evaluate(value => { document.documentElement.dataset.theme = value }, theme)
    for (const name of modules) {
      await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name, exact: true }).click()
      await expect(page.locator('main h1')).toBeVisible()
      await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), { message: name + ' must fit ' + width }).toBeTruthy()
    }
    await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Auditoría', exact: true }).click()
    const control = page.getByRole('button', { name: 'Actualizar historial', exact: true })
    if (theme === 'dark') {
      const contrast = await control.evaluate(element => {
        const rgb = (value: string) => value.match(/[\d.]+/g)!.slice(0, 3).map(Number).map(n => { const c = n / 255; return c <= .04045 ? c / 12.92 : ((c + .055) / 1.055) ** 2.4 })
        const luminance = (value: string) => { const c = rgb(value); return c[0] * .2126 + c[1] * .7152 + c[2] * .0722 }
        const style = getComputedStyle(element), a = luminance(style.color), b = luminance(style.backgroundColor)
        return (Math.max(a, b) + .05) / (Math.min(a, b) + .05)
      })
      expect(contrast, 'Enabled secondary actions require readable contrast in dark mode').toBeGreaterThanOrEqual(4.5)
    }
    await page.screenshot({ path: 'test-results/integral-admin-' + width + '-' + theme + '.png', fullPage: true, animations: 'disabled' })
  }
  expect(errors).toEqual([])
})

test('an unavailable institutional submission never appears as an actual pending procedure', async ({ page }) => {
  await login(page, 'dev.principal')
  const options = await api(page, '/work-plans/options')
  const scope = options.scopes.find((value: { canCreate: boolean }) => value.canCreate)
  const plan = await api(page, '/work-plans', 'POST', { scopeId: scope.id, groupId: scope.groupId, periodId: scope.periodId, requestKey: randomUUID(), title: 'Recuperación de trámite ' + randomUUID() })
  let fail = true
  await page.route('**/api/work-plans/' + plan.id + '/formal-submission', route => fail ? route.fulfill({ status: 503, json: { message: 'Trámite temporalmente no disponible' } }) : route.continue())
  await page.goto('/?view=documents&doc=' + plan.id + '&step=signing&workspace=documents')
  await page.getByText('Elaboradores, opciones y circuito de revisión', { exact: true }).click()
  await expect(page.getByText('Trámite temporalmente no disponible', { exact: true })).toBeVisible()
  await expect(page.getByText(/Documento aprobado, pendiente de trámite externo/)).toHaveCount(0)
  fail = false
  await page.getByRole('button', { name: 'Reintentar trámite institucional', exact: true }).click()
  await expect(page.getByText('Trámite temporalmente no disponible', { exact: true })).toHaveCount(0)
})

test('scope preparation and member lookups recover without endless loading or false empty lists', async ({ page }) => {
  await login(page, 'dev.coordinator')
  let extensionFail = true, membersFail = true
  await page.route('**/api/organization/scopes/*/preparation-extension', route => extensionFail ? route.fulfill({ status: 503, json: { message: 'Plazo temporalmente no disponible' } }) : route.continue())
  await page.route('**/api/organization/scopes/*/members', route => membersFail ? route.fulfill({ status: 503, json: { message: 'Integrantes temporalmente no disponibles' } }) : route.continue())
  await page.getByRole('button', { name: 'Mi organización', exact: true }).click()
  const scope = page.getByRole('combobox', { name: 'Mi ámbito institucional', exact: true })
  await expect(scope.locator('option').nth(1)).toBeAttached()
  await scope.selectOption((await scope.locator('option').nth(1).getAttribute('value'))!)
  await expect(page.getByText('Plazo temporalmente no disponible', { exact: true })).toBeVisible()
  await expect(page.getByText('Cargando plazo de elaboración…', { exact: true })).toHaveCount(0)
  extensionFail = false; membersFail = false
  await page.getByRole('button', { name: 'Reintentar plazo de elaboración', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Elaboración del Plan', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Reintentar integrantes del ámbito', exact: true }).click()
  await expect(page.locator('.scope-member-list li').first()).toBeVisible()
})

test('auxiliary administrative queries distinguish errors from empty data and allow retries', async ({ page }) => {
  await login(page, 'dev.admin')
  for (const [module, endpoint, retry, empty] of [
    ['Cargos institucionales', '/api/admin/positions', 'Reintentar cargos institucionales', 'No hay cargos registrados.'],
    ['Recursos y medios', '/api/admin/planning/catalogs', 'Reintentar catálogos de planificación', ''],
    ['Plantillas', '/api/admin/document-structures/T1', 'Reintentar estructura documental', ''],
    ['Períodos académicos', '/api/admin/deadline-notices', 'Reintentar configuración de avisos', ''],
  ]) {
    let recovered = false
    await page.route(url => url.pathname === endpoint, async route => {
      recovered ||= await page.evaluate(label => document.documentElement.dataset.auditRetry === label, retry)
      return recovered ? route.continue() : route.fulfill({ status: 503, json: { message: module + ': consulta no disponible' } })
    })
    await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: module, exact: true }).click()
    await expect(page.getByText(module + ': consulta no disponible', { exact: true })).toBeVisible()
    if (empty) await expect.soft(page.getByText(empty, { exact: true })).toHaveCount(0)
    await expect.soft(page.getByRole('button', { name: retry, exact: true })).toBeVisible()
    if (await page.getByRole('button', { name: retry, exact: true }).isVisible()) {
      // Restore the network only when the actual retry click is delivered, so a late
      // parent bootstrap request cannot recover the panel before the user acts.
      await page.evaluate(label => {
        const clicked = (event: MouseEvent) => {
          if ((event.target as HTMLElement).closest('button')?.textContent === label) {
            document.documentElement.dataset.auditRetry = label
            document.removeEventListener('click', clicked, true)
          }
        }
        document.addEventListener('click', clicked, true)
      }, retry)
      await page.getByRole('button', { name: retry, exact: true }).click()
      await expect(page.getByText(module + ': consulta no disponible', { exact: true })).toHaveCount(0)
    }
  }
})
