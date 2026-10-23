import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { api, login, acceptanceBaseURL } from './sample-fixtures'
import { openAction } from '../open-action'

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
  } finally { await context.close() }
})
