import { Toast } from '../shared/Toast'
import { useEffect, useState, type FormEvent } from 'react'
import { api, type Group, type User, type Member } from '../shared/api'
import { PlanningAdministration } from '../planning/PlanningAdministration'
import { WorkflowAdministration } from '../workflow/WorkflowAdministration'
import { DirectoryAdministration } from './DirectoryAdministration'
import { AuditViewer } from '../audit/AuditViewer'
import { CertificateAdministration } from './CertificateAdministration'
import { adminPages, type AdminPage } from './adminNavigation'
import { Icon } from '../shared/Icon'

export function Administration({ page, periods, initialGroupId, onChange, onDirtyChange }: { page: AdminPage; periods: import('../shared/api').Period[]; initialGroupId?: string; onChange: () => Promise<void>; onDirtyChange: (dirty: boolean) => void }) {
  const [users, setUsers] = useState<User[]>([]); const [groups, setGroups] = useState<Group[]>([])
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const [memberGroup, setMemberGroup] = useState(initialGroupId ?? ''); const [members, setMembers] = useState<Member[]>([])
  const [dirtyCreates, setDirtyCreates] = useState<string[]>([])
  const [visible, setVisible] = useState(false); const [showCreateUser, setShowCreateUser] = useState(false)
  const createDirty = dirtyCreates.length > 0
  function markCreate(key: string) { setDirtyCreates(current => current.includes(key) ? current : [...current, key]) }
  const [flowDirty, setFlowDirty] = useState(false); const [profileDirty, setProfileDirty] = useState(false)
  const [reload, setReload] = useState(0); const [directoryQuery, setDirectoryQuery] = useState('')
  const [memberSearch, setMemberSearch] = useState(''); const [memberOptions, setMemberOptions] = useState<User[]>([])
  useEffect(() => { let active = true; api<{ items: User[] }>('/admin/users/directory?' + new URLSearchParams({ query: memberSearch, active: 'true', size: '100' })).then(data => { if (active) setMemberOptions(data.items) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [memberSearch, reload])
  useEffect(() => { onDirtyChange(createDirty || flowDirty || profileDirty); return () => onDirtyChange(false) }, [createDirty, flowDirty, profileDirty, onDirtyChange])
  useEffect(() => {
    let cancelled = false
    if (memberGroup) api<Member[]>('/groups/' + memberGroup + '/members').then(data => { if (!cancelled) setMembers(data) }).catch(e => { if (!cancelled) setError(e.message) })
    return () => { cancelled = true }
  }, [memberGroup, reload])
  async function remove(member: Member) {
    setBusy(true); setError('')
    try { await api('/admin/groups/' + memberGroup + '/members/' + member.id, 'DELETE'); setMembers(await api<Member[]>('/groups/' + memberGroup + '/members')); setNotice('Pertenencia retirada.'); await onChange() }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo retirar la pertenencia.') } finally { setBusy(false) }
  }
  async function load() { const [u, g] = await Promise.all([api<User[]>('/admin/users'), api<Group[]>('/admin/groups')]); setUsers(u); setGroups(g); setReload(n => n + 1) }
  useEffect(() => { Promise.resolve().then(load).catch(e => setError(e.message)) }, [])
  async function submit(event: FormEvent<HTMLFormElement>, path: string) {
    event.preventDefault(); const form = event.currentTarget; setError(''); setNotice(''); setBusy(true)
    const data = Object.fromEntries(new FormData(form))
    if (path === '/admin/users') { data.displayName = data.firstNames + ' ' + data.lastNames; if (!data.username) delete data.username }
    try {
      const target = path === 'membership' ? '/admin/groups/' + data.groupId + '/members' : path
      await api(target, 'POST', data); if (path === '/admin/users') { setDirectoryQuery(String(data.email)); setMemberSearch(String(data.email)) } form.reset(); setDirtyCreates(current => current.filter(key => key !== path)); setNotice('Cambios guardados.'); await load(); await onChange()
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.') } finally { setBusy(false) }
  }
  useEffect(() => {
    const prevent = (event: BeforeUnloadEvent) => { if (createDirty) { event.preventDefault(); event.returnValue = '' } }
    window.addEventListener('beforeunload', prevent); return () => window.removeEventListener('beforeunload', prevent)
  }, [createDirty])
  return <section aria-label={'Módulo: ' + adminPages.find(item => item.id === page)!.title}>
    <span className="section-kicker">ADMINISTRACIÓN INSTITUCIONAL</span><h1>{adminPages.find(item => item.id === page)!.title}</h1>
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    {page === 'users' && <>
    {!showCreateUser ? <div style={{ margin: '20px 0' }}><button onClick={() => setShowCreateUser(true)}>+ Agregar Usuario</button></div> :
    <section className="card animate-reveal"><div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}><h3 style={{ margin: 0 }}>Crear usuario</h3><button type="button" className="secondary" onClick={() => setShowCreateUser(false)}>Cancelar</button></div><form onChange={() => markCreate('/admin/users')} onSubmit={e => { submit(e, '/admin/users'); setShowCreateUser(false); }}>
      <div className="grid"><label>Nombres<input name="firstNames" required maxLength={80} /></label><label>Apellidos<input name="lastNames" required maxLength={80} /></label></div><label>Correo<input type="email" name="email" required maxLength={254} /></label>
      <label>Identificador de acceso<input name="username" aria-label="Identificador de acceso" maxLength={100} pattern="[a-zA-Z0-9][a-zA-Z0-9._\-]{0,99}" autoCapitalize="none" autoComplete="off" /><small className="field-help">Opcional. Si se deja vacío se deriva del correo; debe ser único.</small></label>
      <label>Permiso del sistema<select name="systemRole"><option value="USER">Usuario</option><option value="ADMIN">Administrador</option></select></label>
      <div className="auth-password-field"><label htmlFor="admin-temporary-password">Contraseña temporal</label><span className="password-control"><input id="admin-temporary-password" type={visible ? 'text' : 'password'} name="temporaryPassword" required minLength={12} maxLength={72} autoComplete="new-password" /><button type="button" className="icon-button" title={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} aria-pressed={visible} onClick={() => setVisible(v => !v)}><Icon name="eye" /></button></span></div>
      <button disabled={busy}>Crear usuario</button></form></section>}
    <DirectoryAdministration mode="users" groups={groups} reload={reload} suggestedQuery={directoryQuery} onChange={async () => { await load(); await onChange() }} onDirtyChange={setProfileDirty} />
    </>}
    {page === 'groups' && <>
    <section className="card"><h3>Crear grupo</h3><form onChange={() => markCreate('/admin/groups')} onSubmit={e => submit(e, '/admin/groups')}>
      <label>Nombre del grupo<input name="name" required maxLength={160} /></label><label>Tipo<select name="groupType"><option value="COMMISSION">Comisión</option><option value="UNIT">Unidad</option><option value="CLUB">Club</option><option value="OTHER">Otro</option></select></label>
      <button disabled={busy}>Crear grupo</button></form>
      <h3>Asignar integrante</h3><form onChange={() => markCreate('membership')} onSubmit={e => submit(e, 'membership')}><label>Grupo<select aria-label="Grupo" required name="groupId"><option value="">Seleccione</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>
        <label>Buscar usuario para asignar<input maxLength={200} value={memberSearch} onChange={e => setMemberSearch(e.target.value)} /></label><p>Se muestran hasta 100 coincidencias; refine la búsqueda por nombre o correo.</p>
        <label>Usuario<select aria-label="Usuario" required name="userId"><option value="">Seleccione</option>{memberOptions.map(u => <option key={u.id} value={u.id}>{u.display_name}</option>)}</select></label>
        <label>Rol de pertenencia<select name="membershipRole"><option value="MEMBER">Miembro</option><option value="COORDINATOR">Coordinador</option></select></label><button disabled={busy}>Asignar integrante</button></form>
    </section>
    <section className="card"><h3>Gestionar integrantes</h3><label>Grupo a gestionar<select aria-label="Grupo a gestionar" value={memberGroup} onChange={e => { setMemberGroup(e.target.value); setMembers([]) }}><option value="">Seleccione</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>{memberGroup && <ul>{members.map(m => <li key={m.id}>{m.display_name} <button className="secondary" disabled={busy} onClick={() => remove(m)} aria-label={'Retirar a ' + m.display_name}>Retirar pertenencia</button></li>)}</ul>}</section>
    <DirectoryAdministration mode="groups" groups={groups} reload={reload} suggestedQuery="" onChange={async () => { await load(); await onChange() }} onDirtyChange={setProfileDirty} />
    </>}
    {page === 'periods' && <>
<section id="admin-periods" className="card admin-block"><h3>Crear período</h3><form onChange={() => markCreate('/admin/periods')} className="grid" onSubmit={e => submit(e, '/admin/periods')}><label>Nombre del período<input name="name" required maxLength={120} /></label>
      {([['startsOn','Inicio del período'],['endsOn','Fin del período'],['preparationStartsOn','Inicio de elaboración'],['preparationEndsOn','Fin de elaboración'],['reviewStartsOn','Inicio de revisión'],['reviewEndsOn','Fin de revisión']] as const).map(([name,label]) => <label key={name}>{label}<input required type="date" name={name} /></label>)}<button disabled={busy}>Crear período</button></form></section>
    <section className="card"><h2>Períodos registrados</h2>{periods.length ? <ul className="period-list">{periods.map(period => <li key={period.id}><strong>{period.name}</strong><span>{period.starts_on.split('-').reverse().join('/')} — {period.ends_on.split('-').reverse().join('/')}</span></li>)}</ul> : <p>No hay períodos registrados.</p>}</section></>}
    {(page === 'resources' || page === 'activities' || page === 'holidays') && <PlanningAdministration groups={groups} section={page} onDirtyChange={setProfileDirty} />}
    {page === 'workflows' && <WorkflowAdministration groups={groups} users={users} suggestedQuery={directoryQuery} onDirtyChange={setFlowDirty} />}
    {page === 'audit' && <AuditViewer />}
    {page === 'certificates' && <CertificateAdministration onDirtyChange={setProfileDirty} />}
  </section>
}
