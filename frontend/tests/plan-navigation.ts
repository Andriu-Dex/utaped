import { type Page } from '@playwright/test'

export async function openPlanFollowUp(page: Page, section: string) {
  await page.getByRole('button', { name: 'Seguimiento del documento', exact: true }).click()
  await page.getByRole('button', { name: section, exact: true }).click()
}

export async function returnToPlan(page: Page) {
  const steps = page.getByRole('navigation', { name: 'Pasos del Plan', exact: true })
  if (await steps.isVisible()) await steps.getByRole('button', { name: /^1\s*Información general/ }).click()
  else await page.getByRole('button', { name: /^Volver al (Plan|Informe)$/, exact: true }).click()
}
