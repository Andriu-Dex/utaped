import { Toast } from '../shared/Toast'
import { useRef, useState, useEffect, type FormEvent } from 'react'
import { api, type User } from '../shared/api'
import { Icon } from '../shared/Icon'

export function TemporaryPasswordReset({ user, onComplete, onClose, onDirtyChange }: { user: User; onComplete: () => Promise<void>; onClose: () => void; onDirtyChange: (value: boolean) => void }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [showNew, setShowNew] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const form = useRef<HTMLFormElement>(null)
  const cardRef = useRef<HTMLElement>(null)

  useEffect(() => {
    cardRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [])

  async function reset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true)
    const data = new FormData(event.currentTarget)
    try {
      if (data.get('temporaryPassword') !== data.get('confirmation')) throw new Error('Las contraseñas no coinciden.')
      await api('/admin/users/' + user.id + '/temporary-password', 'POST', { rowVersion: user.row_version, temporaryPassword: data.get('temporaryPassword') })
      form.current?.reset(); onDirtyChange(false); await onComplete()
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo restablecer el acceso.') }
    finally { form.current?.reset(); onDirtyChange(false); setBusy(false) }
  }

  return <section ref={cardRef} className="card animate-reveal reset-access-card" aria-label="Restablecer acceso">
    <div className="reset-access-header">
      <div className="reset-access-title-group">
        <div className="reset-access-icon" aria-hidden="true">
          <Icon name="lock" size={22} />
        </div>
        <div>
          <span className="section-kicker">SEGURIDAD DE ACCESO</span>
          <h3 style={{ margin: '2px 0 0', fontSize: '1.25rem', fontWeight: 700 }}>Restablecer acceso de {user.display_name}</h3>
          <div className="reset-access-user-tag">
            <span>Usuario:</span>
            <strong>{user.display_name}</strong>
            {user.username && <span className="muted">(@{user.username})</span>}
            <span className="muted">·</span>
            <span className="muted">{user.email}</span>
          </div>
        </div>
      </div>
      <button type="button" className="icon-button" onClick={onClose} title="Cerrar" aria-label="Cerrar restablecimiento">
        <Icon name="x" size={18} />
      </button>
    </div>

    <div className="reset-security-notice" role="status">
      <Icon name="lock" size={18} />
      <div>
        <strong>Aviso de seguridad institucional:</strong>
        <p style={{ margin: '4px 0 0', fontSize: '0.84rem' }}>
          Se cerrarán sus sesiones y se exigirá cambiar la contraseña en el próximo ingreso. Entrega la contraseña temporal por un canal privado autorizado.
        </p>
      </div>
    </div>

    <form ref={form} onSubmit={reset} onChange={() => onDirtyChange(true)}>
      <fieldset disabled={busy}>
        <div className="reset-password-grid">
          <div className="auth-password-field">
            <label htmlFor="reset-new-password">Nueva contraseña temporal</label>
            <span className="password-control">
              <input
                id="reset-new-password"
                name="temporaryPassword"
                type={showNew ? 'text' : 'password'}
                required
                minLength={12}
                maxLength={72}
                autoComplete="new-password"
                autoFocus
                placeholder="Mínimo 12 caracteres"
              />
              <button
                type="button"
                className="icon-button"
                title={showNew ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                aria-label={showNew ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                aria-pressed={showNew}
                onClick={() => setShowNew(v => !v)}
              >
                <Icon name="eye" />
              </button>
            </span>
            <small className="field-help">Mínimo 12 caracteres (máx. 72).</small>
          </div>

          <div className="auth-password-field">
            <label htmlFor="reset-confirm-password">Confirmar contraseña temporal</label>
            <span className="password-control">
              <input
                id="reset-confirm-password"
                name="confirmation"
                type={showConfirm ? 'text' : 'password'}
                required
                minLength={12}
                maxLength={72}
                autoComplete="new-password"
                placeholder="Repite la contraseña temporal"
              />
              <button
                type="button"
                className="icon-button"
                title={showConfirm ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                aria-label={showConfirm ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                aria-pressed={showConfirm}
                onClick={() => setShowConfirm(v => !v)}
              >
                <Icon name="eye" />
              </button>
            </span>
            <small className="field-help">Debe coincidir exactamente.</small>
          </div>
        </div>

        <label className="reset-checkbox-wrap">
          <input type="checkbox" required />
          <span>Confirmo el restablecimiento de esta cuenta.</span>
        </label>

        {error && <Toast message={error} />}

        <div className="reset-actions">
          <button type="button" className="secondary" onClick={onClose} disabled={busy}>
            Cancelar restablecimiento
          </button>
          <button disabled={busy}>
            <Icon name="lock" size={16} />
            {busy ? 'Restableciendo…' : 'Restablecer contraseña'}
          </button>
        </div>
      </fieldset>
    </form>
  </section>
}
