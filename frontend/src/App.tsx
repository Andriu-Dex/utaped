import { useEffect, useRef, useState } from 'react'
import { Access, ChangePassword } from './identity/Access'
import { Administration } from './institution/Administration'
import { WorkPlans } from './planning/WorkPlans'
import { TrackingDashboard } from './planning/TrackingDashboard'
import { Notifications, NotificationIndicator, type NotificationTarget } from './notifications/Notifications'
import { api, ApiError, refreshCsrf, type Account, type Group, type Member, type Period } from './shared/api'
import { Brand, Icon, type IconName } from './shared/Icon'
import './App.css'

type View = 'home' | 'admin' | 'password' | 'documents' | 'notifications'
const titles: Record<View, string> = { home: 'Mi espacio de trabajo', documents: 'Gestión documental', admin: 'Administración institucional', password: 'Seguridad de la cuenta', notifications: 'Centro de notificaciones' }
function displayDate(value: string) { return value.slice(0,10).split('-').reverse().join('/') }

export default function App() {
  const [account,setAccount] = useState<Account | null>(null); const [loading,setLoading] = useState(true)
  const [error,setError] = useState(''); const [groups,setGroups] = useState<Group[]>([]); const [periods,setPeriods] = useState<Period[]>([])
  const [selected,setSelected] = useState(''); const [members,setMembers] = useState<Member[]>([]); const [membersLoading,setMembersLoading] = useState(false)
  const [draftDirty,setDraftDirty] = useState(false); const [documentTarget,setDocumentTarget] = useState<string | null>(null)
  const [view,setView] = useState<View>('home'); const content = useRef<HTMLElement>(null)
  const [allPeriods,setAllPeriods] = useState(false)
  const identityEpoch = useRef(0)
  function navigate(next: View, targetDocId: string | null = null) {
    if (next === view && targetDocId === documentTarget) return
    if (!draftDirty || window.confirm('Hay cambios sin guardar. ¿Salir y descartarlos?')) { setDraftDirty(false); setDocumentTarget(targetDocId); setView(next) }
  }
  async function openNotification(target: NotificationTarget) {
    if (draftDirty && !window.confirm('Hay cambios sin guardar. ¿Salir y descartarlos?')) return
    setDraftDirty(false)
    if (target.type === 'WORK_PLAN') { setDocumentTarget(target.id); setView('documents') }
    else { await load(); setMembers([]); setMembersLoading(true); setSelected(target.id); setDocumentTarget(null); setView('home') }
  }
  function clearAccount() { identityEpoch.current += 1; setAccount(null); setSelected(''); setGroups([]); setPeriods([]); setMembers([]); setDraftDirty(false); setDocumentTarget(null); setView('home'); setAllPeriods(false) }
  useEffect(() => { content.current?.focus({ preventScroll: true }); window.scrollTo({ top: 0 }) }, [view])
  useEffect(() => {
    const expired = () => { setLoading(true); clearAccount(); setError('Su sesión expiró. Ingrese nuevamente.'); refreshCsrf().catch(() => {}).finally(() => setLoading(false)) }
    window.addEventListener('session-expired', expired)
    return () => window.removeEventListener('session-expired', expired)
  }, [])
  async function load() {
    const epoch=identityEpoch.current
    const [g,p] = await Promise.all([api<Group[]>('/groups'),api<Period[]>('/periods')]); if(epoch!==identityEpoch.current) return; setGroups(g); setPeriods(p)
    setSelected(current => g.some(group => group.id === current) ? current : '')
  }
  async function refreshInstitution() {
    const epoch=identityEpoch.current; await load(); if(epoch!==identityEpoch.current) return; const fresh = await api<Account>('/auth/me'); if(epoch!==identityEpoch.current) return
    setAccount(current => JSON.stringify(current) === JSON.stringify(fresh) ? current : fresh)
  }
  useEffect(() => {
    refreshCsrf().then(() => api<Account>('/auth/me')).then(setAccount)
      .catch(e => { if (!(e instanceof ApiError && e.status === 401)) setError(e.message) }).finally(() => setLoading(false))
  }, [])
  useEffect(() => {
    if (account && !account.mustChangePassword) Promise.resolve().then(load).catch(e => { setError(e.message); if (e instanceof ApiError && e.status === 401) clearAccount() })
  }, [account])
  useEffect(() => {
    let cancelled=false
    if (selected) api<Member[]>('/groups/' + selected + '/members').then(data => { if (!cancelled) setMembers(data) }).catch(e => { if (!cancelled) setError(e.message) }).finally(() => { if (!cancelled) setMembersLoading(false) })
    return () => { cancelled=true }
  }, [selected])
  async function logout() { if (draftDirty && !window.confirm('Hay cambios sin guardar. ¿Cerrar sesión y descartarlos?')) return; try { await api('/auth/logout','POST'); setLoading(true); clearAccount(); setError(''); await refreshCsrf() } catch(e) { setError(e instanceof Error ? e.message : 'No se pudo cerrar sesión.') } finally { setLoading(false) } }
  if (loading) return <main className="connecting"><Brand /><p role="status">Conectando con UTAPED…</p></main>
  if (!account) return <>{error && <p className="error session-alert" role="alert">{error}</p>}<Access onLogin={a => {setError('');setAccount(a)}} /></>
  function navButton(target: View, label: string, icon: IconName) { return <button aria-current={view === target ? 'page' : undefined} onClick={() => navigate(target)}><Icon name={icon} /><span>{label}</span>{view === target && <span className="nav-active-dot" />}</button> }
  return <div className="shell"><a className="skip-link" href="#main-content">Ir al contenido</a>
    <aside className="sidebar"><Brand /><p className="sidebar-section-label">ESPACIO INSTITUCIONAL</p>
      {!account.mustChangePassword && <nav className="primary-nav" aria-label="Navegación principal">
        {navButton('home','Inicio','home')}{navButton('documents','Documentos','document')}{navButton('notifications','Notificaciones','bell')}
        {account.systemRole === 'ADMIN' && navButton('admin','Administración','settings')}
        {navButton('password','Mi contraseña','lock')}
      </nav>}
      <div className="sidebar-institution"><span className="institution-mark">UTA</span><div><strong>FISEI</strong><small>Universidad Técnica de Ambato</small></div></div>
      <div className="sidebar-account"><span className="avatar">{account.displayName.trim().split(/\s+/).slice(0,2).map(word => word[0]).join('')}</span><div><strong>{account.displayName}</strong><small>{account.systemRole === 'ADMIN' ? 'Administrador' : 'Usuario institucional'}</small></div></div>
      <button className="logout-button" onClick={logout}><Icon name="logout" />Cerrar sesión</button>
    </aside>
    <div className="workspace"><header className="topbar"><div><span className="breadcrumb">UTAPED <span>/</span> {account.mustChangePassword ? 'Primer acceso' : titles[view]}</span><span className="workspace-caption">Gestión Documental Académica</span></div><div className="topbar-actions"><span className="institution-pill">FISEI · UTA</span>{!account.mustChangePassword && <NotificationIndicator onOpen={() => navigate('notifications')} />}</div></header>
      <main id="main-content" ref={content} tabIndex={-1}>{error && <p className="error" role="alert">{error}</p>}
      {account.mustChangePassword ? <ChangePassword temporary onComplete={clearAccount} /> : <>
        {view === 'notifications' ? <Notifications onOpen={openNotification} /> : view === 'documents' ? <WorkPlans onDirtyChange={setDraftDirty} initialDocId={documentTarget} /> : view === 'password' ? <ChangePassword temporary={false} onComplete={clearAccount} /> : view === 'admin' ? <Administration onChange={refreshInstitution} onDirtyChange={setDraftDirty} /> : <>
          <section className="welcome-banner"><div><span className="eyebrow">TU ESPACIO DE TRABAJO</span><h1>Bienvenido, {account.displayName}</h1><p>Organiza tu planificación y retoma tus documentos desde un solo lugar.</p><button className="hero-button" onClick={() => navigate('documents')}>Ver mis documentos<Icon name="arrow" size={18} /></button></div><div className="welcome-art" aria-hidden="true"><Icon name="document" size={88} /><span><Icon name="check" size={24} /></span></div></section>
          <TrackingDashboard onOpen={id => navigate('documents', id)} />
          <div className="context-grid"><section className="card context-card"><div className="section-heading"><span className="section-icon"><Icon name="users" /></span><div><span className="section-kicker">COLABORACIÓN</span><h2>Grupos institucionales</h2></div></div><p className="muted">Consulta tus integrantes. Tu identidad se conserva al cambiar de contexto.</p>
            {groups.length === 0 ? <div className="empty-state"><Icon name="users" size={32} /><p>No tiene grupos disponibles.</p><small>Solicita la asignación al administrador.</small></div> : <label>Contexto de grupo<select aria-label="Contexto de grupo" value={selected} onChange={e => { setMembers([]); setMembersLoading(!!e.target.value); setSelected(e.target.value) }}><option value="">Seleccione un grupo</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>}
            {selected && <><h3>Integrantes</h3>{membersLoading ? <p role="status">Cargando integrantes…</p> : members.length === 0 ? <p className="muted">Sin integrantes disponibles.</p> : <ul className="member-list">{members.map(m => <li key={m.id}>{m.display_name} · {m.membership_role === 'COORDINATOR' ? 'Coordinador' : 'Miembro'}</li>)}</ul>}</>}
          </section><section className="card context-card"><div className="section-heading"><span className="section-icon amber"><Icon name="calendar" /></span><div><span className="section-kicker">ORGANIZACIÓN ACADÉMICA</span><h2>Períodos académicos</h2></div></div>{periods.length === 0 ? <div className="empty-state"><Icon name="calendar" size={32} /><p>No hay períodos registrados.</p></div> : <ul className="period-list">{(allPeriods ? periods : periods.slice(0,4)).map(p => <li key={p.id}><strong>{p.name}</strong><span>{displayDate(p.starts_on)} — {displayDate(p.ends_on)}</span></li>)}</ul>}{periods.length > 4 && <button className="link" aria-expanded={allPeriods} onClick={() => setAllPeriods(value => !value)}>{allPeriods ? 'Mostrar menos períodos' : 'Ver todos los períodos (' + periods.length + ')'}</button>}</section></div>
        </>}
      </>}
      </main><footer className="workspace-footer"><span>UTAPED · Gestión Documental Académica</span><span>FISEI / Universidad Técnica de Ambato</span></footer>
    </div>
  </div>
}
