import { test, expect } from '@playwright/test'
import { api, login } from './sample-fixtures'

test('document ordering preserves search filters and works on desktop and dark tablet', async ({ page }) => {
  await login(page, 'dev.principal')
  await page.getByRole('button', { name: 'Documentos', exact: true }).click()
  await page.getByLabel('Buscar por título', { exact: true }).fill('Plan')
  await page.getByRole('button', { name: 'Buscar', exact: true }).click()
  const order = page.getByLabel('Ordenar por', { exact: true })
  for (const sort of ['TITLE_ASC', 'TITLE_DESC', 'UPDATED_ASC', 'UPDATED_DESC']) {
    const response = page.waitForResponse(response => response.url().includes('/api/work-plans?') && new URL(response.url()).searchParams.get('sort') === sort)
    await order.selectOption(sort); await response
    const expected = await api(page, '/work-plans?query=Plan&sort=' + sort)
    await expect(page.locator('.documents-table tbody td:first-child strong')).toHaveText(expected.items.map((item: { title: string }) => item.title))
    await expect(page.getByLabel('Buscar por título', { exact: true })).toHaveValue('Plan')
  }
  await page.screenshot({ path: 'test-results/document-ordering-desktop.png', fullPage: true })
  await page.setViewportSize({ width: 820, height: 1180 }); await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  await expect(order).toBeVisible()
  const box = await order.boundingBox(); expect(box!.width).toBeGreaterThan(150)
  await page.screenshot({ path: 'test-results/document-ordering-tablet-dark.png', fullPage: true })
})
