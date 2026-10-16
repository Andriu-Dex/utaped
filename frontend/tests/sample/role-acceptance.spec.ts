import { acceptanceBaseURL } from './acceptance-environment'
import { openAction } from '../open-action'
import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { api, login } from './sample-fixtures'

test('seven roles preserve scope permissions, immutable access and revocation in active sessions', async ({ browser }) => {
  const roles = ['admin', 'principal', 'coordinator', 'member', 'reviewer', 'approver', 'outsider']
  const contexts = await Promise.all(roles.map(() => browser.newContext({ baseURL: acceptanceBaseURL, viewport: { width: 1440, height: 1000 } })))
  const pages = await Promise.all(contexts.map(context => context.newPage()))
  const [admin, principal, coordinator, member, reviewer, approver, outsider] = pages
  try {
    const actors = []
    for (let i = 0; i < roles.length; i++) actors.push(await login(pages[i], 'dev.' + roles[i]))
    const suffix = randomUUID().slice(0, 8), title = 'Aceptación privada ' + suffix
    const date = (offset: number) => new Date(Date.now() + offset * 86400000).toISOString().slice(0, 10)
    const group = await api(admin, '/admin/groups', 'POST', { name: 'Comisión aceptación ' + suffix, groupType: 'COMMISSION' })
    const period = await api(admin, '/admin/periods', 'POST', { name: 'Período aceptación ' + suffix, startsOn: date(-1), endsOn: date(30), preparationStartsOn: date(-1), preparationEndsOn: date(10), reviewStartsOn: date(-1), reviewEndsOn: date(30) })
    const scope = await api(admin, '/admin/organization/scopes', 'POST', { groupId: group.id, periodId: period.id, careerId: null })
    for (const index of [1, 2, 3]) await api(admin, '/admin/organization/scopes/' + scope.id + '/members', 'PUT', { userId: actors[index].id, membershipRole: index === 2 ? 'COORDINATOR' : 'MEMBER', positionLabel: '', active: true, rowVersion: 0 })
    await api(admin, '/admin/organization/scopes/' + scope.id + '/principal', 'PUT', { userId: actors[1].id, rowVersion: 0 })
    await principal.reload(); await principal.getByRole('button', { name: 'Documentos', exact: true }).click(); await openAction(principal, 'Crear documento');await principal.getByLabel('Ámbito del nuevo Plan').selectOption(scope.id); await principal.getByLabel('Título del nuevo Plan').fill(title); await principal.getByRole('button', { name: 'Crear borrador', exact: true }).click()
    await expect(principal.getByRole('heading', { name: title, exact: true })).toBeVisible()
    const doc = (await api(principal, '/work-plans?query=' + encodeURIComponent(title))).items[0]
    await expect(admin.getByRole('button', { name: 'Usuarios', exact: true })).toBeVisible(); await expect(principal.getByRole('button', { name: 'Usuarios', exact: true })).toHaveCount(0)
    for (const page of [coordinator, member]) {
      await page.reload(); await page.getByRole('button', { name: 'Documentos', exact: true }).click(); await page.getByRole('button', { name: 'Ver ' + title, exact: false }).click()
      await expect(page.getByLabel('Título del Plan')).toBeDisabled(); await expect(page.getByRole('button', { name: 'Guardar borrador', exact: true })).toBeDisabled()
      const csrf = await api(page, '/auth/csrf'); const response = await page.request.put('/api/work-plans/' + doc.id, { headers: { [csrf.headerName]: csrf.token }, data: { rowVersion: 0, title: 'Intrusión', institutionalUnit: 'FISEI', career: '', justification: '', objective: '' } }); expect(response.status()).toBe(403)
    }
    for (const page of [admin, reviewer, approver, outsider]) {
      expect((await page.request.get('/api/work-plans/' + doc.id)).status()).toBe(404)
      expect((await page.request.get('/api/work-plans/' + doc.id + '/artifacts')).status()).toBe(404)
    }
    for (const page of [reviewer, approver]) expect((await api(page, '/reviews/inbox?query=' + encodeURIComponent(title))).total).toBe(0)
    expect((await outsider.request.get('/api/admin/users')).status()).toBe(403)
    expect((await api(coordinator, '/organization/scopes/' + scope.id + '/accounts')).length).toBe(0)
    await member.screenshot({ path: 'test-results/role-member-readonly-desktop.png', fullPage: true })
    await api(admin, '/admin/organization/scopes/' + scope.id + '/members', 'PUT', { userId: actors[3].id, membershipRole: 'MEMBER', positionLabel: '', active: false, rowVersion: 1 })
    expect((await member.request.get('/api/work-plans/' + doc.id)).status()).toBe(404); expect((await member.request.get('/api/work-plans/' + doc.id + '/history')).status()).toBe(404)
    expect((await api(principal, '/work-plans/' + doc.id)).title).toBe(title); expect((await api(principal, '/work-plans/' + doc.id)).rowVersion).toBe(0)
  } finally { for (const context of contexts) await context.close() }
})
