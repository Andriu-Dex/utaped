import { Toast } from '../shared/Toast'
import { useEffect, useState, type FormEvent } from 'react'
import { api, type User } from '../shared/api'
interface Binding { id: string; fingerprint: string; verificationNote: string; active: boolean; createdAt: string }
export function CertificateAdministration() {
  const [query, setQuery] = useState(''); const [users, setUsers] = useState<User[]>([]); const [user, setUser] = useState('')
  const [bindings, setBindings] = useState<Binding[]>([]); const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  useEffect(() => { let active = true; api<{ items: User[] }>('/admin/users/directory?' + new URLSearchParams({ query, active: 'true', size: '100' })).then(result => { if (active) setUsers(result.items) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [query])
  useEffect(() => { let active = true; if (user) api<Binding[]>('/admin/users/' + user + '/signing-certificates').then(result => { if (active) setBindings(result) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [user])
  async function register(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = event.currentTarget; const data = new FormData(form); setBusy(true); setError(''); setNotice('')
    try {
      await api('/admin/users/' + user + '/signing-certificates', 'POST', { fingerprint: String(data.get('fingerprint')), identityVerified: data.get('identityVerified') === 'on', verificationNote: String(data.get('verificationNote')), expectedBindingId: bindings.find(b => b.active)?.id ?? null })
      setBindings(await api<Binding[]>('/admin/users/' + user + '/signing-certificates')); form.reset(); setNotice('Huella pública vinculada.')
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo vincular el certificado.') } finally { setBusy(false) }
  }
  async function revoke(binding: Binding) {
    if (!window.confirm('¿Desvincular esta huella para impedir nuevas firmas? Los PDFs firmados permanecerán conservados.')) return
    setBusy(true); setError('')
    try { await api('/admin/users/' + user + '/signing-certificates/' + binding.id, 'DELETE'); setBindings(await api<Binding[]>('/admin/users/' + user + '/signing-certificates')); setNotice('Huella desvinculada.') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo desvincular la huella.') } finally { setBusy(false) }
  }
  return <section className="card" aria-label="Vinculación de certificados"><h2>Certificados públicos de firmantes</h2>
    <p>Vincule la huella SHA-256 obtenida del certificado público después de comprobar la identidad del titular por un canal confiable. No introduzca archivos privados ni contraseñas. Esto no acredita validación de la entidad emisora o revocación.</p>
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    <label>Buscar firmante<input value={query} disabled={busy} onChange={e => setQuery(e.target.value)} maxLength={200} /></label>
    <label>Cuenta del firmante<select value={user} disabled={busy} onChange={e => { setUser(e.target.value); setBindings([]); setError(''); setNotice('') }}><option value="">Seleccione una cuenta</option>{users.map(u => <option key={u.id} value={u.id}>{u.display_name} · {u.email}</option>)}</select></label>
    {user && <><form key={user} onSubmit={event => void register(event)}><fieldset disabled={busy}>
      <label>Huella SHA-256 pública<input name="fingerprint" pattern="[a-fA-F0-9]{64}" required minLength={64} maxLength={64} /></label>
      <label>Referencia de comprobación de identidad<textarea name="verificationNote" required maxLength={500} /></label>
      <label className="checkbox"><input name="identityVerified" type="checkbox" required />He comprobado que el certificado público corresponde a esta cuenta.</label>
      <button>Vincular huella pública</button></fieldset></form>
      <ul>{bindings.map(binding => <li key={binding.id}><p style={{ overflowWrap: 'anywhere' }}>{binding.fingerprint} · {binding.active ? 'Vinculada' : 'Desvinculada'}</p><p>{binding.verificationNote}</p>{binding.active && <button className="secondary" disabled={busy} onClick={() => void revoke(binding)}>Desvincular huella</button>}</li>)}</ul></>}
  </section>
}
