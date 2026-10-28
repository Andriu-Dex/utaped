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
  await page.getByRole('button', { name: 'Inicio', exact: true }).click()
  await page.getByRole('button', { name: 'Actualizar seguimiento', exact: true }).click()
  const overview = page.getByRole('region', { name: 'Indicadores de gestión', exact: true })
  await expect(overview.getByText('70 %', { exact: true })).toBeVisible()
  await expect(overview.getByText('4 de 10 actividades informadas', { exact: true })).toBeVisible()
  await expect(overview.getByText('Cobertura de resultados: 40 %', { exact: true })).toBeVisible()
  await expect(overview.getByRole('button', { name: /^Abrir ejecución:/ })).toBeVisible()
  await page.screenshot({ path: 'test-results/management-home-desktop.png', fullPage: true })
  await page.getByRole('button', { name: 'Ver reportes de gestión', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Reportes de gestión', exact: true })).toBeVisible()
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
  await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  await page.setViewportSize({ width: 820, height: 1180 })
  await expect(page.getByRole('button', { name: 'Exportar CSV', exact: true })).toHaveCSS('background-color', 'rgb(28, 53, 76)')
  await expect(page.getByRole('button', { name: /^Abrir ejecución:/ })).toHaveCSS('color', 'rgb(219, 231, 242)')
  await page.screenshot({ path: 'test-results/management-reports-tablet-dark.png', fullPage: true })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
  unavailable = true
  await page.getByRole('button', { name: 'Actualizar', exact: true }).click()
  await expect(page.getByRole('region', { name: 'Reportes no disponibles' })).toBeVisible()
  await expect(overview).toHaveCount(0)
  unavailable = false; empty = true
  await page.getByRole('button', { name: 'Reintentar reportes', exact: true }).click()
  await expect(overview.getByText('Sin resultados', { exact: true })).toBeVisible()
  await expect(overview.getByText('0 de 0 actividades informadas', { exact: true })).toBeVisible()
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
