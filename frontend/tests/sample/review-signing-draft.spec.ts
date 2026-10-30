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

test('review observation CRUD and legacy stage labels', async ({ page }) => {
  const actor = await login(page, 'dev.reviewer'), reviewId = randomUUID(), taskId = randomUUID(), obsId = randomUUID()
  const title = 'Revisión con etiquetas normalizadas'
  let rowVersion = 0
  let observations: any[] = []

  await page.route('**/api/reviews/inbox?*', route => route.fulfill({
    json: {
      items: [{
        id: taskId,
        reviewId,
        documentId: randomUUID(),
        title,
        groupName: 'Comisión de prueba',
        periodName: 'Julio–Diciembre 2026',
        stageLabel: 'Revisión de ejemplo',
        action: 'REVIEW',
        requiresSignature: false,
        status: 'ACTIVE',
        roundNumber: 1
      }],
      total: 1,
      page: 0,
      size: 20,
      metrics: { active: 1, completed: 0, returned: 0 }
    }
  }))

  await page.route('**/api/reviews/' + reviewId, route => route.fulfill({
    json: {
      review: { id: reviewId, status: 'IN_REVIEW', round_number: 1 },
      tasks: [{
        id: taskId,
        actor_id: actor.id,
        actor_name: actor.displayName,
        stage_label: 'Revisión de ejemplo',
        stage_action: 'REVIEW',
        requires_signature: false,
        status: 'ACTIVE',
        row_version: rowVersion
      }],
      observations,
      pageCount: 1,
      pdfHash: 'b'.repeat(64),
      canAct: true
    }
  }))
  await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({
    contentType: 'image/png',
    body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64')
  }))

  let lastPostPayload: any = null
  await page.route('**/api/reviews/tasks/' + taskId + '/observations', route => {
    if (route.request().method() === 'POST') {
      lastPostPayload = route.request().postDataJSON()
      rowVersion++
      observations = [{
        id: obsId,
        review_id: reviewId,
        task_id: taskId,
        actor_id: actor.id,
        actor_name: actor.displayName,
        message: lastPostPayload.message,
        page_index: lastPostPayload.pageIndex,
        x: lastPostPayload.x,
        y: lastPostPayload.y,
        width: lastPostPayload.width,
        height: lastPostPayload.height,
        created_at: '2026-10-30T10:00:00Z',
        updated_at: null,
        can_edit: true,
        can_delete: true
      }]
      route.fulfill({ status: 200, json: { ok: true } })
    } else {
      route.continue()
    }
  })

  let lastPutPayload: any = null
  await page.route('**/api/reviews/tasks/' + taskId + '/observations/' + obsId + '*', route => {
    if (route.request().method() === 'PUT') {
      lastPutPayload = route.request().postDataJSON()
      rowVersion++
      observations[0].message = lastPutPayload.message
      observations[0].updated_at = '2026-10-30T10:15:00Z'
      route.fulfill({ status: 200, json: { ok: true } })
    } else if (route.request().method() === 'DELETE') {
      rowVersion++
      observations = []
      route.fulfill({ status: 200, json: { ok: true } })
    } else {
      route.continue()
    }
  })

  await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
  await expect(page.getByRole('cell', { name: 'Revisión documental', exact: false })).toBeVisible()
  await expect(page.getByText('Revisión de ejemplo')).toHaveCount(0)

  await page.getByRole('button', { name: 'Revisar ' + title + ' · Ronda 1', exact: true }).click()
  const stageItem = page.locator('.review-stages li').first()
  await expect(stageItem).toContainText('Revisión documental')
  await expect(stageItem).not.toContainText('Revisión de ejemplo')

  await openAction(page, 'Agregar observación')
  await page.getByPlaceholder('Describe la corrección necesaria…').fill('Observación de prueba para verificar edición')
  await page.getByRole('button', { name: 'Guardar observación', exact: true }).click()

  const obsCard = page.locator('.observation-card').first()
  await expect(obsCard).toBeVisible()
  await expect(obsCard).toContainText('Observación de prueba para verificar edición')
  await expect(obsCard.locator('.observation-badge-edited')).toHaveCount(0)

  await obsCard.getByRole('button', { name: 'Editar observación', exact: true }).click()
  const textarea = page.getByPlaceholder('Describe la corrección necesaria…')
  await expect(textarea).toHaveValue('Observación de prueba para verificar edición')
  await textarea.fill('Observación modificada con éxito')
  await page.getByRole('button', { name: 'Guardar cambios', exact: true }).click()

  await expect(obsCard).toContainText('Observación modificada con éxito')
  await expect(obsCard.locator('.observation-badge-edited')).toBeVisible()

  await withConfirmation(page, false, () => obsCard.getByRole('button', { name: 'Eliminar observación', exact: true }).click())
  await expect(obsCard).toBeVisible()
  await withConfirmation(page, true, () => obsCard.getByRole('button', { name: 'Eliminar observación', exact: true }).click())
  await expect(page.locator('.observation-card')).toHaveCount(0)
  await expect(page.getByText('Aún no hay observaciones registradas.')).toBeVisible()
})

