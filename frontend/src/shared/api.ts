export class ApiError extends Error {
  status: number
  constructor(message: string, status: number) { super(message); this.status = status }
}
export async function download(path: string, filename: string) {
  const response = await fetch('/api' + path, { credentials: 'same-origin' })
  if (!response.ok) { if (response.status === 401) window.dispatchEvent(new Event('session-expired')); const error = await response.json().catch(() => ({ message: 'No se pudo descargar el archivo.' })); throw new ApiError(error.message, response.status) }
  const url = URL.createObjectURL(await response.blob()); const link = document.createElement('a'); link.href = url; link.download = filename; document.body.append(link); link.click(); link.remove(); window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
let csrf: { headerName: string; token: string } | undefined
export async function signPdf<T>(path: string, file: File, password: string): Promise<T> {
  const encoded = new TextEncoder().encode(password)
  let container: Uint8Array<ArrayBuffer> | undefined; let payload: Uint8Array<ArrayBuffer> | undefined
  try {
    if (!encoded.length || encoded.length > 512 || file.size > 1048576 || !file.size) throw new ApiError('Revise la contraseña y el archivo (máximo 1 MB).', 400)
    container = new Uint8Array(await file.arrayBuffer()); payload = new Uint8Array(4 + encoded.length + container.length)
    new DataView(payload.buffer).setUint32(0, encoded.length); payload.set(encoded, 4); payload.set(container, 4 + encoded.length)
    if (!csrf) await refreshCsrf()
    const response = await fetch('/api' + path, { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/octet-stream', [csrf!.headerName]: csrf!.token }, body: payload.buffer })
    if (!response.ok) { if (response.status === 401) window.dispatchEvent(new Event('session-expired')); const error = await response.json().catch(() => ({ message: 'No se pudo firmar el PDF.' })); throw new ApiError(error.message, response.status) }
    return await response.json()
  } finally { encoded.fill(0); container?.fill(0); payload?.fill(0) }
}
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
  if (body && !(body instanceof FormData)) headers['Content-Type'] = form ? 'application/x-www-form-urlencoded' : 'application/json'
  const response = await fetch('/api' + path, {
    method, credentials: 'same-origin', headers,
    body: body instanceof FormData ? body : body ? form ? body.toString() : JSON.stringify(body) : undefined,
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
export interface Group { id: string; name: string; group_type: string; membership_role: string | null; collective_label: string; active: boolean; row_version: number }
export interface Member { id: string; display_name: string; membership_role: string }
export interface Period { id: string; name: string; starts_on: string; ends_on: string; restrict_holiday_endpoints: boolean }
export interface User { id: string; email: string; display_name: string; system_role: string; active: boolean; row_version: number }
