import { test, expect } from '@playwright/test'
import { login, api } from './sample-fixtures'
import { openAction } from '../open-action'

test('la revisión permanece accesible y la búsqueda exige confirmar la comisión', async ({ page }) => {
  await login(page, 'dev.principal')
  await expect(page.getByRole('button', { name: 'Revisión documental', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Documentos', exact: true }).click()
  await openAction(page, 'Crear documento')
  const dialog = page.getByRole('dialog', { name: 'Crear documento', exact: true })
  const input = dialog.getByRole('combobox', { name: 'Comisión o unidad asignada' })
  await input.focus()
  const first = dialog.getByRole('option').first()
  const firstId = await first.getAttribute('id')
  await expect(input).toHaveAttribute('aria-activedescendant', firstId!)
  await input.press('Enter')
  await expect(dialog.getByRole('button', { name: 'Crear borrador', exact: true })).toBeEnabled()
  await input.fill('Comisión inexistente 234567')
  await expect(dialog.getByRole('button', { name: 'Crear borrador', exact: true })).toBeDisabled()
  await expect(dialog.getByText(/No hay comisiones que coincidan/)).toBeVisible()
})

test('la vista previa en memoria no sobrevive al cierre de sesión', async ({ page }) => {
  await login(page, 'dev.principal')
  const options = await api(page, '/work-plans/options')
  const scope = options.scopes.find((item: { canCreate: boolean }) => item.canCreate)
  const plan = await api(page, '/work-plans', 'POST', { scopeId: scope.id, groupId: scope.groupId, periodId: scope.periodId, requestKey: crypto.randomUUID() })
  const path = '/?view=documents&doc=' + plan.id + '&step=general&workspace=documents'
  await page.goto(path)
  const viewer = page.getByRole('complementary', { name: 'Previsualización simultánea del Plan' })
  await expect(viewer.locator('img.document-page')).toBeVisible({ timeout: 90000 })
  await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click()
  await login(page, 'dev.principal')
  let release!: () => void
  const blocked = new Promise<void>(resolve => { release = resolve })
  await page.route('**/artifacts/draft-preview', async route => { await blocked; await route.continue() })
  const request = page.waitForRequest(r => r.url().includes('/artifacts/draft-preview'))
  await page.goto(path)
  await request
  try { await expect(viewer.locator('img.document-page')).toHaveCount(0) }
  finally { release() }
  await expect(viewer.locator('img.document-page')).toBeVisible({ timeout: 90000 })
})
