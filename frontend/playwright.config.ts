import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  workers: 1,
  use: { baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:15173', trace: 'off' },
  webServer: process.env.E2E_BASE_URL ? undefined : {
    command: 'npm run dev -- --port 15173 --strictPort',
    url: 'http://127.0.0.1:15173',
    env: { API_TARGET: 'http://127.0.0.1:18080' },
  },
})
