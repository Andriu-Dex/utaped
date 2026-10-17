import { randomUUID } from 'node:crypto'
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

test('fictional people have realistic names in the profile and activity responsibilities', async ({ page }) => {
  await login(page, 'dev.principal')
  await expect(page.locator('.sidebar-account')).toContainText('Andrés Sebastián Molina Vera')
  const options = await api(page, '/work-plans/options')
  const scope = options.scopes.find((item: { canCreate: boolean }) => item.canCreate)
  expect(scope).toBeTruthy()
  const plan = await api(page, '/work-plans', 'POST', { scopeId: scope.id, groupId: scope.groupId, periodId: scope.periodId, requestKey: randomUUID() })
  await page.getByRole('button', { name: 'Documentos', exact: true }).click()
  await page.getByLabel('Buscar por título').fill(plan.title)
  await page.getByRole('button', { name: 'Buscar', exact: true }).click()
  await page.getByRole('button', { name: 'Editar ' + plan.title, exact: false }).click()
  await page.getByRole('button', { name: /3\s*Actividades y matriz/ }).click()
  await openAction(page, 'Agregar actividad')
  await page.getByLabel('Título de Otra actividad').fill('Coordinar el seguimiento académico del período')
  await page.getByRole('button', { name: 'Agregar actividad', exact: true }).click()
  const people = page.getByRole('region', { name: 'Actividad 1', exact: true }).getByRole('group', { name: 'Responsables', exact: true })
  for (const name of ['Andrés Sebastián Molina Vera', 'Patricia Elizabeth Cárdenas Ríos', 'Luis Fernando Acosta León']) {
    await expect(people.getByRole('checkbox', { name, exact: true })).toBeVisible()
  }
  await expect(people).not.toContainText('Desarrollo')
  await people.screenshot({ path: 'test-results/realistic-responsibles-desktop.png', animations: 'disabled' })
  await page.setViewportSize({ width: 820, height: 1180 })
  await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await people.screenshot({ path: 'test-results/realistic-responsibles-tablet-dark.png', animations: 'disabled' })
})
