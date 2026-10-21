import { resolve, basename } from 'node:path'

function configuration() {
  const project = process.env.SAMPLE_COMPOSE_PROJECT
  const environmentFile = process.env.SAMPLE_ENV_FILE
  const baseURL = process.env.SAMPLE_BASE_URL
  if (project !== 'utaped-acceptance' || !environmentFile || !baseURL) {
    throw new Error('Run scripts/test-acceptance.ps1; manual development environments are forbidden for this suite')
  }
  const address = new URL(baseURL)
  if (address.href !== 'http://127.0.0.1:24431/' || basename(environmentFile) !== '.env.acceptance'
      || resolve(environmentFile) !== resolve('..', '.env.acceptance')) {
    throw new Error('Acceptance tests require the isolated loopback port 24431 and .env.acceptance')
  }
  return { project, environmentFile, baseURL }
}

// Validate before requests, credential reads or SQL; no fallback to the manual environment.
const settings = configuration()
export const acceptanceProject = settings.project
export const acceptanceEnvironmentFile = settings.environmentFile
export const acceptanceBaseURL = settings.baseURL
