import type { Locator, Page } from '@playwright/test'

export async function openAction(target: Page | Locator, label: string) {
  if (!(await target.getByRole('region', { name: 'Formulario: ' + label, exact: true }).isVisible())) {
    await target.getByRole('button', { name: label, exact: true }).click()
  }
}
