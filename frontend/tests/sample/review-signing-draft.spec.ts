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
