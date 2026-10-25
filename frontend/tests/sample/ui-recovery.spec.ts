import { test, expect } from '@playwright/test'
import { login } from './sample-fixtures'

test('failed lists, reports and organization queries are recoverable without false empty states', async ({ page }) => {
  await login(page, 'dev.coordinator')
  let documentsFail = true, reportsFail = true, scopesFail = true, accountsFail = true, historyFail = true
  await page.route(url => url.pathname === '/api/work-plans', route => documentsFail && route.request().method() === 'GET' ? route.fulfill({ status: 503, json: { message: 'Lista temporalmente no disponible' } }) : route.continue())
  await page.getByRole('button', { name: 'Documentos', exact: true }).click()
  await expect(page.getByRole('region', { name: 'Documentos no disponibles', exact: true })).toBeVisible()
  await expect(page.getByText('No hay documentos que coincidan con los filtros.', { exact: true })).toHaveCount(0)
  documentsFail = false; await page.getByRole('button', { name: 'Reintentar documentos', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Mis documentos', exact: true })).toBeVisible()
  await expect(page.getByRole('region', { name: 'Documentos no disponibles', exact: true })).toHaveCount(0)

  await page.route(url => url.pathname === '/api/reporting', route => reportsFail ? route.fulfill({ status: 503, json: { message: 'Reporte temporalmente no disponible' } }) : route.continue())
  await page.getByRole('button', { name: 'Reportes de gestión', exact: true }).click()
  await expect(page.getByRole('region', { name: 'Reportes no disponibles', exact: true })).toBeVisible()
  await expect(page.getByText('Consultando reportes…', { exact: true })).toHaveCount(0)
  reportsFail = false; await page.getByRole('button', { name: 'Reintentar reportes', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Resultados documentales', exact: true })).toBeVisible()

  await page.route(url => url.pathname === '/api/organization/scopes', route => scopesFail ? route.fulfill({ status: 503, json: { message: 'Ámbitos temporalmente no disponibles' } }) : route.continue())
  await page.route('**/api/organization/scopes/*/accounts', route => accountsFail ? route.fulfill({ status: 503, json: { message: 'Cuentas temporalmente no disponibles' } }) : route.continue())
  await page.route('**/api/organization/scopes/*/history', route => historyFail ? route.fulfill({ status: 503, json: { message: 'Historial temporalmente no disponible' } }) : route.continue())
  await page.getByRole('button', { name: 'Mi organización', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Reintentar ámbitos', exact: true })).toBeVisible()
  await expect(page.getByText('No tiene ámbitos asignados.', { exact: true })).toHaveCount(0)
  scopesFail = false; await page.getByRole('button', { name: 'Reintentar ámbitos', exact: true }).click()
  const scope = page.getByRole('combobox', { name: 'Mi ámbito institucional', exact: true })
  await expect(scope.locator('option').nth(1)).toBeAttached()
  await scope.selectOption((await scope.locator('option').nth(1).getAttribute('value'))!)
  await expect(page.getByRole('button', { name: 'Reintentar cuentas del ámbito', exact: true })).toBeVisible()
  accountsFail = false; await page.getByRole('button', { name: 'Reintentar cuentas del ámbito', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Reintentar cuentas del ámbito', exact: true })).toHaveCount(0)
  await page.getByText('Historial de cargos y responsables', { exact: true }).click()
  await expect(page.getByRole('button', { name: 'Reintentar historial del ámbito', exact: true })).toBeVisible()
  historyFail = false; await page.getByRole('button', { name: 'Reintentar historial del ámbito', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Designaciones de responsable principal', exact: true })).toBeVisible()
})