test('review observation page association preservation, general conversion dirty state, and cancel protection', async ({ page }) => {
  const actor = await login(page, 'dev.reviewer'), reviewId = randomUUID(), taskId = randomUUID(), obsId = randomUUID()
  const title = 'Revisión preservación de página'
  let rowVersion = 0
  let observations: any[] = [{
    id: obsId,
    review_id: reviewId,
    task_id: taskId,
    actor_id: actor.id,
    actor_name: actor.displayName,
    message: 'Observación en página 1 sin resaltado',
    page_index: 0,
    x: null,
    y: null,
    width: null,
    height: null,
    created_at: '2026-10-30T10:00:00Z',
    updated_at: null,
    can_edit: true,
    can_delete: true
  }]

  await page.route('**/api/reviews/inbox?*', route => route.fulfill({
    json: {
      items: [{
        id: taskId,
        reviewId,
        documentId: randomUUID(),
        title,
        groupName: 'Comisión de prueba',
        periodName: 'Julio–Diciembre 2026',
        stageLabel: 'Revisión documental',
        action: 'REVIEW',
        requiresSignature: false,
        status: 'ACTIVE',
        roundNumber: 1
      }],
      total: 1,
      page: 0,
      size: 20,
      metrics: { active: 1, completed: 0, returned: 0 }
    }
  }))

  await page.route('**/api/reviews/' + reviewId, route => route.fulfill({
    json: {
      review: { id: reviewId, status: 'IN_REVIEW', round_number: 1 },
      tasks: [{
        id: taskId,
        actor_id: actor.id,
        actor_name: actor.displayName,
        stage_label: 'Revisión documental',
        stage_action: 'REVIEW',
        requires_signature: false,
        status: 'ACTIVE',
        row_version: rowVersion
      }],
      observations,
      pageCount: 3,
      pdfHash: 'c'.repeat(64),
      canAct: true
    }
  }))
  await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({
    contentType: 'image/png',
    body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64')
  }))

  let lastPutPayload: any = null
  await page.route('**/api/reviews/tasks/' + taskId + '/observations/' + obsId + '*', route => {
    if (route.request().method() === 'PUT') {
      lastPutPayload = route.request().postDataJSON()
      rowVersion++
      observations[0].message = lastPutPayload.message
      observations[0].page_index = lastPutPayload.pageIndex
      observations[0].updated_at = '2026-10-30T10:30:00Z'
      route.fulfill({ status: 200, json: { ok: true } })
    } else {
      route.continue()
    }
  })

  await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
  await page.getByRole('button', { name: 'Revisar ' + title + ' · Ronda 1', exact: true }).click()

  const obsCard = page.locator('.observation-card').first()
  await expect(obsCard).toBeVisible()

  await obsCard.getByRole('button', { name: 'Editar observación', exact: true }).click()
  await expect(page.getByText('Asociada a página 1 (sin zona seleccionada).')).toBeVisible()

  await page.getByRole('button', { name: 'Página siguiente', exact: true }).click()
  await expect(page.getByText('Asociada a página 1 (sin zona seleccionada).')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Asociar a página actual (2)', exact: true })).toBeVisible()

  const textarea = page.getByPlaceholder('Describe la corrección necesaria…')
  await textarea.fill('Observación modificada mientras se mira página 2')
  await page.getByRole('button', { name: 'Guardar cambios', exact: true }).click()

  expect(lastPutPayload.pageIndex).toBe(0)
  expect(lastPutPayload.message).toBe('Observación modificada mientras se mira página 2')

  await obsCard.getByRole('button', { name: 'Editar observación', exact: true }).click()
  await textarea.fill('Cambio que voy a cancelar')
  await withConfirmation(page, false, () => page.getByRole('button', { name: 'Cancelar edición', exact: true }).click())
  await expect(textarea).toHaveValue('Cambio que voy a cancelar')
  await withConfirmation(page, true, () => page.getByRole('button', { name: 'Cancelar edición', exact: true }).click())
  await expect(page.getByRole('button', { name: 'Guardar cambios' })).toHaveCount(0)

  await obsCard.getByRole('button', { name: 'Editar observación', exact: true }).click()
  const saveBtn = page.getByRole('button', { name: 'Guardar cambios', exact: true })
  await expect(saveBtn).toBeDisabled()
  await page.getByRole('button', { name: 'Convertir en observación general', exact: true }).click()
  await expect(saveBtn).toBeEnabled()
  await expect(page.getByText('Observación general (sin página ni zona).')).toBeVisible()
  await saveBtn.click()
  expect(lastPutPayload.pageIndex).toBeNull()


  // The container owns confirmation for both its Cancel button and Escape.
  for (const cancelWithEscape of [false, true]) {
    await obsCard.getByRole('button', { name: 'Editar observación', exact: true }).click()
    await textarea.fill('Edición pendiente protegida')
    const cancel = () => cancelWithEscape
      ? textarea.press('Escape')
      : page.getByRole('button', { name: 'Cancelar: Editar observación', exact: true }).click()
    await withConfirmation(page, false, cancel)
    await expect(textarea).toBeVisible()
    await expect(textarea).toHaveValue('Edición pendiente protegida')
    await withConfirmation(page, true, cancel)
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await expect(textarea).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Agregar observación', exact: true })).toBeVisible()
  }
})


