import { useEffect, useState } from 'react'
import { Access, ChangePassword } from './identity/Access'
import { Administration } from './institution/Administration'
import { WorkPlans } from './planning/WorkPlans'
import { api, ApiError, refreshCsrf, type Account, type Group, type Member, type Period } from './shared/api'
import './App.css'

function displayDate(value: string) { return value.slice(0,10).split('-').reverse().join('/') }

export default function App() {
  const [account,setAccount] = useState<Account | null>(null); const [loading,setLoading] = useState(true)
  const [error,setError] = useState(''); const [groups,setGroups] = useState<Group[]>([]); const [periods,setPeriods] = useState<Period[]>([])
  const [selected,setSelected] = useState(''); const [members,setMembers] = useState<Member[]>([])
  const [draftDirty,setDraftDirty] = useState(false)
  function navigate(next: 'home'|'admin'|'password'|'documents') { if (next === view) return; if (!draftDirty || window.confirm('Hay cambios sin guardar. ¿Salir y descartarlos?')) { setDraftDirty(false); setView(next) } }
  const [view,setView] = useState<'home'|'admin'|'password'|'documents'>('home')
  useEffect(() => {
    const expired = () => { setAccount(null); setSelected(''); setView('home'); setError('Su sesión expiró. Ingrese nuevamente.'); refreshCsrf().catch(() => {}) }
    window.addEventListener('session-expired', expired)
    return () => window.removeEventListener('session-expired', expired)
  }, [])
  async function load() {
    const [g,p] = await Promise.all([api<Group[]>('/groups'),api<Period[]>('/periods')]); setGroups(g); setPeriods(p)
    setSelected(current => g.some(group => group.id === current) ? current : '')
  }
  useEffect(() => {
    refreshCsrf().then(() => api<Account>('/auth/me')).then(setAccount)
      .catch(e => { if (!(e instanceof ApiError && e.status === 401)) setError(e.message) }).finally(() => setLoading(false))
  }, [])
  useEffect(() => {
    if (account && !account.mustChangePassword) Promise.resolve().then(load).catch(e => { setError(e.message); if (e instanceof ApiError && e.status === 401) setAccount(null) })
  }, [account])
  useEffect(() => {
    let cancelled=false
    if (selected) api<Member[]>('/groups/' + selected + '/members').then(data => { if (!cancelled) setMembers(data) }).catch(e => { if (!cancelled) setError(e.message) })
    return () => { cancelled=true }
  }, [selected])
  async function logout() { if (draftDirty && !window.confirm('Hay cambios sin guardar. ¿Cerrar sesión y descartarlos?')) return; setDraftDirty(false); try { await api('/auth/logout','POST'); setAccount(null); setSelected(''); setView('home'); await refreshCsrf() } catch(e) { setError(e instanceof Error ? e.message : 'No se pudo cerrar sesión.') } }
  if (loading) return <main className="access"><p role="status">Conectando con UTAPED…</p></main>
  if (!account) return <>{error && <p className="error" role="alert">{error}</p>}<Access onLogin={a => {setError('');setAccount(a)}} /></>
  return <div className="shell"><header><div><strong>UTAPED</strong><span>Gestión Documental Académica</span></div><div><span>{account.displayName}</span><button className="secondary" onClick={logout}>Cerrar sesión</button></div></header>
    <main>{error && <p className="error" role="alert">{error}</p>}
    {account.mustChangePassword ? <ChangePassword temporary onComplete={() => { setAccount(null); setView('home') }} /> : <>
      <nav aria-label="Navegación principal"><button aria-current={view === 'home' ? 'page' : undefined} onClick={() => navigate('home')}>Inicio</button><button aria-current={view === 'documents' ? 'page' : undefined} onClick={() => navigate('documents')}>Documentos</button>{account.systemRole === 'ADMIN' && <button aria-current={view === 'admin' ? 'page' : undefined} onClick={() => navigate('admin')}>Administración</button>}<button aria-current={view === 'password' ? 'page' : undefined} onClick={() => navigate('password')}>Mi contraseña</button></nav>
      {view === 'documents' ? <WorkPlans onDirtyChange={setDraftDirty} /> : view === 'password' ? <ChangePassword temporary={false} onComplete={() => {setAccount(null);setView('home')}} /> : view === 'admin' ? <Administration onChange={load} /> : <>
        <h1>Bienvenido, {account.displayName}</h1><p>Seleccione un grupo. Su identidad se conserva al cambiar de contexto.</p>
        <section className="card"><h2>Grupos institucionales</h2>{groups.length === 0 ? <p>No tiene grupos disponibles.</p> : <label>Contexto de grupo<select aria-label="Contexto de grupo" value={selected} onChange={e => setSelected(e.target.value)}><option value="">Seleccione un grupo</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>}
          {selected && <><h3>Integrantes</h3>{members.length === 0 ? <p>Sin integrantes disponibles.</p> : <ul>{members.map(m => <li key={m.id}>{m.display_name} · {m.membership_role === 'COORDINATOR' ? 'Coordinador' : 'Miembro'}</li>)}</ul>}</>}
        </section><section className="card"><h2>Períodos académicos</h2>{periods.length === 0 ? <p>No hay períodos registrados.</p> : <ul>{periods.map(p => <li key={p.id}>{p.name} · {displayDate(p.starts_on)} — {displayDate(p.ends_on)}</li>)}</ul>}</section>
      </>}
    </>}
    </main></div>
}
