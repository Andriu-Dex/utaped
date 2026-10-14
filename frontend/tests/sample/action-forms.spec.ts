import { test, expect } from '@playwright/test'
import { login } from './sample-fixtures'
import { openAction } from '../open-action'

test('administrative creation forms stay closed and cancellation clears only confirmed drafts', async ({ page }) => {
  await login(page, 'dev.admin')
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: 'Grupos y pertenencias', exact: true }).click()
  await expect(page.getByLabel('Nombre del grupo')).toHaveCount(0)
  await expect(page.getByLabel('Buscar usuario para asignar')).toHaveCount(0)
  await openAction(page, 'Crear grupo')
  await expect(page.getByLabel('Nombre del grupo')).toBeFocused()
  await page.getByLabel('Nombre del grupo').fill('Borrador que se debe descartar')
  page.once('dialog', dialog => dialog.dismiss()); await page.keyboard.press('Escape')
  await expect(page.getByLabel('Nombre del grupo')).toHaveValue('Borrador que se debe descartar')
  page.once('dialog', dialog => dialog.accept()); await page.keyboard.press('Escape')
  await expect(page.getByLabel('Nombre del grupo')).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Crear grupo', exact: true })).toBeFocused()
  await openAction(page, 'Crear grupo'); await expect(page.getByLabel('Nombre del grupo')).toHaveValue('')
  await page.getByRole('button', { name: 'Cancelar: Crear grupo', exact: true }).click()
  const screens = [
    ['Períodos académicos', 'Nombre del período', 'Crear período'],
    ['Carreras y ámbitos', 'Código de carrera', 'Crear carrera'],
    ['Recursos y medios', 'Denominación del catálogo', 'Crear o editar elemento'],
  ]
  for (const [screen, field, action] of screens) {
    await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: screen, exact: true }).click()
    await expect(page.getByLabel(field, { exact: true })).toHaveCount(0)
    await expect(page.getByRole('button', { name: action, exact: true })).toBeVisible()
  }
  await page.screenshot({ path: 'test-results/action-forms-admin-desktop.png', fullPage: true })
})

test('document creation is opt-in while search and access remain available', async ({ page }) => {
  await login(page, 'dev.principal')
  await page.getByRole('button', { name: 'Documentos', exact: true }).click()
  await expect(page.getByRole('radio', { name: /^Plan de Trabajo/ })).toHaveCount(0)
  await expect(page.getByLabel('Buscar por título')).toBeVisible()
  await openAction(page, 'Crear documento')
  await expect(page.getByRole('radio', { name: /^Plan de Trabajo/ })).toBeChecked()
  await page.getByRole('button', { name: 'Cancelar: Crear documento', exact: true }).click()
  await expect(page.getByRole('radio', { name: /^Plan de Trabajo/ })).toHaveCount(0)
  await page.setViewportSize({ width: 820, height: 1180 })
  await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  await page.screenshot({ path: 'test-results/action-forms-documents-tablet-dark.png', fullPage: true })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})
