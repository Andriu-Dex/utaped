import { readFileSync, existsSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

const frontendDirectory = fileURLToPath(new URL('.', import.meta.url))

export default defineConfig(({ mode }) => {
  const envFile = fileURLToPath(new URL('../.env', import.meta.url))
  const legacyEnvFile = fileURLToPath(new URL('../.env.sample', import.meta.url))
  const targetFile = existsSync(envFile) ? envFile : existsSync(legacyEnvFile) ? legacyEnvFile : null
  const developmentPort = targetFile
    ? readFileSync(targetFile, 'utf8').match(/^BACKEND_PORT=(\d+)\s*$/m)?.[1]
    : undefined
  const frontendEnvironment = loadEnv(mode, frontendDirectory, 'API_TARGET')
  const apiTarget = frontendEnvironment.API_TARGET || `http://127.0.0.1:${developmentPort || '18080'}`
  return { plugins: [react()], server: { host: '127.0.0.1', proxy: { '/api': apiTarget } } }
})
