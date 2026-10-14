import { openAction } from '../open-action'
import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { api, login } from './sample-fixtures'

test('document type chooser proposes the institutional name and excludes draft Plans from Reports', async ({ browser }) => {
  const adminContext = await browser.newContext({ baseURL: 'http://127.0.0.1:64431' })
  const principalContext = await browser.newContext({ baseURL: 'http://127.0.0.1:64431', viewport: { width: 1440, height: 1000 } })
  const admin = await adminContext.newPage(), principal = await principalContext.newPage()
  try {
    await login(admin, 'dev.admin'); const actor = await login(principal, 'dev.principal')
    const suffix = randomUUID().slice(0, 8), groupName = 'Unidad creación ' + suffix
    const date = (offset: number) => new Date(Date.now() + offset * 86400000).toISOString().slice(0, 10)
    const group = await api(admin, '/admin/groups', 'POST', { name: groupName, groupType: 'COMMISSION' })
    const period = await api(admin, '/admin/periods', 'POST', { name: 'Período creación ' + suffix, startsOn: date(-1), endsOn: date(30), preparationStartsOn: date(-1), preparationEndsOn: date(10), reviewStartsOn: date(-1), reviewEndsOn: date(30) })
    const scope = await api(admin, '/admin/organization/scopes', 'POST', { groupId: group.id, periodId: period.id, careerId: null })
    await api(admin, '/admin/organization/scopes/' + scope.id + '/members', 'PUT', { userId: actor.id, membershipRole: 'MEMBER', positionLabel: 'Responsable de ejemplo', active: true, rowVersion: 0 })
    await api(admin, '/admin/organization/scopes/' + scope.id + '/principal', 'PUT', { userId: actor.id, rowVersion: 0 })
    await principal.reload(); await principal.getByRole('button', { name: 'Documentos', exact: true }).click()
    await expect(principal.getByRole('radio', { name: /^Plan de Trabajo/ })).toHaveCount(0)
    await principal.getByLabel('Buscar por título').fill(suffix)
    await principal.getByRole('button', { name: 'Buscar', exact: true }).click()
    await openAction(principal, 'Crear documento')
    const planChoice = principal.getByRole('radio', { name: /^Plan de Trabajo/ }), reportChoice = principal.getByRole('radio', { name: /^Informe/ })
    await expect(planChoice).toBeChecked()
    await expect(principal.getByLabel('Título del nuevo Plan')).toBeDisabled()
    await openAction(principal, 'Crear documento');await principal.getByLabel('Ámbito del nuevo Plan').selectOption(scope.id)
    await expect(principal.getByLabel('Título del nuevo Plan')).toHaveValue('Plan de Trabajo: ' + groupName)
    await expect(principal.getByLabel('Título del nuevo Plan')).toBeEnabled()
    await expect(principal.getByLabel('Ámbito del nuevo Plan')).toHaveValue(scope.id)
    principal.once('dialog', dialog => dialog.dismiss()); await reportChoice.click(); await expect(planChoice).toBeChecked()
    await principal.screenshot({ path: 'test-results/document-creation-plan-desktop.png', fullPage: true })
    await principal.getByRole('button', { name: 'Crear borrador', exact: true }).click()
    await expect(principal.getByRole('heading', { name: 'Plan de Trabajo: ' + groupName, exact: true })).toBeVisible()
    const created = (await api(principal, '/work-plans?query=' + encodeURIComponent(groupName))).items[0]
    await principal.getByRole('button', { name: 'Volver a documentos', exact: true }).click()
    await openAction(principal, 'Crear documento'); await reportChoice.check(); await principal.getByLabel('Buscar Plan aprobado').fill(groupName); await principal.getByRole('button', { name: 'Buscar Plan', exact: true }).click()
    await expect(principal.getByText('No hay Planes aprobados que coincidan con la búsqueda.', { exact: true })).toBeVisible()
    expect((await api(principal, '/reports/options?query=' + encodeURIComponent(groupName))).total).toBe(0)
    await principal.keyboard.press('Escape')
    await principal.setViewportSize({ width: 820, height: 1180 }); await principal.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
    await openAction(principal, 'Crear documento'); await reportChoice.check(); await principal.getByLabel('Buscar Plan aprobado').fill(groupName); await principal.getByRole('button', { name: 'Buscar Plan', exact: true }).click()
    await expect(principal.getByText('No hay Planes aprobados que coincidan con la búsqueda.', { exact: true })).toBeVisible()
    await principal.screenshot({ path: 'test-results/document-creation-report-tablet-dark.png', fullPage: true })
    expect(await principal.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    expect((await api(principal, '/work-plans/' + created.id)).documentState).toBe('DRAFT')
  } finally { await adminContext.close(); await principalContext.close() }
})
