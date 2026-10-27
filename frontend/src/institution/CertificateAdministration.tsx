import { Toast } from '../shared/Toast'
import { useEffect, useState } from 'react'
import { api, type User } from '../shared/api'
interface Binding { id: string; fingerprint: string; active: boolean; createdAt: string }
export function CertificateAdministration({ onDirtyChange }: { onDirtyChange: (dirty: boolean) => void }) {
  const [query, setQuery] = useState(''), [users, setUsers] = useState<User[]>([]), [user, setUser] = useState('')
  const [bindings, setBindings] = useState<Binding[]>([]), [error, setError] = useState('')
  useEffect(() => { onDirtyChange(false) }, [onDirtyChange])
  useEffect(() => {
    let active = true
    api<{ items: User[] }>('/admin/users/directory?' + new URLSearchParams({ query, active: 'true', size: '100' })).then(result => { if (active) setUsers(result.items) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [query])
  useEffect(() => {
    if (!user) return
    let active = true
    api<Binding[]>('/admin/users/' + user + '/signing-certificates').then(result => { if (active) { setBindings(result); setError('') } }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [user])
  return <section className="card" aria-label="Historial de certificados"><h2>Historial de certificados públicos</h2>
    {error && <Toast message={error} onDismiss={() => setError('')} />}
    <div className="filter-toolbar"><label>Buscar firmante<input value={query} onChange={event => setQuery(event.target.value)} maxLength={200} /></label><label>Cuenta del firmante<select value={user} onChange={event => { setUser(event.target.value); setBindings([]); setError('') }}><option value="">Seleccione una cuenta</option>{users.map(person => <option key={person.id} value={person.id}>{person.display_name} · {person.email}</option>)}</select></label></div>
    {user && <ul className="admin-catalog-list">{bindings.map(binding => <li key={binding.id}><strong className="preserve-text">{binding.fingerprint}</strong><span> · {new Date(binding.createdAt).toLocaleString('es-EC')} · {binding.active ? 'Registro conservado' : 'Retirado'}</span></li>)}</ul>}
  </section>
}