test('review workspace is preserved across page reload and deep linking', async ({ page }) => {
  const actor = await login(page, 'dev.reviewer'), reviewId = randomUUID(), taskId = randomUUID()
  const title = 'Revisión persistente en recarga'

  await page.route('**/api/reviews/inbox?*', route => route.fulfill({
    json: {
      items: [{
        id: taskId,
        reviewId,
        documentId: randomUUID(),
        title,
        groupName: 'Comisión de auditoría',
        periodName: 'Julio–Diciembre 2026',
        stageLabel: 'Revisión',
        requiresSignature: false,
        status: 'ACTIVE',
        roundNumber: 1
      }],
      total: 1,
      page: 0,
      size: 20,
      metrics: { active: 1, completed: 0, returned: 0 }
    }
  }))

  await page.route('**/api/reviews/' + reviewId, route => route.fulfill({
    json: {
      review: { id: reviewId, title, status: 'IN_REVIEW', round_number: 1 },
      tasks: [{ id: taskId, actor_id: actor.id, actor_name: actor.displayName, stage_label: 'Revisión', requires_signature: false, status: 'ACTIVE', row_version: 0 }],
      observations: [],
      pageCount: 2,
      pdfHash: 'b'.repeat(64),
      canAct: true
    }
  }))

  await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({
    contentType: 'image/png',
    body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64')
  }))

  // 1. Navigate to Review view and open the document
  await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Bandeja de revisión' })).toBeVisible()
  await page.getByRole('button', { name: 'Revisar ' + title + ' · Ronda 1', exact: true }).click()

  // Verify workspace is opened and URL contains the doc param
  await expect(page.locator('.review-layout')).toBeVisible()
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
  expect(new URL(page.url()).searchParams.get('doc')).toBe(reviewId)
  expect(new URL(page.url()).searchParams.get('view')).toBe('reviews')

  // 2. Reload the page (F5) - MUST preserve the open review workspace
  await page.reload()
  await expect(page.locator('.review-layout')).toBeVisible()
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
  expect(new URL(page.url()).searchParams.get('doc')).toBe(reviewId)
  expect(new URL(page.url()).searchParams.get('view')).toBe('reviews')

  // 3. Click back button - URL resets cleanly to ?view=reviews and shows inbox
  await page.locator('.plan-back-btn').click()
  await expect(page.getByRole('heading', { name: 'Bandeja de revisión' })).toBeVisible()
  expect(new URL(page.url()).searchParams.get('doc')).toBeNull()
  expect(new URL(page.url()).searchParams.get('view')).toBe('reviews')
})
