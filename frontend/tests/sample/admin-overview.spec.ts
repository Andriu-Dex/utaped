import { test, expect } from '@playwright/test'
import { api, login } from './sample-fixtures'

test('executive overview keeps four indicators, independent windows, actionable alerts and report context', async ({ page }) => {
  await login(page, 'dev.admin')
  const live = await api(page, '/admin/overview')
  expect(live.reporting.upcoming).toEqual([])
  expect(live.selectedPeriodId).toBeTruthy()
  const selected = live.periods.find((period: { id: string }) => period.id === live.selectedPeriodId)
  const reviewEnd = new Date(live.today + 'T12:00:00Z'); reviewEnd.setUTCDate(reviewEnd.getUTCDate() + 2)
  const row = { groupId: 'f0000000-0000-4000-8000-000000000001', groupName: 'Comisión de seguimiento académico', careerName: 'Ingeniería de Software', periodName: selected.name, plans: 7, reports: 5, approved: 5, inReview: 3, drafts: 4, activities: 5, completedActivities: 2, evidenceCoverage: 40, declaredExecution: 75, declaredActivities: 2, pendingEvidence: 3, dueSoonActivities: 1, overdueActivities: 1 }
  const data = { ...live, users: 10, activeUsers: 8, activeGroups: 3, periods: [{ ...selected, windows: [{ label: 'Elaboración', startsOn: live.today, endsOn: live.today, status: 'OPEN', remainingDays: 0 }, { label: 'Revisión', startsOn: live.today, endsOn: reviewEnd.toISOString().slice(0, 10), status: 'OPEN', remainingDays: 2 }] }], reporting: { ...live.reporting, rows: [row], plans: 7, reports: 5, approved: 5, inReview: 3, drafts: 4, activities: 5, completedActivities: 2, declaredExecution: 75, declaredActivities: 2, declarationCoverage: 40, pendingEvidence: 3, overdueActivities: 1 }, documentTypes: [{ type: 'T1', count: 7 }, { type: 'T2', count: 2 }, { type: 'T3', count: 3 }], alerts: { scopesWithoutCoordinator: 2, reviewsOutsideWindow: 1, pendingEvidence: 3, overdueActivities: 1 }, events: [{ id: 'one', action: 'LOGIN_SUCCEEDED', actorName: 'María Salazar', occurredAt: live.updatedAt }, { id: 'two', action: 'USER_CREATED', actorName: 'María Salazar', occurredAt: live.updatedAt }, { id: 'three', action: 'REVIEW_TASK_ASSIGNED', actorName: 'María Salazar', occurredAt: live.updatedAt }] }
  let unavailable = false
  const queries: string[] = []
  await page.route('**/api/admin/overview?**', route => {
    const params = new URL(route.request().url()).searchParams; queries.push(params.toString())
    const all = params.get('allPeriods') === 'true'
    const empty = { ...data, selectedPeriodId: null, reporting: { ...data.reporting, rows: [], plans: 0, reports: 0, approved: 0, inReview: 0, drafts: 0, activities: 0, declaredActivities: 0, declarationCoverage: 0, declaredExecution: null, pendingEvidence: 0 }, documentTypes: [{ type: 'T1', count: 0 }, { type: 'T2', count: 0 }, { type: 'T3', count: 0 }], alerts: { scopesWithoutCoordinator: 0, reviewsOutsideWindow: 0, pendingEvidence: 0, overdueActivities: 0 } }
    return unavailable ? route.fulfill({ status: 503, json: { message: 'Resumen temporalmente no disponible.' } }) : route.fulfill({ json: all ? empty : data })
  })
  await page.getByRole('button', { name: 'Actualizar panel', exact: true }).click()
  const indicators = page.getByRole('region', { name: 'Indicadores de gestión', exact: true })
  await expect(indicators.getByRole('button')).toHaveCount(4)
  await expect(indicators.getByText('75 %', { exact: true })).toBeVisible()
  await expect(indicators.getByText(/Cobertura 40 %/)).toBeVisible()
  await expect(page.getByText('Cierra hoy', { exact: true })).toBeVisible()
  await expect(page.getByText('2 días calendario restantes', { exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Configuración institucional', exact: true })).toHaveCount(0)
  await expect(page.getByRole('img', { name: 'Distribución de 12 documentos por estado' })).toBeVisible()
  const alerts = page.getByRole('region', { name: 'Alertas operativas', exact: true })
  await expect(alerts.getByRole('listitem')).toHaveCount(4)
  await expect(page.getByRole('region', { name: 'Actividad administrativa reciente' }).getByText('Seguridad', { exact: true })).toBeVisible()
  await page.screenshot({ path: 'test-results/executive-overview-desktop.png', fullPage: true, animations: 'disabled' })
  await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  await page.setViewportSize({ width: 820, height: 1180 })
  await page.screenshot({ path: 'test-results/executive-overview-tablet-dark.png', fullPage: true, animations: 'disabled' })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
  await page.getByLabel('Período del panel').selectOption('all')
  await expect.poll(() => queries.at(-1)).toContain('allPeriods=true')
  await expect(page.getByText('Sin alertas en los controles consultados.', { exact: true })).toBeVisible()
  await expect(indicators.getByText('Sin resultados', { exact: true })).toBeVisible()
  await expect(page.getByRole('img', { name: 'Sin documentos para este período' })).toBeVisible()
  unavailable = true
  await page.getByRole('button', { name: 'Actualizar panel', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Reintentar panel', exact: true })).toBeVisible()
  await expect(page.getByText('Sin alertas en los controles consultados.', { exact: true })).toHaveCount(0)
  unavailable = false
  await page.getByRole('button', { name: 'Reintentar panel', exact: true }).click()
  await page.getByLabel('Período del panel').selectOption(selected.id)
  await expect.poll(() => queries.at(-1)).toContain('periodId=' + selected.id)
  await page.getByLabel('Período del panel').selectOption(selected.id)
  await expect(indicators.getByRole('button')).toHaveCount(4)
  await page.getByRole('button', { name: 'Ver reportes de gestión', exact: true }).click()
  await expect(page.getByLabel('Período del reporte')).toHaveValue(selected.id)
  await expect(page.getByRole('button', { name: /^Abrir ejecución:/ })).toHaveCount(0)
})
