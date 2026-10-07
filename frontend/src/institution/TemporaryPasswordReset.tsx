import { useRef, useState, type FormEvent } from 'react'
import { api, type User } from '../shared/api'

export function TemporaryPasswordReset({ user, onComplete, onClose, onDirtyChange }: { user: User; onComplete: () => Promise<void>; onClose: () => void; onDirtyChange: (value: boolean) => void }) {
  const [busy, setBusy] = useState(false); const [error, setError] = useState('')
  const form = useRef<HTMLFormElement>(null)
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
  return <section className="conflict" aria-label="Restablecer acceso"><h3>Restablecer acceso de {user.display_name}</h3><p>Se cerrarán sus sesiones y se exigirá cambiar la contraseña en el próximo ingreso. Entrega la contraseña temporal por un canal privado autorizado.</p>
    <form ref={form} onSubmit={reset} onChange={() => onDirtyChange(true)}><fieldset disabled={busy}>
      <label>Nueva contraseña temporal<input name="temporaryPassword" type="password" required minLength={12} maxLength={72} autoComplete="new-password" autoFocus /></label>
      <label>Confirmar contraseña temporal<input name="confirmation" type="password" required minLength={12} maxLength={72} autoComplete="new-password" /></label>
      <label className="check"><input type="checkbox" required />Confirmo el restablecimiento de esta cuenta.</label>
      {error && <p className="error" role="alert">{error}</p>}
      <button disabled={busy}>{busy ? 'Restableciendo…' : 'Restablecer contraseña'}</button>{' '}<button type="button" className="secondary" onClick={onClose}>Cancelar restablecimiento</button>
    </fieldset></form>
  </section>
}
