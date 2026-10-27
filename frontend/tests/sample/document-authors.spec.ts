import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { api, login, acceptanceBaseURL } from './sample-fixtures'
import { openAction } from '../open-action'
import { withConfirmation } from '../confirmation'

test('principal configures coauthors while members retain read-only access', async ({ page, browser }) => {
  await login(page, 'dev.principal')
  const context = await browser.newContext({ baseURL: acceptanceBaseURL })
  try {
    const member = await context.newPage(), identity = await login(member, 'dev.member')
    const options = await api(page, '/work-plans/options'), scope = options.scopes.find((item: { canCreate: boolean; periodId: string }) => item.canCreate && item.periodId === options.periods.find((period: { name: string }) => period.name === 'Julio–Diciembre 2026').id)
    const plan = await api(page, '/work-plans', 'POST', { scopeId: scope.id, groupId: scope.groupId, periodId: scope.periodId, title: 'Plan de seguimiento académico ' + randomUUID().slice(0, 8), requestKey: randomUUID() })
    await page.getByRole('button', { name: 'Documentos', exact: true }).click()
    await page.getByRole('button', { name: 'Editar ' + plan.title, exact: false }).click()
    const panel = page.getByRole('region', { name: 'Elaboradores del documento', exact: true })
    await expect(panel.getByRole('checkbox')).toHaveCount(0)
    await openAction(panel, 'Configurar elaboradores')
    await panel.getByRole('checkbox', { name: identity.displayName, exact: true }).check()
    await expect(page.getByRole('button', { name: 'Guardar borrador', exact: true })).toBeDisabled()
    await expect(page.getByLabel('Título del Plan', { exact: true })).toBeDisabled()
    await panel.getByRole('button', { name: 'Guardar elaboradores', exact: true }).click()
    await expect(page.getByText('Elaboradores guardados. Cada persona firmará con su propia identidad.', { exact: true })).toBeVisible()
    const authors = await api(page, '/work-plans/' + plan.id + '/authors')
    expect(authors.authors).toHaveLength(2)
    expect(authors.authors[1].id).toBe(identity.id)
    expect(authors.authors[1].position).toBe('Docente integrante')
    expect((await api(member, '/work-plans/' + plan.id)).editable).toBe(false)
    const csrf = await api(member, '/auth/csrf')
    expect((await member.request.put('/api/work-plans/' + plan.id + '/authors', { headers: { [csrf.headerName]: csrf.token }, data: { rowVersion: authors.rowVersion, coauthorIds: [] } })).status()).toBe(403)
    await panel.screenshot({ path: 'test-results/document-authors-desktop.png', animations: 'disabled' })
    await member.getByRole('button', { name: 'Documentos', exact: true }).click()
    await member.getByRole('button', { name: 'Ver ' + plan.title, exact: false }).click()
    await expect(member.getByRole('heading', { name: 'Documento aún no preparado', exact: true })).toBeVisible()
    await expect(member.getByRole('navigation', { name: 'Pasos del Plan' })).toHaveCount(0)
    await expect(member.getByRole('button', { name: 'Guardar borrador', exact: true })).toHaveCount(0)
    const artifact = { id: randomUUID(), pageCount: 1, current: true, createdAt: new Date().toISOString(), pages: [{ number: 1, width: 595, height: 842, kind: 'T1' }], signatureSlots: [{ label: 'Elaborado por', pageNumber: 1 }] }
    let turn = false
    await member.route('**/api/work-plans/' + plan.id + '/artifacts', route => route.fulfill({ json: [artifact] }))
    await member.route('**/api/work-plans/' + plan.id + '/artifacts/' + artifact.id + '/signing', route => route.fulfill({ json: {
      signed: [], actorSigned: false, automaticSubmission: true, documentState: 'DRAFT', signingEnabled: turn,
      signingBlockers: turn ? [] : [{ section: 'Turno', message: 'Solo el siguiente elaborador pendiente puede firmar.' }],
      pendingAuthors: [{ id: identity.id, name: identity.displayName, position: 'Docente integrante' }],
      preparation: { rowVersion: authors.rowVersion, pdfHash: '0'.repeat(64) },
    } }))
    await member.route('**/api/work-plans/' + plan.id + '/artifacts/' + artifact.id + '/pages/*', route => route.fulfill({ contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64') }))
    await member.getByRole('button', { name: 'Actualizar consulta', exact: true }).click()
    await expect(member.getByAltText('Página 1 del Plan T1')).toBeVisible()
    await member.getByRole('checkbox', { name: /He revisado el documento completo/ }).check()
    await expect(member.getByRole('button', { name: 'Preparar firma', exact: true })).toBeDisabled()
    turn = true
    await member.getByRole('button', { name: 'Actualizar consulta', exact: true }).click()
    await member.getByRole('checkbox', { name: /He revisado el documento completo/ }).check()
    await openAction(member, 'Preparar firma')
    await member.getByLabel('Contraseña del certificado', { exact: true }).fill('UI-only-pending-credential')
    await withConfirmation(member, false, () => member.getByRole('button', { name: 'Documentos', exact: true }).click())
    await expect(member.getByLabel('Contraseña del certificado', { exact: true })).toHaveValue('UI-only-pending-credential')
    await withConfirmation(member, true, () => member.getByRole('button', { name: 'Cancelar: Preparar firma', exact: true }).click())
    await member.getByRole('button', { name: 'Volver a Documentos', exact: true }).click()
    await expect(member.getByRole('heading', { name: 'Mis documentos', exact: true })).toBeVisible()
  } finally { await context.close() }
})
