export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) { super(message); this.status = status }
}
let csrf: { headerName: string; token: string } | undefined
export async function refreshCsrf() {
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
  if (!response.ok) throw new ApiError('No se pudo conectar con el servidor.', response.status)
  csrf = await response.json()
}
export async function api<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  if (method !== 'GET') {
    if (!csrf) await refreshCsrf()
    headers[csrf!.headerName] = csrf!.token
  }
  const form = body instanceof URLSearchParams
  if (body) headers['Content-Type'] = form ? 'application/x-www-form-urlencoded' : 'application/json'
  const response = await fetch('/api' + path, {
    method, credentials: 'same-origin', headers,
    body: body ? form ? body.toString() : JSON.stringify(body) : undefined,
  })
  if (!response.ok) {
    if (response.status === 401 && !['/auth/me', '/auth/login'].includes(path)) window.dispatchEvent(new Event('session-expired'))
    const data = await response.json().catch(() => ({ message: 'No se pudo completar la solicitud.' }))
    throw new ApiError(data.message, response.status)
  }
  const text = await response.text()
  return text ? JSON.parse(text) : undefined as T
}
export interface Account { id: string; email: string; displayName: string; systemRole: 'ADMIN' | 'USER'; mustChangePassword: boolean }
export interface Group { id: string; name: string; group_type: string; membership_role: string | null }
export interface Member { id: string; display_name: string; membership_role: string }
export interface Period { id: string; name: string; starts_on: string; ends_on: string }
export interface User { id: string; email: string; display_name: string; system_role: string; active: boolean }
