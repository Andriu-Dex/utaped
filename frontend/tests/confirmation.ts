import { expect, type Page } from '@playwright/test'

export async function withConfirmation(page: Page, accepted: boolean, trigger: () => Promise<unknown>) {
  await trigger()
  const dialog = page.getByRole('dialog', { name: 'Confirmar acción', exact: true })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: accepted ? 'Confirmar' : 'Cancelar', exact: true }).click()
  await expect(dialog).not.toBeVisible()
}
