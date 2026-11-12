import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { api, login } from './sample-fixtures'

test('management overview distinguishes declared progress, coverage and evidence; filters and CSV remain connected', async ({ page }) => {
  await login(page, 'dev.principal')
  const live = await api(page, '/reporting')
  expect(live.upcoming).toBeInstanceOf(Array)
  const groups = await api(page, '/groups'), group = groups[0]
  const row = { groupId: group.id, groupName: 'Comisión de seguimiento académico', careerName: 'Ingeniería de Software', periodName: 'Julio–Diciembre 2026', plans: 5, reports: 3, approved: 3, inReview: 2, drafts: 3, activities: 10, completedActivities: 6, evidenceCoverage: 60, declaredExecution: 70, declaredActivities: 4, pendingEvidence: 5, dueSoonActivities: 2, overdueActivities: 1 }
  const summary = { rows: [row], plans: 5, reports: 3, approved: 3, inReview: 2, drafts: 3, activities: 10, completedActivities: 6, pendingEvidence: 5, dueSoonActivities: 2, overdueActivities: 1, declaredExecution: 70, declaredActivities: 4, declarationCoverage: 40, dueSoonDays: 7, upcoming: [{ documentId: randomUUID(), activityId: randomUUID(), documentTitle: 'Plan de seguimiento', activityTitle: 'Consolidar evidencias de las actividades académicas', groupName: row.groupName, periodName: row.periodName, deadline: '2026-10-30', missingEvidence: 2 }] }
  let unavailable = false, empty = false
  const queries: string[] = []

  await page.route(url => url.pathname === '/api/reporting', route => {
    queries.push(new URL(route.request().url()).search)
    return unavailable ? route.fulfill({ status: 503, json: { message: 'Reporte no disponible temporalmente.' } }) : route.fulfill({ json: empty ? { ...summary, rows: [], plans: 0, reports: 0, approved: 0, inReview: 0, drafts: 0, activities: 0, completedActivities: 0, pendingEvidence: 0, dueSoonActivities: 0, overdueActivities: 0, declaredExecution: null, declaredActivities: 0, declarationCoverage: 0, upcoming: [] } : summary })
  })

  // 1. On Inicio: analytical overview is not shown and /api/reporting is not requested
  await page.getByRole('button', { name: 'Inicio', exact: true }).click()
  expect(queries.length).toBe(0)
  const homeIndicators = page.getByRole('region', { name: 'Indicadores de gestión', exact: true })
  await expect(homeIndicators).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Ver reportes de gestión', exact: true })).toBeVisible()

  // 2. Navigate to Reportes de gestión
  await page.getByRole('button', { name: 'Ver reportes de gestión', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Reportes de gestión', exact: true })).toBeVisible()
  const overview = page.getByRole('region', { name: 'Indicadores de gestión', exact: true })
  await expect(overview.getByText('70 %', { exact: true })).toBeVisible()
  await expect(overview.getByText('4 de 10 actividades informadas', { exact: true })).toBeVisible()
  await expect(overview.getByText('Cobertura de resultados: 40 %', { exact: true })).toBeVisible()
  await expect(overview.getByRole('button', { name: /^Abrir ejecución:/ })).toBeVisible()
  await page.screenshot({ path: 'test-results/management-reports-desktop.png', fullPage: true })

  // 3. Filters and CSV export
  await page.getByLabel('Grupo del reporte').selectOption(group.id)
  await expect.poll(() => queries.at(-1)).toContain('groupId=' + group.id)
  await page.getByLabel('Incluir versiones archivadas').check()
  await expect.poll(() => queries.at(-1)).toContain('archived=true')
  let exportQuery = ''
  await page.route(url => url.pathname === '/api/reporting/export', route => {
    exportQuery = new URL(route.request().url()).search
    return route.fulfill({ contentType: 'text/csv;charset=UTF-8', headers: { 'Content-Disposition': 'attachment; filename=seguimiento-utaped.csv' }, body: '\uFEFFGrupo,Cobertura de resultados (%)\r\nComisión,40\r\n' })
  })
  const pending = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Exportar CSV', exact: true }).click()
  expect((await pending).suggestedFilename()).toBe('seguimiento-utaped.csv')
  expect(exportQuery).toContain('groupId=' + group.id); expect(exportQuery).toContain('archived=true')

  // 4. Tablet and Dark mode in reporting
  await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  await page.setViewportSize({ width: 820, height: 1180 })
  await expect(page.getByRole('button', { name: 'Exportar CSV', exact: true })).toHaveCSS('background-color', 'rgb(28, 53, 76)')
  await expect(page.getByRole('button', { name: /^Abrir ejecución:/ })).toHaveCSS('color', 'rgb(219, 231, 242)')
  await page.screenshot({ path: 'test-results/management-reports-tablet-dark.png', fullPage: true })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()

  // 5. Error recovery in reporting
  unavailable = true
  await page.getByRole('button', { name: 'Actualizar', exact: true }).click()
  await expect(page.getByText('No se pudo actualizar. Se conserva la última consulta de estos filtros.')).toBeVisible()
  await expect(overview).toBeVisible()
  unavailable = false; empty = true
  await page.getByRole('button', { name: 'Reintentar reportes', exact: true }).click()
  await expect(overview.getByText('Sin resultados', { exact: true })).toBeVisible()
  await expect(overview.getByText('0 de 0 actividades informadas', { exact: true })).toBeVisible()
})

test('personal home requests only recent documents and pending work, avoiding analytical calls', async ({ page }) => {
  let reportingCalled = false
  let optionsCalled = false
  let recentCalled = false
  let pendingCalled = false

  await page.route('**/api/reporting**', route => {
    reportingCalled = true
    return route.fulfill({ status: 500, json: { message: 'Reporting should not be called from Inicio' } })
  })
  await page.route('**/api/work-plans/options**', route => {
    optionsCalled = true
    return route.fulfill({ status: 500, json: { message: 'Options should not be called from Inicio' } })
  })
  await page.route('**/api/tracking/recent**', route => {
    recentCalled = true
    return route.continue()
  })
  await page.route('**/api/pending-work?**', route => {
    pendingCalled = true
    return route.continue()
  })

  await login(page, 'dev.principal')
  await page.getByRole('button', { name: 'Inicio', exact: true }).click()

  // Verify Inicio regions
  const inbox = page.getByRole('region', { name: 'Mis pendientes', exact: true })
  await expect(inbox).toBeVisible()
  const recentCard = page.getByRole('region', { name: 'Documentos recientes', exact: true })
  await expect(recentCard).toBeVisible()

  // Analytical calls must NOT happen
  expect(reportingCalled).toBe(false)
  expect(optionsCalled).toBe(false)
  expect(recentCalled).toBe(true)
  expect(pendingCalled).toBe(true)

  // Verify header navigation button
  await expect(page.getByRole('button', { name: 'Ver reportes de gestión', exact: true })).toBeVisible()

  await page.screenshot({ path: 'test-results/home-desktop-separated-light.png', fullPage: true })
  await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  await page.screenshot({ path: 'test-results/home-desktop-separated-dark.png', fullPage: true })
  await page.setViewportSize({ width: 820, height: 1180 })
  await page.screenshot({ path: 'test-results/home-tablet-separated-dark.png', fullPage: true })
  await page.setViewportSize({ width: 1440, height: 900 })
  await page.getByRole('button', { name: 'Modo claro', exact: true }).click()

  // Open recent document
  const openFirstBtn = recentCard.getByRole('button', { name: /^Abrir/ }).first()
  if (await openFirstBtn.isVisible()) {
    await openFirstBtn.click()
    await expect(page).toHaveURL(/doc=/)
  }

  // Navigate back to Inicio and test "Ver todos los documentos"
  await page.getByRole('button', { name: 'Inicio', exact: true }).click()
  // Unroute mocks so Documentos can load its options normally
  await page.unroute('**/api/work-plans/options**')
  await page.unroute('**/api/reporting**')
  const viewAllBtn = recentCard.getByRole('button', { name: /Ver todos los documentos/i })
  if (await viewAllBtn.isVisible()) {
    await viewAllBtn.click()
    await expect(page.getByRole('heading', { name: 'Mis documentos', exact: true })).toBeVisible()
    await expect(page.locator('.table-scroll table, .empty-state').first()).toBeVisible()
  }
})

test('recent documents supports 0 items, 1 item, and more than 5 items without confusing total with item length', async ({ page }) => {
  const makeItem = (idx: number, state: 'DRAFT' | 'IN_REVIEW' | 'APPROVED' = 'DRAFT', editable = true) => ({
    id: randomUUID(),
    title: `Plan de gestión académica número ${idx}`,
    groupName: 'Comisión de carrera',
    periodName: 'Julio–Diciembre 2026',
    documentState: state,
    formalVersion: '1.0',
    updatedAt: '2026-10-30T10:15:00Z',
    editable,
    documentType: 'T1' as const,
    archivedAt: null,
    canManage: true,
    rowVersion: 1,
  })

  let currentResponse = { items: [] as unknown[], total: 0, page: 0, size: 5 }

  await page.route('**/api/tracking/recent', route =>
    route.fulfill({ json: currentResponse })
  )

  await login(page, 'dev.principal')
  const recentCard = page.getByRole('region', { name: 'Documentos recientes', exact: true })

  // 1. Scenario: 0 documents
  await expect(recentCard.getByText('Aún no tienes documentos disponibles.')).toBeVisible()
  await recentCard.getByRole('button', { name: 'Ver todos los documentos', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Mis documentos', exact: true })).toBeVisible()
  await expect(page.locator('.table-scroll table, .empty-state').first()).toBeVisible()

  // 2. Scenario: 1 document
  currentResponse = {
    items: [makeItem(1, 'DRAFT', false)],
    total: 1,
    page: 0,
    size: 5,
  }
  await page.getByRole('button', { name: 'Inicio', exact: true }).click()
  await expect(recentCard.getByText('Plan T1')).toBeVisible()
  await expect(recentCard.getByText('Borrador', { exact: true })).toBeVisible()
  await expect(recentCard.getByText('Solo lectura', { exact: true })).toBeVisible()
  await expect(recentCard.getByText(/Actualizado 30 oct 2026/)).toBeVisible()
  await expect(recentCard.getByRole('button', { name: 'Ver todos los documentos', exact: true })).toBeVisible()

  // 3. Scenario: 8 total documents accessible, returns top 5
  currentResponse = {
    items: [makeItem(1), makeItem(2), makeItem(3), makeItem(4), makeItem(5)],
    total: 8,
    page: 0,
    size: 5,
  }
  await recentCard.getByRole('button', { name: 'Actualizar documentos recientes', exact: true }).click()
  await expect(recentCard.locator('.tracking-list li')).toHaveCount(5)
  const viewAllWithCount = recentCard.getByRole('button', { name: 'Ver todos los documentos (8)', exact: true })
  await expect(viewAllWithCount).toBeVisible()
  await viewAllWithCount.click()
  await expect(page.getByRole('heading', { name: 'Mis documentos', exact: true })).toBeVisible()
})

test('recent documents and pending work isolate errors and support local recovery', async ({ page }) => {
  let failRecent = true
  let failPending = false

  await page.route('**/api/tracking/recent', route => {
    if (failRecent) {
      return route.fulfill({ status: 503, json: { message: 'Error temporal al cargar recientes.' } })
    }
    return route.fulfill({
      json: {
        items: [{
          id: randomUUID(),
          title: 'Plan recuperado exitosamente',
          groupName: 'Comisión',
          periodName: '2026',
          documentState: 'DRAFT',
          formalVersion: '1.0',
          updatedAt: '2026-10-30T11:00:00Z',
          editable: true,
          documentType: 'T1',
          archivedAt: null,
          canManage: true,
          rowVersion: 1,
        }],
        total: 1,
        page: 0,
        size: 5,
      },
    })
  })

  await page.route('**/api/pending-work?**', route => {
    if (failPending) {
      return route.fulfill({ status: 503, json: { message: 'Error temporal al cargar pendientes.' } })
    }
    return route.continue()
  })

  await login(page, 'dev.principal')

  // Recent failed: shows error and retry; does NOT show fake 0 documents
  const recentCard = page.getByRole('region', { name: 'Documentos recientes', exact: true })
  await expect(recentCard.getByText('No se pudieron consultar tus documentos recientes.')).toBeVisible()
  await expect(recentCard.getByRole('button', { name: 'Reintentar recientes', exact: true })).toBeVisible()
  await expect(recentCard.getByText('Aún no tienes documentos disponibles.')).toHaveCount(0)

  // PendingInbox remains functional
  const inbox = page.getByRole('region', { name: 'Mis pendientes', exact: true })
  await expect(inbox).toBeVisible()
  // With no pending work the inbox collapses into the «Al día» banner (2026-11-05); its filters open on demand.
  const expand = inbox.getByRole('button', { name: 'Ver filtros', exact: true })
  await expect(inbox.getByRole('button', { name: /^(Todos|Ver filtros$)/ }).first()).toBeVisible()
  if (await expand.isVisible()) await expand.click()
  await expect(inbox.getByRole('button', { name: /^Todos/ })).toBeVisible()

  // Recover recent
  failRecent = false
  await recentCard.getByRole('button', { name: 'Reintentar recientes', exact: true }).click()
  await expect(recentCard.getByText('Plan recuperado exitosamente')).toBeVisible()

  // Now fail pending work; recent remains intact
  failPending = true
  await inbox.getByRole('button', { name: 'Actualizar mis pendientes', exact: true }).click()
  await expect(inbox.getByText('No se pudieron consultar tus pendientes.')).toBeVisible()
  await expect(inbox.getByRole('button', { name: 'Reintentar pendientes', exact: true })).toBeVisible()
  await expect(recentCard.getByText('Plan recuperado exitosamente')).toBeVisible()
})

test('recent documents displays real status badges for in review and approved without replacing state with read only', async ({ page }) => {
  await page.route('**/api/tracking/recent', route =>
    route.fulfill({
      json: {
        items: [
          {
            id: randomUUID(),
            title: 'Plan en revisión por comité técnico',
            groupName: 'Comisión',
            periodName: '2026',
            documentState: 'IN_REVIEW',
            formalVersion: '1.0',
            updatedAt: '2026-10-30T10:00:00Z',
            editable: false,
            documentType: 'T1',
            archivedAt: null,
            canManage: true,
            rowVersion: 1,
          },
          {
            id: randomUUID(),
            title: 'Informe final de gestión de carrera',
            groupName: 'Comisión',
            periodName: '2026',
            documentState: 'APPROVED',
            formalVersion: '1.0',
            updatedAt: '2026-10-30T11:00:00Z',
            editable: false,
            documentType: 'T2',
            archivedAt: null,
            canManage: true,
            rowVersion: 1,
          },
          {
            id: randomUUID(),
            title: 'Borrador editable normal',
            groupName: 'Comisión',
            periodName: '2026',
            documentState: 'DRAFT',
            formalVersion: '1.0',
            updatedAt: '2026-10-30T12:00:00Z',
            editable: true,
            documentType: 'T1',
            archivedAt: null,
            canManage: true,
            rowVersion: 1,
          },
        ],
        total: 3,
        page: 0,
        size: 5,
      },
    })
  )

  await login(page, 'dev.principal')
  const recentCard = page.getByRole('region', { name: 'Documentos recientes', exact: true })

  // Verify real status badges
  await expect(recentCard.getByText('En revisión', { exact: true })).toBeVisible()
  await expect(recentCard.getByText('Aprobado', { exact: true })).toBeVisible()
  await expect(recentCard.getByText('Borrador', { exact: true })).toBeVisible()

  // Verify Solo lectura is a secondary pill, not replacing the document state
  const items = recentCard.locator('.tracking-item')
  await expect(items.nth(0).getByText('En revisión', { exact: true })).toBeVisible()
  await expect(items.nth(0).getByText('Solo lectura', { exact: true })).toBeVisible()

  await expect(items.nth(1).getByText('Aprobado', { exact: true })).toBeVisible()
  await expect(items.nth(1).getByText('Solo lectura', { exact: true })).toBeVisible()

  await expect(items.nth(2).getByText('Borrador', { exact: true })).toBeVisible()
  await expect(items.nth(2).getByText('Solo lectura', { exact: true })).toHaveCount(0)
})

test('administrative home includes institutional indicators without implicit private document access', async ({ page }) => {
  await login(page, 'dev.admin')
  const metrics = await api(page, '/reporting')
  expect(metrics.upcoming).toEqual([])
  await expect(page.getByRole('region', { name: 'Indicadores de gestión', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Ver reportes de gestión', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Reportes de gestión', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: /^Abrir ejecución:/ })).toHaveCount(0)
})


test('report loading isolates catalogs, preserves same-filter refresh and hides previous filters', async ({ page }) => {
  await login(page, 'dev.principal')
  const groups = await api(page, '/groups')
  let catalogCalls = 0, reportCalls = 0
  let releaseCatalogs!: () => void
  const catalogGate = new Promise<void>(resolve => { releaseCatalogs = resolve })
  const pending: (() => void)[] = []
  let fail = false
  const summary = { rows: [], plans: 7, reports: 0, approved: 0, inReview: 0, drafts: 7, activities: 0, completedActivities: 0, pendingEvidence: 0, dueSoonActivities: 0, overdueActivities: 0, declaredExecution: null, declaredActivities: 0, declarationCoverage: 0, dueSoonDays: 7, upcoming: [] }
  await page.route(url => ['/api/groups', '/api/periods'].includes(url.pathname), async route => {
    catalogCalls++
    await catalogGate
    await route.continue()
  })
  await page.route(url => url.pathname === '/api/reporting', async route => {
    reportCalls++
    await new Promise<void>(resolve => pending.push(resolve))
    await route.fulfill(fail ? { status: 503, json: { message: 'Reporte temporalmente no disponible' } } : { json: summary })
  })
  await page.getByRole('button', { name: 'Reportes de gestión', exact: true }).click()
  const overview = page.getByRole('region', { name: 'Indicadores de gestión', exact: true })
  await expect(page.getByRole('region', { name: 'Cargando reporte', exact: true })).toBeVisible()
  await expect.poll(() => pending.length).toBe(1)
  pending.shift()!()
  await expect(overview).toBeVisible() // Catalogs are still pending.
  releaseCatalogs()
  await expect(page.getByLabel('Grupo del reporte').locator('option[value="' + groups[0].id + '"]')).toHaveCount(1)
  const baselineCatalogCalls = catalogCalls
  await page.getByRole('button', { name: 'Actualizar', exact: true }).click()
  await expect.poll(() => pending.length).toBe(1)
  await expect(overview).toBeVisible()
  await expect(page.getByText('Actualizando reporte…', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Exportar CSV', exact: true })).toBeDisabled()
  fail = true
  pending.shift()!()
  await expect(page.getByText('No se pudo actualizar. Se conserva la última consulta de estos filtros.')).toBeVisible()
  await expect(overview).toBeVisible()
  expect(catalogCalls).toBe(baselineCatalogCalls)
  fail = false
  await page.getByLabel('Grupo del reporte').selectOption(groups[0].id)
  await expect.poll(() => pending.length).toBe(1)
  await expect(overview).toHaveCount(0)
  await expect(page.getByRole('region', { name: 'Cargando reporte', exact: true })).toBeVisible()
  await page.screenshot({ path: 'test-results/report-skeleton-desktop.png', fullPage: true })
  await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  await page.setViewportSize({ width: 820, height: 1180 })
  await page.screenshot({ path: 'test-results/report-skeleton-tablet-dark.png', fullPage: true })
  fail = true
  pending.shift()!()
  await expect(page.getByRole('region', { name: 'Reportes no disponibles' })).toBeVisible()
  await expect(overview).toHaveCount(0) // Never label previous-filter data as current.
  fail = false
  await page.getByRole('button', { name: 'Reintentar reportes', exact: true }).click()
  await expect.poll(() => pending.length).toBe(1)
  pending.shift()!()
  await expect(overview).toBeVisible()
  expect(catalogCalls).toBe(baselineCatalogCalls)
  expect(reportCalls).toBe(4)
})
