import { acceptanceBaseURL } from './tests/sample/acceptance-environment'
import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './tests/sample',
  outputDir: './test-results/sample',
  workers: 1,
  timeout: 240000,
  use: {
    baseURL: acceptanceBaseURL,
    viewport: { width: 1440, height: 1000 },
    actionTimeout: 15000,
    navigationTimeout: 15000,
    trace: 'off',
    screenshot: 'only-on-failure',
  },
})
