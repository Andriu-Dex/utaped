import { type Page } from '@playwright/test'

export async function returnToPlan(page: Page) {
  const steps = page.getByRole('navigation', { name: 'Pasos del Plan', exact: true })
  if (await steps.isVisible()) await steps.getByRole('button', { name: /^1\s*Información general/ }).click()
  else await page.getByRole('button', { name: /^Volver al (Plan|Informe)$/, exact: true }).click()
}
