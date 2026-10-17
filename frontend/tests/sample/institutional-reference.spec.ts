import { withConfirmation } from '../confirmation'
import { test, expect } from '@playwright/test'
import { api, login } from './sample-fixtures'
import { openAction } from '../open-action'

test('template-based example scopes use July–December 2026 inside the document creation modal', async ({ page }) => {
  await login(page, 'dev.principal')
  const options = await api(page, '/work-plans/options')
  const period = options.periods.find((p: { name: string }) => p.name === 'Julio–Diciembre 2026')
  expect(period).toBeTruthy()
  expect(period.preparationStartsOn).toBe('2026-07-01')
  expect(period.preparationEndsOn).toBe('2026-12-31')
  const scopes = options.scopes.filter((s: { periodId: string; groupName: string }) => s.periodId === period.id && s.groupName === 'Unidad de Titulación')
  expect(scopes).toHaveLength(2)
  expect(scopes.map((s: { careerName: string }) => s.careerName).sort()).toEqual(['Ingeniería de Software', 'Tecnologías de la Información'])
  const scope = scopes.find((s: { careerName: string }) => s.careerName === 'Tecnologías de la Información')
  await page.getByRole('button', { name: 'Documentos', exact: true }).click()
  await openAction(page, 'Crear documento')
  await page.getByLabel('Ámbito del nuevo Plan').selectOption(scope.id)
  await expect(page.getByLabel('Título del nuevo Plan')).toHaveValue('Plan de Trabajo: Unidad de Titulación')
  await expect(page.getByLabel('Ámbito del nuevo Plan').locator('option:checked')).toContainText('Julio–Diciembre 2026')
   await withConfirmation(page, false, async () => { await page.keyboard.press('Escape') });
  await expect(page.getByRole('dialog', { name: 'Crear documento', exact: true })).toBeVisible()
  await expect(page.getByLabel('Ámbito del nuevo Plan')).toHaveValue(scope.id)
   await withConfirmation(page, true, async () => { await page.keyboard.press('Escape') });
  await expect(page.getByRole('dialog', { name: 'Crear documento', exact: true })).not.toBeVisible()
  await openAction(page, 'Crear documento')
  await expect(page.getByLabel('Ámbito del nuevo Plan')).toHaveValue('')
})
