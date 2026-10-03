import { useEffect, useState, type FormEvent } from 'react'
import { api, refreshCsrf, type Account } from '../shared/api'

export function Access({ onLogin }: { onLogin: (account: Account) => void }) {
  const [resetToken, setResetToken] = useState(() => new URLSearchParams(window.location.hash.slice(1)).get('reset'))
  const [mode, setMode] = useState<'login' | 'forgot' | 'reset'>(resetToken ? 'reset' : 'login')
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  useEffect(() => {
    const changed = () => {
      const token = new URLSearchParams(window.location.hash.slice(1)).get('reset')
      setResetToken(token); setMode(token ? 'reset' : 'login'); setError(''); setNotice('')
    }
    window.addEventListener('hashchange', changed)
    return () => window.removeEventListener('hashchange', changed)
  }, [])
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setNotice(''); setBusy(true)
    const data = new FormData(event.currentTarget)
    try {
      if (mode === 'login') {
        await api('/auth/login', 'POST', new URLSearchParams({ username: String(data.get('email')), password: String(data.get('password')) }))
        await refreshCsrf(); onLogin(await api<Account>('/auth/me'))
      } else if (mode === 'forgot') {
        const result = await api<{ message: string }>('/auth/forgot-password', 'POST', { email: data.get('email') }); setNotice(result.message)
      } else {
        await api('/auth/reset-password', 'POST', { token: resetToken, newPassword: data.get('password') })
        window.history.replaceState(null, '', window.location.pathname); setMode('login'); setNotice('Contraseña actualizada. Inicie sesión.'); await refreshCsrf()
      }
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo conectar.') }
    finally { setBusy(false) }
  }
  return <main className="access"><section className="card access-card">
    <p className="brand">UTAPED</p><p>Gestión Documental Académica · FISEI / UTA</p>
    <h1>{mode === 'login' ? 'Iniciar sesión' : mode === 'forgot' ? 'Recuperar acceso' : 'Nueva contraseña'}</h1>
    <form onSubmit={submit}>
      {mode !== 'reset' && <label>Correo institucional<input required type="email" name="email" maxLength={254} autoComplete="username" /></label>}
      {mode !== 'forgot' && <label>{mode === 'reset' ? 'Nueva contraseña' : 'Contraseña'}<input required type="password" name="password" minLength={mode === 'reset' ? 12 : undefined} maxLength={72} autoComplete={mode === 'reset' ? 'new-password' : 'current-password'} /></label>}
      {mode === 'reset' && <p>Use al menos 12 caracteres; máximo 72 bytes UTF-8.</p>}
      {error && <p role="alert" className="error">{error}</p>}{notice && <p role="status" className="notice">{notice}</p>}
      <button disabled={busy}>{busy ? 'Procesando…' : mode === 'login' ? 'Ingresar' : mode === 'forgot' ? 'Enviar enlace' : 'Guardar contraseña'}</button>
    </form>
    <button className="link" onClick={() => { setMode(mode === 'login' ? 'forgot' : 'login'); setError(''); setNotice('') }}>{mode === 'login' ? '¿Olvidó su contraseña?' : 'Volver al inicio de sesión'}</button>
  </section></main>
}

export function ChangePassword({ temporary, onComplete }: { temporary: boolean; onComplete: () => void }) {
  const [error, setError] = useState(''); const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true)
    const data = new FormData(event.currentTarget)
    if (data.get('newPassword') !== data.get('confirmation')) { setError('Las contraseñas no coinciden.'); setBusy(false); return }
    try { await api('/auth/change-password', 'POST', { currentPassword: data.get('currentPassword'), newPassword: data.get('newPassword') }); await refreshCsrf(); onComplete() }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.') } finally { setBusy(false) }
  }
  return <section className="card"><h2>{temporary ? 'Cambie su contraseña temporal' : 'Cambiar contraseña'}</h2><p>Use al menos 12 caracteres. Al guardar se cerrarán sus sesiones.</p>
    <form onSubmit={submit}><label>Contraseña actual<input required name="currentPassword" type="password" autoComplete="current-password" maxLength={128} /></label>
      <label>Nueva contraseña<input required name="newPassword" type="password" autoComplete="new-password" minLength={12} maxLength={72} /></label>
      <label>Confirmar contraseña<input required name="confirmation" type="password" autoComplete="new-password" minLength={12} maxLength={72} /></label>
      {error && <p className="error" role="alert">{error}</p>}<button disabled={busy}>{busy ? 'Guardando…' : 'Guardar contraseña'}</button></form>
  </section>
}
