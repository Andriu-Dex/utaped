import { Toast } from '../shared/Toast'
import { useEffect, useState } from 'react'
import { api, type User } from '../shared/api'
import { Icon } from '../shared/Icon'

interface Binding { id: string; fingerprint: string; active: boolean; createdAt: string }

export function CertificateAdministration({ onDirtyChange }: { onDirtyChange: (dirty: boolean) => void }) {
  const [query, setQuery] = useState(''), [users, setUsers] = useState<User[]>([]), [user, setUser] = useState('')
  const [bindings, setBindings] = useState<Binding[]>([]), [error, setError] = useState('')

  useEffect(() => { onDirtyChange(false) }, [onDirtyChange])

  useEffect(() => {
    let active = true
    const controller = new AbortController()
    api<{ items: User[] }>('/admin/users/directory?' + new URLSearchParams({ query, active: 'true', size: '100' }), 'GET', undefined, controller.signal).then(result => { if (active) setUsers(result.items) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false; controller.abort() }
  }, [query])

  useEffect(() => {
    if (!user) return
    let active = true
    api<Binding[]>('/admin/users/' + user + '/signing-certificates').then(result => { if (active) { setBindings(result); setError('') } }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [user])

  return <section className="card" aria-label="Historial de certificados">
    <div className="section-heading">
      <div className="section-heading-icon" aria-hidden="true"><Icon name="lock" size={24} /></div>
      <div>
        <span className="section-heading-kicker">FIRMA ELECTRÓNICA</span>
        <h2 style={{ fontSize: '1.25rem', margin: 0 }}>Historial de certificados públicos</h2>
        <p className="muted" style={{ margin: '4px 0 0' }}>Consulte los certificados públicos y huellas digitales criptográficas registradas para cada usuario.</p>
      </div>
    </div>
    {error && <Toast message={error} onDismiss={() => setError('')} />}
    <div className="filter-toolbar" style={{ margin: '16px 0' }}>
      <div className="grid">
        <label>Buscar firmante
          <input value={query} onChange={event => { setQuery(event.currentTarget.value); setUsers([]) }} maxLength={200} placeholder="Nombre o correo..." />
        </label>
        <label>Cuenta del firmante
          <select value={user} onChange={event => { setUser(event.target.value); setBindings([]); setError('') }}>
            <option value="">Seleccione una cuenta</option>
            {users.map(person => <option key={person.id} value={person.id}>{person.display_name} · {person.email}</option>)}
          </select>
        </label>
      </div>
    </div>
    {!user ? (
      <div className="empty-state" style={{ margin: '24px 0 12px' }}>
        <Icon name="lock" size={32} />
        <p>Seleccione una cuenta institucional para consultar el registro de sus certificados públicos de firma.</p>
      </div>
    ) : bindings.length === 0 ? (
      <div className="empty-state" style={{ margin: '24px 0 12px' }}>
        <Icon name="document" size={32} />
        <p>No hay certificados registrados para esta cuenta institucional.</p>
      </div>
    ) : (
      <ul className="admin-catalog-list">
        {bindings.map(binding => (
          <li key={binding.id} className="admin-catalog-item">
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
              <span className="position-code-badge" style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: '0.82rem' }}>
                {binding.fingerprint}
              </span>
              <span className="muted">· {new Date(binding.createdAt).toLocaleString('es-EC')}</span>
              <span className={`status-badge ${binding.active ? 'active' : 'inactive'}`}>
                {binding.active ? 'Registro conservado' : 'Retirado'}
              </span>
            </div>
          </li>
        ))}
      </ul>
    )}
  </section>
}
