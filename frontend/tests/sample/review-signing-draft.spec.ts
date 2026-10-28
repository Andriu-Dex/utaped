import { randomUUID } from 'node:crypto'
import { test, expect } from '@playwright/test'
import { login } from './sample-fixtures'
import { openAction } from '../open-action'
import { withConfirmation } from '../confirmation'

test('review certificate preparation is protected on return, module navigation and cancellation', async ({ page }) => {
  const actor = await login(page, 'dev.reviewer'), reviewId = randomUUID(), taskId = randomUUID()
  const title = 'Revisión protegida de aceptación'
  await page.route('**/api/reviews/inbox?*', route => route.fulfill({ json: { items: [{ id: taskId, reviewId, documentId: randomUUID(), title, groupName: 'Comisión de aceptación', periodName: 'Julio–Diciembre 2026', stageLabel: 'Revisión', requiresSignature: true, status: 'ACTIVE', roundNumber: 1 }], total: 1, page: 0, size: 20, metrics: { active: 1, completed: 0, returned: 0 } } }))
  await page.route('**/api/reviews/' + reviewId, route => route.fulfill({ json: { review: { id: reviewId, status: 'IN_REVIEW', round_number: 1 }, tasks: [{ id: taskId, actor_id: actor.id, actor_name: actor.displayName, stage_label: 'Revisión', requires_signature: true, status: 'ACTIVE', row_version: 0 }], observations: [], pageCount: 1, pdfHash: 'a'.repeat(64), canAct: true } }))
  await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({ contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64') }))
  await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
  await page.getByRole('button', { name: 'Revisar ' + title + ' · Ronda 1', exact: true }).click()
  const pageNavigator = page.locator('.review-document > .document-page-navigator')
  await expect(pageNavigator).toBeVisible()
  await expect(pageNavigator).toHaveCSS('position', 'sticky')
  await page.locator('.review-page').evaluate(element => { element.style.minHeight = '1600px' })
  for (const width of [1440, 820]) {
    await page.setViewportSize({ width, height: 1000 })
    await pageNavigator.evaluate(element => element.scrollIntoView({ block: 'start' }))
    await page.evaluate(() => window.scrollBy(0, 200))
    await expect.poll(async () => {
      const nav = await pageNavigator.boundingBox(), bar = await page.locator('.topbar').boundingBox()
      return nav && bar ? Math.abs(nav.y - bar.y - bar.height - 8) : 100
    }).toBeLessThan(2)
  }
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.evaluate(() => window.scrollTo(0, 0))
  await openAction(page, 'Preparar firma de revisión')
  await page.getByLabel('Certificado personal (.p12/.pfx)', { exact: true }).setInputFiles({ name: 'prueba.p12', mimeType: 'application/x-pkcs12', buffer: Buffer.from('temporary-test-container') })
  await page.getByLabel('Contraseña del certificado', { exact: true }).fill('Temporary-test-password')
  await withConfirmation(page, false, () => page.getByRole('button', { name: 'Volver', exact: true }).click())
  await expect(page.getByLabel('Contraseña del certificado', { exact: true })).toHaveValue('Temporary-test-password')
  await withConfirmation(page, false, () => page.getByRole('button', { name: 'Inicio', exact: true }).click())
  await expect(page.getByLabel('Contraseña del certificado', { exact: true })).toHaveValue('Temporary-test-password')
  await withConfirmation(page, false, () => page.getByRole('button', { name: 'Cancelar: Preparar firma de revisión', exact: true }).click())
  await expect(page.getByLabel('Contraseña del certificado', { exact: true })).toHaveValue('Temporary-test-password')
  await withConfirmation(page, true, () => page.getByRole('button', { name: 'Cancelar: Preparar firma de revisión', exact: true }).click())
  await expect(page.getByLabel('Contraseña del certificado', { exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: 'Volver', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Bandeja de revisión', exact: true })).toBeVisible()
})
