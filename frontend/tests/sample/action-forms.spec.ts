import { withConfirmation } from '../confirmation'
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
   await withConfirmation(page, false, async () => { await page.keyboard.press('Escape') });
  await expect(page.getByLabel('Nombre del grupo')).toHaveValue('Borrador que se debe descartar')
   await withConfirmation(page, true, async () => { await page.keyboard.press('Escape') });
  await expect(page.getByLabel('Nombre del grupo')).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Crear grupo', exact: true })).toBeFocused()
  await openAction(page, 'Crear grupo'); await expect(page.getByLabel('Nombre del grupo')).toHaveValue('')
  await page.getByRole('button', { name: 'Cancelar: Crear grupo', exact: true }).click()
  const screens = [
    ['Períodos académicos', 'Nombre del período', 'Crear período'],
    ['Carreras y ámbitos', 'Código de carrera', 'Crear carrera'],
    ['Plantillas', 'Texto predeterminado de la Justificación:', 'Editar plantilla'],
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
  const dialog = page.getByRole('dialog', { name: 'Crear documento', exact: true })
  await expect(dialog).toBeVisible()
  expect(await page.evaluate(() => document.body.style.overflow)).toBe('hidden')
  for (let i = 0; i < 12; i++) {
    await page.keyboard.press('Tab')
    expect(await page.evaluate(() => !!document.activeElement?.closest('dialog'))).toBe(true)
  }
  await expect(dialog.locator('.document-creation')).toHaveCSS('opacity', '1')
  await page.screenshot({ path: 'test-results/create-document-modal-desktop.png', animations: 'disabled' })
  await expect(page.getByRole('radio', { name: /^Plan de Trabajo/ })).toBeChecked()
  await page.keyboard.press('Escape')
  await expect(dialog).not.toBeVisible()
  await expect(page.getByRole('button', { name: 'Crear documento', exact: true })).toBeFocused()
  expect(await page.evaluate(() => document.body.style.overflow)).not.toBe('hidden')
  await expect(page.getByRole('radio', { name: /^Plan de Trabajo/ })).toHaveCount(0)
  await page.setViewportSize({ width: 820, height: 1180 })
  await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  await openAction(page, 'Crear documento')
  await page.screenshot({ path: 'test-results/action-forms-documents-tablet-dark.png', animations: 'disabled' })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
})
