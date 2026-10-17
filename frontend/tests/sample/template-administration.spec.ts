import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { api, login, acceptanceBaseURL } from './sample-fixtures'
import { withConfirmation } from '../confirmation'

test('templates are admin-only, cancellation preserves edits and defaults affect only newly created Plans', async ({ page, browser }) => {
  await login(page, 'dev.admin')
  const principalContext = await browser.newContext({ baseURL: acceptanceBaseURL })
  const principal = await principalContext.newPage()
  await login(principal, 'dev.principal')
  const before = await api(page, '/admin/document-templates/T1')
  try {
    await page.getByRole('button', { name: 'Plantillas', exact: true }).click()
    await expect(page.getByLabel('Texto predeterminado de la Justificación:')).toHaveCount(0)
    await page.getByRole('button', { name: 'Contraer menú lateral', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Cerrar sesión', exact: true })).toBeVisible()
    await page.getByRole('button', { name: 'Expandir menú lateral', exact: true }).first().click()
    await page.getByRole('button', { name: 'Editar plantilla', exact: true }).click()
    const input = page.getByLabel('Texto predeterminado de la Justificación:')
    const text = 'Justificación de aceptación ' + randomUUID()
    await input.fill(text)
    await page.screenshot({ path: 'test-results/template-administration-desktop.png', animations: 'disabled' })
    await withConfirmation(page, false, async () => { await page.getByRole('button', { name: 'Cancelar', exact: true }).click() })
    await expect(input).toHaveValue(text)
    await page.getByRole('button', { name: 'Guardar cambios', exact: true }).click()
    await expect(page.getByText('Plantilla institucional actualizada correctamente.', { exact: true })).toBeVisible()
    const options = await api(principal, '/work-plans/options')
    expect(options.defaultJustification).toBe(text)
    const scope = options.scopes.find((s: { canCreate: boolean }) => s.canCreate)
    const plan = await api(principal, '/work-plans', 'POST', { scopeId: scope.id, groupId: scope.groupId, periodId: scope.periodId, requestKey: randomUUID() })
    expect(plan.justification).toBe(text)
    expect((await principal.request.get('/api/admin/document-templates')).status()).toBe(403)
    const current = await api(page, '/admin/document-templates/T1')
    await api(page, '/admin/document-templates/T1', 'PUT', { rowVersion: current.rowVersion, justification: before.justification })
    expect((await api(principal, '/work-plans/' + plan.id)).justification).toBe(text)
  } finally {
    const current = await api(page, '/admin/document-templates/T1')
    if (current.justification !== before.justification) await api(page, '/admin/document-templates/T1', 'PUT', { rowVersion: current.rowVersion, justification: before.justification })
    await principalContext.close()
  }
})
