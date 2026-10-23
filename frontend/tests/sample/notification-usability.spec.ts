import { test, expect } from '@playwright/test'
import { login } from './sample-fixtures'

test('notification popover supports keyboard, reading filters and unavailable targets on desktop and tablet', async ({ page }) => {
  const actor = await login(page, 'dev.principal')
  let read = false
  await page.route('**/api/notifications**', async route => {
    const url = new URL(route.request().url())
    if (url.pathname.endsWith('/unread-count')) return route.fulfill({ json: { unread: read ? 0 : 1 } })
    if (route.request().method() === 'POST') { read = true; return route.fulfill({ json: { type: 'ACCOUNT', id: actor.id } }) }
    const items = [{ id: 'fixture', kind: 'ACCOUNT_PASSWORD_CHANGED', targetType: 'ACCOUNT', targetId: actor.id, title: 'Seguridad de la cuenta', message: 'Revise su contraseña institucional.', createdAt: new Date().toISOString(), readAt: read ? new Date().toISOString() : null, targetAvailable: true }, { id: 'removed', kind: 'DOCUMENT_RETURNED', targetType: 'WORK_PLAN', targetId: actor.id, title: 'Documento no disponible', message: 'Documento de ejemplo retirado.', createdAt: new Date().toISOString(), readAt: new Date().toISOString(), targetAvailable: false }]
    const filter = url.searchParams.get('read')
    const filtered = items.filter(item => filter === null || (filter === 'true' ? !!item.readAt : !item.readAt))
    return route.fulfill({ json: { items: filtered, total: filtered.length, unread: read ? 0 : 1, page: 0, size: 20 } })
  })
  const trigger = page.getByRole('button', { name: /^Notificaciones/ })
  await trigger.focus(); await page.keyboard.press('Enter')
  const panel = page.getByRole('dialog', { name: 'Notificaciones', exact: true })
  await expect(panel).toBeVisible()
  await expect(panel.getByRole('button', { name: 'Abrir Documento no disponible', exact: true })).toBeDisabled()
  await panel.getByRole('button', { name: 'Sin leer', exact: true }).click()
  await expect(panel.getByRole('button', { name: 'Abrir Seguridad de la cuenta', exact: true })).toBeVisible()
  await expect(panel.getByText('Documento no disponible', { exact: true })).toHaveCount(0)
  await panel.getByRole('button', { name: 'Marcar como leída', exact: true }).focus(); await page.keyboard.press('Enter')
  await expect(panel.getByRole('heading', { name: 'Sin notificaciones', exact: true })).toBeVisible()
  await panel.getByRole('button', { name: 'Leídas', exact: true }).click()
  await expect(panel.getByRole('button', { name: 'Abrir Seguridad de la cuenta', exact: true })).toBeVisible()
  await panel.screenshot({ path: 'test-results/notifications-desktop.png', animations: 'disabled' })
  await page.keyboard.press('Escape'); await expect(panel).not.toBeVisible(); await expect(trigger).toBeFocused()
  await page.setViewportSize({ width: 820, height: 1180 })
  await page.getByRole('button', { name: 'Modo oscuro', exact: true }).click()
  await trigger.click(); await expect(panel).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await panel.screenshot({ path: 'test-results/notifications-tablet-dark.png', animations: 'disabled' })
  await panel.getByRole('button', { name: 'Abrir Seguridad de la cuenta', exact: true }).focus(); await page.keyboard.press('Enter')
  await expect(panel).not.toBeVisible()
})
