import { test, expect } from '@playwright/test'
import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { login } from './sample-fixtures'
import { openAction } from '../open-action'

test('production frontend contains no native alert, confirm or prompt calls', () => {
  const offending: string[] = []
  function inspect(path: string) {
    for (const entry of readdirSync(path, { withFileTypes: true })) {
      const file = join(path, entry.name)
      if (entry.isDirectory()) inspect(file)
      else if (/\.[jt]sx?$/.test(file) && /\b(?:window\.|globalThis\.|self\.)?(?:alert|confirm|prompt)\s*\(/.test(readFileSync(file, 'utf8'))) offending.push(file)
    }
  }
  inspect('src'); expect(offending).toEqual([])
})

test('nested confirmation preserves the draft, theme and focus without browser dialogs', async ({ page }) => {
  const native: string[] = []
  page.on('dialog', async dialog => { native.push(dialog.type()); await dialog.dismiss() })
  await login(page, 'dev.principal')
  await page.getByRole('button', { name: 'Documentos', exact: true }).click()
  await openAction(page, 'Crear documento')
  const creation = page.getByRole('dialog', { name: 'Crear documento', exact: true })
  const scope = await page.getByLabel('Ámbito del nuevo Plan').locator('option').evaluateAll(options => (options.find(o => (o as HTMLOptionElement).value) as HTMLOptionElement).value)
  await page.getByLabel('Ámbito del nuevo Plan').selectOption(scope)
  await page.route('**/api/work-plans', route => route.abort())
  await page.getByRole('button', { name: 'Crear borrador', exact: true }).click()
  await expect(creation.getByRole('alert')).toContainText('No se pudo conectar con el servidor.')
  await expect(page.getByLabel('Ámbito del nuevo Plan')).toHaveValue(scope)
  await creation.getByRole('button', { name: 'Cerrar aviso', exact: true }).click()
  await page.getByRole('radio', { name: /^Informe/ }).click()
  const confirmation = page.getByRole('dialog', { name: 'Confirmar acción', exact: true })
  await expect(confirmation).toBeVisible()
  await expect(confirmation.locator('#toast-root')).toHaveCount(1)
  await expect(confirmation.getByRole('button', { name: 'Cancelar', exact: true })).toBeFocused()
  const toggle = confirmation.getByRole('button', { name: 'Modo oscuro', exact: true })
  await toggle.click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await expect(confirmation.getByRole('button', { name: 'Modo claro', exact: true })).toBeVisible()
  for (let i = 0; i < 8; i++) {
    await page.keyboard.press(i % 2 ? 'Shift+Tab' : 'Tab')
    expect(await page.evaluate(() => document.activeElement?.closest('dialog')?.getAttribute('aria-labelledby'))).toBe('confirmation-title')
  }
  await page.screenshot({ path: 'test-results/confirmation-nested-dark.png', animations: 'disabled' })
  await page.keyboard.press('Escape')
  await expect(confirmation).not.toBeVisible()
  await expect(creation).toBeVisible()
  await expect(creation.locator('#toast-root')).toHaveCount(1)
  await expect(page.getByLabel('Ámbito del nuevo Plan')).toHaveValue(scope)
  await expect(page.getByRole('radio', { name: /^Plan de Trabajo/ })).toBeChecked()
  await page.getByRole('radio', { name: /^Informe/ }).click()
  await confirmation.getByRole('button', { name: 'Confirmar', exact: true }).click()
  await expect(page.getByRole('radio', { name: /^Informe/ })).toBeChecked()
  await page.getByRole('button', { name: 'Cancelar: Crear documento', exact: true }).click()
  await expect(creation).not.toBeVisible()
  expect(await page.evaluate(() => document.body.style.overflow)).not.toBe('hidden')
  expect(native).toEqual([])
})
