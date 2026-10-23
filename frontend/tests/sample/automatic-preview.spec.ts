import { randomUUID } from 'node:crypto'
import { test, expect } from '@playwright/test'
import { api, login } from './sample-fixtures'

test('T1 preview retries failures, reuses current artifacts and never generates read-only documents', async ({ page }) => {
  await login(page, 'dev.principal')
  const options = await api(page, '/work-plans/options')
  const scope = options.scopes.find((s: { canCreate: boolean }) => s.canCreate)
  const plan = await api(page, '/work-plans', 'POST', { scopeId: scope.id, groupId: scope.groupId, periodId: scope.periodId, requestKey: randomUUID(), title: 'Previsualización automática ' + randomUUID() })
  let posts = 0, stored = false, readOnly = false, failLoading = false
  const artifact = { id: randomUUID(), pageCount: 1, sourceRowVersion: plan.rowVersion, current: true, createdAt: new Date().toISOString(), pages: [{ number: 1, kind: 'CONTENT', width: 595, height: 842 }], signatureSlots: [] }
  const path = '**/api/work-plans/' + plan.id
  await page.route(path, route => route.fulfill({ json: { ...plan, editable: !readOnly, documentState: readOnly ? 'APPROVED' : 'DRAFT' } }))
  await page.route(path + '/artifacts/readiness', route => failLoading ? route.fulfill({ status: 500, json: { message: 'Error de carga de ejemplo' } }) : route.fulfill({ json: { rowVersion: plan.rowVersion, ready: true, blockers: [] } }))
  await page.route(path + '/artifacts', route => {
    if (route.request().method() === 'POST') {
      posts++
      if (posts === 1) return route.fulfill({ status: 503, json: { message: 'Generación temporalmente no disponible' } })
      stored = true
      return route.fulfill({ json: artifact })
    }
    return route.fulfill({ json: stored ? [artifact] : [] })
  })
  await page.route(path + '/artifacts/*/pages/*', route => route.fulfill({ contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64') }))
  await page.getByRole('button', { name: 'Documentos', exact: true }).click()
  await page.getByLabel('Buscar por título').fill(plan.title)
  await page.getByRole('button', { name: 'Buscar', exact: true }).click()
  await page.getByRole('button', { name: 'Editar ' + plan.title, exact: false }).click()
  const open = async () => page.getByRole('button', { name: /5\s*Preparación/ }).click()
  await open()
  await expect(page.getByText('Generación temporalmente no disponible', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: /Siguiente: Firma/ })).toBeDisabled()
  await page.getByRole('button', { name: 'Reintentar previsualización', exact: true }).click()
  await expect(page.getByAltText('Página 1 del Plan T1')).toBeVisible()
  expect(posts).toBe(2)
  await page.getByRole('button', { name: 'Volver al Plan', exact: true }).click()
  await open()
  await expect(page.getByAltText('Página 1 del Plan T1')).toBeVisible()
  expect(posts).toBe(2)
  await page.getByRole('button', { name: 'Volver al Plan', exact: true }).click()
  readOnly = true
  artifact.current = false
  await open()
  await expect(page.getByAltText('Página 1 del Plan T1')).toBeVisible()
  expect(posts).toBe(2)
  await page.getByRole('button', { name: 'Volver al Plan', exact: true }).click()
  failLoading = true
  await open()
  await expect(page.getByText('Error de carga de ejemplo', { exact: true })).toBeVisible()
  failLoading = false
  await page.getByRole('button', { name: 'Reintentar previsualización', exact: true }).click()
  await expect(page.getByAltText('Página 1 del Plan T1')).toBeVisible()
  expect(posts).toBe(2)
})
