import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

const frontendDirectory = fileURLToPath(new URL('.', import.meta.url))
const projectDirectory = fileURLToPath(new URL('..', import.meta.url))

export default defineConfig(({ mode }) => {
  const projectEnvironment = loadEnv(mode, projectDirectory, 'BACKEND_PORT')
  const frontendEnvironment = loadEnv(mode, frontendDirectory, 'API_TARGET')
  const apiTarget = frontendEnvironment.API_TARGET || `http://127.0.0.1:${projectEnvironment.BACKEND_PORT || '8080'}`
  return { plugins: [react()], server: { host: '127.0.0.1', proxy: { '/api': apiTarget } } }
})
