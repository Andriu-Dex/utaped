import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, refreshCsrf, type Account } from '../shared/api'
import { Brand, Icon } from '../shared/Icon'

export function Access({ onLogin }: { onLogin: (account: Account) => void }) {
  const [resetToken, setResetToken] = useState(() => new URLSearchParams(window.location.hash.slice(1)).get('reset'))
  const [mode, setMode] = useState<'login' | 'forgot' | 'reset'>(resetToken ? 'reset' : 'login')
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const [challenge, setChallenge] = useState(() => crypto.randomUUID()); const [captchaReady, setCaptchaReady] = useState(false); const [captchaError, setCaptchaError] = useState(false)
  const [visible, setVisible] = useState(false)
  const answer = useRef<HTMLInputElement>(null)
  function renew() { setCaptchaReady(false); setCaptchaError(false); setChallenge(crypto.randomUUID()); if (answer.current) answer.current.value = '' }
  function switchMode(next: typeof mode) { setMode(next); setError(''); setNotice(''); setVisible(false); if (next === 'login') renew() }
  useEffect(() => {
    const changed = () => { const token = new URLSearchParams(window.location.hash.slice(1)).get('reset'); setResetToken(token); setMode(token ? 'reset' : 'login'); setError(''); setNotice(''); setVisible(false); renew() }
    window.addEventListener('hashchange', changed)
    return () => window.removeEventListener('hashchange', changed)
  }, [])
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setNotice(''); setBusy(true)
    const data = new FormData(event.currentTarget)
    try {
      if (mode === 'login') {
        await api('/auth/login', 'POST', new URLSearchParams({ username: String(data.get('username')), password: String(data.get('password')), captcha: String(data.get('captcha')) }))
        await refreshCsrf(); onLogin(await api<Account>('/auth/me'))
      } else if (mode === 'forgot') {
        const result = await api<{ message: string }>('/auth/forgot-password', 'POST', { email: data.get('email') }); setNotice(result.message)
      } else {
        if (data.get('password') !== data.get('confirmation')) throw new Error('Las contraseñas no coinciden.')
        await api('/auth/reset-password', 'POST', { token: resetToken, newPassword: data.get('password') })
        window.history.replaceState(null, '', window.location.pathname); setResetToken(null); switchMode('login'); setNotice('Contraseña actualizada. Inicie sesión.'); await refreshCsrf()
      }
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo conectar.'); if (mode === 'login') { renew(); answer.current?.focus() } }
    finally { setBusy(false) }
  }
  return <main className="access">
    <aside className="access-story" aria-label="UTAPED, gestión documental académica">
      <Brand /><div className="access-story-copy"><p className="eyebrow">FISEI · UNIVERSIDAD TÉCNICA DE AMBATO</p><h2>Tu trabajo académico,<br /><em>bien conectado.</em></h2><p>Un espacio para planificar, organizar y dar seguimiento a la gestión documental de tu comisión.</p>
      <div className="document-illustration" aria-hidden="true"><div className="paper-back" /><div className="paper-front"><span className="paper-tag">PLAN DE TRABAJO</span><div className="paper-title">De la planificación<br />a los resultados.</div><div className="paper-line" /><div className="paper-line short" /><div className="paper-row"><span /><span /><span /></div><div className="paper-line" /><div className="paper-line short" /><span className="paper-seal"><Icon name="check" size={24} /></span></div><span className="floating-label"><Icon name="document" />Todo en su lugar</span></div>
      <div className="access-principles"><span><Icon name="lock" size={16} /> Acceso institucional</span><span><Icon name="document" size={16} /> Trazabilidad documental</span></div></div>
      <small className="access-institution">Facultad de Ingeniería en Sistemas, Electrónica e Industrial</small>
    </aside>
    <section className="access-form-pane"><div className="access-mobile-brand"><Brand /></div><div className="access-card">
      <span className="section-kicker">{mode === 'login' ? 'BIENVENIDO A TU ESPACIO' : 'ACCESO A TU CUENTA'}</span>
      <h1>{mode === 'login' ? 'Iniciar sesión' : mode === 'forgot' ? 'Recuperar acceso' : 'Nueva contraseña'}</h1>
      <p className="muted">{mode === 'login' ? 'Ingresa con las credenciales asignadas por la institución.' : mode === 'forgot' ? 'Enviaremos un enlace a tu correo institucional.' : 'Elige una contraseña nueva para proteger tu cuenta.'}</p>
      <form onSubmit={submit} key={mode}>
        <fieldset disabled={busy}>
        {mode === 'login' && <label>Usuario<input required name="username" maxLength={254} aria-label="Usuario" autoComplete="username" autoCapitalize="none" spellCheck={false} placeholder="tu.usuario" aria-describedby="username-help" /><small id="username-help" className="field-help">Identificador institucional. También puedes usar tu correo.</small></label>}
        {mode === 'forgot' && <label>Correo institucional<input required type="email" name="email" maxLength={254} autoComplete="email" placeholder="usuario@uta.edu.ec" /></label>}
        {mode !== 'forgot' && <div className="auth-password-field"><label htmlFor="access-password">{mode === 'reset' ? 'Nueva contraseña' : 'Contraseña'}</label><span className="password-control"><input id="access-password" required type={visible ? 'text' : 'password'} name="password" minLength={mode === 'reset' ? 12 : undefined} maxLength={72} autoComplete={mode === 'reset' ? 'new-password' : 'current-password'} /><button type="button" className="icon-button" title={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} aria-pressed={visible} onClick={() => setVisible(value => !value)}><Icon name="eye" /></button></span></div>}
        {mode === 'reset' && <><label>Confirmar contraseña<input required name="confirmation" type="password" minLength={12} maxLength={72} autoComplete="new-password" /></label><p className="field-help">Usa al menos 12 caracteres; máximo 72 bytes UTF-8.</p></>}
        {mode === 'login' && <div className="captcha-field"><div className="captcha-heading"><label htmlFor="captcha-answer">Código de verificación</label><button type="button" className="icon-button" title="Cambiar código de verificación" aria-label="Cambiar código de verificación" onClick={renew}><Icon name="refresh" /></button></div>
          {!captchaError && <img key={challenge} className="captcha-image" src={'/api/auth/captcha?v=' + challenge} alt="Código de verificación de seis caracteres" onLoad={() => { setCaptchaReady(true); setCaptchaError(false) }} onError={() => { setCaptchaReady(false); setCaptchaError(true) }} />}
          {captchaError && <p className="error" role="alert">No se pudo cargar el código. Pulsa Cambiar código de verificación para reintentar.</p>}
          {!captchaReady && !captchaError && <p className="field-help">Cargando código…</p>}
          <input ref={answer} id="captcha-answer" name="captcha" required minLength={6} maxLength={6} autoComplete="off" autoCapitalize="characters" spellCheck={false} placeholder="Escribe el código" aria-describedby="captcha-help" /><small id="captcha-help" className="field-help">No distingue mayúsculas y minúsculas. Válido por 3 minutos.</small>
        </div>}
        {error && <p role="alert" className="error">{error}</p>}{notice && <p role="status" className="notice">{notice}</p>}
        <button className="access-submit" disabled={busy || (mode === 'login' && !captchaReady)}>{busy ? 'Procesando…' : mode === 'login' ? 'Ingresar' : mode === 'forgot' ? 'Enviar enlace' : 'Guardar contraseña'}<Icon name="arrow" size={18} /></button>
        </fieldset>
      </form>
      <button className="link access-recovery" disabled={busy} onClick={() => switchMode(mode === 'login' ? 'forgot' : 'login')}>{mode === 'login' ? '¿Olvidó su contraseña?' : 'Volver al inicio de sesión'}</button>
      <p className="access-footnote"><Icon name="lock" size={15} /> Uso exclusivo de personal institucional autorizado.</p>
    </div><small className="access-copyright">UTAPED · Universidad Técnica de Ambato</small></section>
  </main>
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
  return <section className="card security-card"><span className="section-kicker">SEGURIDAD DE LA CUENTA</span><h2>{temporary ? 'Cambie su contraseña temporal' : 'Cambiar contraseña'}</h2><p className="muted">Use al menos 12 caracteres. Al guardar se cerrarán sus sesiones.</p>
    <form onSubmit={submit}><fieldset disabled={busy}><label>Contraseña actual<input required name="currentPassword" type="password" autoComplete="current-password" maxLength={128} /></label>
      <label>Nueva contraseña<input required name="newPassword" type="password" autoComplete="new-password" minLength={12} maxLength={72} /></label>
      <label>Confirmar contraseña<input required name="confirmation" type="password" autoComplete="new-password" minLength={12} maxLength={72} /></label>
      {error && <p className="error" role="alert">{error}</p>}<button disabled={busy}>{busy ? 'Guardando…' : 'Guardar contraseña'}</button></fieldset></form>
  </section>
}
