import { confirmAction } from '../shared/confirm-action'
import './administration.css'
import { ActionForm } from '../shared/ActionForm'
import { UserImportWorkspace } from './UserImportWorkspace'
import { DeadlineNotices } from './DeadlineNotices'
import { PeriodControl } from './PeriodControl'
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
import { OrganizationAdministration } from './OrganizationAdministration'
import { TemplateAdministration } from './TemplateAdministration'
import { PositionAdministration } from './PositionAdministration'

export function Administration({ page, periods, initialGroupId, onChange, onDirtyChange }: { page: AdminPage; periods: import('../shared/api').Period[]; initialGroupId?: string; onChange: () => Promise<void>; onDirtyChange: (dirty: boolean) => void }) {
  const [users, setUsers] = useState<User[]>([]); const [groups, setGroups] = useState<Group[]>([])
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const [memberGroup, setMemberGroup] = useState(initialGroupId ?? ''); const [members, setMembers] = useState<Member[]>([]); const [loadedMemberGroup, setLoadedMemberGroup] = useState('')
  const [dirtyCreates, setDirtyCreates] = useState<string[]>([])
  const [visible, setVisible] = useState(false); const [showCreateUser, setShowCreateUser] = useState(false)
  const createDirty = dirtyCreates.length > 0
  function markCreate(key: string) { setDirtyCreates(current => current.includes(key) ? current : [...current, key]) }
  const [importDirty, setImportDirty] = useState(false)
  const [flowDirty, setFlowDirty] = useState(false); const [profileDirty, setProfileDirty] = useState(false); const [templateDirty, setTemplateDirty] = useState(false)
  const [reload, setReload] = useState(0); const [directoryQuery, setDirectoryQuery] = useState('')
  const [memberSearch, setMemberSearch] = useState(''); const [memberOptions, setMemberOptions] = useState<User[]>([])
  useEffect(() => { let active = true; api<{ items: User[] }>('/admin/users/directory?' + new URLSearchParams({ query: memberSearch, active: 'true', size: '100' })).then(data => { if (active) setMemberOptions(data.items) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [memberSearch, reload])
  useEffect(() => { onDirtyChange(createDirty || flowDirty || profileDirty || importDirty || templateDirty); return () => onDirtyChange(false) }, [createDirty, flowDirty, profileDirty, importDirty, templateDirty, onDirtyChange])
  useEffect(() => {
    let cancelled = false
    if (memberGroup) api<Member[]>('/groups/' + memberGroup + '/members').then(data => { if (!cancelled) { setMembers(data); setLoadedMemberGroup(memberGroup) } }).catch(e => { if (!cancelled) setError(e.message) })
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
    if (path === '/admin/periods' && !data.preparationEndsOn) delete data.preparationEndsOn
    if (path === '/admin/users') { data.displayName = data.firstNames + ' ' + data.lastNames; if (!data.username) delete data.username }
    try {
      const target = path === 'membership' ? '/admin/groups/' + data.groupId + '/members' : path
      await api(target, 'POST', data); if (path === '/admin/users') { setDirectoryQuery(String(data.email)); setMemberSearch(String(data.email)) } form.reset(); if (path === '/admin/users') { setShowCreateUser(false); setVisible(false) } setDirtyCreates(current => current.filter(key => key !== path)); setNotice('Cambios guardados.'); await load(); await onChange()
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.') } finally { setBusy(false) }
  }
  useEffect(() => {
    const prevent = (event: BeforeUnloadEvent) => { if (createDirty) { event.preventDefault(); event.returnValue = '' } }
    window.addEventListener('beforeunload', prevent); return () => window.removeEventListener('beforeunload', prevent)
  }, [createDirty])
  return <section aria-label={'Módulo: ' + adminPages.find(item => item.id === page)!.title}>
    <div className="admin-overview-heading" style={{ marginBottom: 20 }}>
      <div>
        <span className="section-kicker">ADMINISTRACIÓN INSTITUCIONAL</span>
        <h1>{adminPages.find(item => item.id === page)!.title}</h1>
        {page === 'users' && <p className="muted">Administra las cuentas de usuario, roles de acceso y perfiles del personal docente y directivo.</p>}
        {page === 'groups' && <p className="muted">Administra comisiones académicas, unidades, clubes y la pertenencia de sus integrantes.</p>}
        {page === 'organization' && <p className="muted">Estructura unidades académicas, catálogo de carreras y ámbitos de planificación por período.</p>}
        {page === 'positions' && <p className="muted">Define los cargos institucionales y las autoridades asignadas para cada período académico.</p>}
        {page === 'periods' && <p className="muted">Administra el calendario académico, ventanas de elaboración, revisión y cierre de períodos.</p>}
      </div>
      {page === 'users' && !showCreateUser && (
        <div className="admin-header-actions">
          <button type="button" className="primary" onClick={() => setShowCreateUser(true)}>
            + Agregar Usuario
          </button>
        </div>
      )}
    </div>
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    {page === 'organization' && <OrganizationAdministration groups={groups} periods={periods} onDirtyChange={setProfileDirty} onChange={onChange} />}
    {page === 'users' && <>
    {showCreateUser && (
      <section className="card animate-reveal" style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ margin: 0 }}>Crear usuario</h3>
          <button type="button" className="secondary" disabled={busy} onClick={async () => {
            if (!dirtyCreates.includes('/admin/users') || (await confirmAction('Hay cambios sin guardar. ¿Cancelar la creación?'))) {
              setShowCreateUser(false); setVisible(false); setDirtyCreates(keys => keys.filter(key => key !== '/admin/users'))
            }
          }}>
            Cancelar
          </button>
        </div>
        <form onChange={() => markCreate('/admin/users')} onSubmit={e => void submit(e, '/admin/users')}>
          <div className="grid">
            <label>Nombres<input name="firstNames" required maxLength={80} /></label>
            <label>Apellidos<input name="lastNames" required maxLength={80} /></label>
          </div>
          <label>Correo<input type="email" name="email" required maxLength={254} /></label>
          <label>Identificador de acceso<input name="username" aria-label="Identificador de acceso" maxLength={100} pattern="[a-zA-Z0-9][a-zA-Z0-9._\-]{0,99}" autoCapitalize="none" autoComplete="off" /><small className="field-help">Opcional. Si se deja vacío se deriva del correo; debe ser único.</small></label>
          <label>Permiso del sistema<select name="systemRole"><option value="USER">Usuario</option><option value="ADMIN">Administrador</option></select></label>
          <div className="auth-password-field">
            <label htmlFor="admin-temporary-password">Contraseña temporal</label>
            <span className="password-control">
              <input id="admin-temporary-password" type={visible ? 'text' : 'password'} name="temporaryPassword" required minLength={12} maxLength={72} autoComplete="new-password" />
              <button type="button" className="icon-button" title={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} aria-pressed={visible} onClick={() => setVisible(v => !v)}><Icon name="eye" /></button>
            </span>
          </div>
          <button disabled={busy}>Crear usuario</button>
        </form>
      </section>
    )}
    <UserImportWorkspace onDirtyChange={setImportDirty} onChange={async () => { await load(); await onChange() }} />
    <DirectoryAdministration mode="users" groups={groups} reload={reload} suggestedQuery={directoryQuery} onChange={async () => { await load(); await onChange() }} onDirtyChange={setProfileDirty} />
    </>}
    {page === 'groups' && <>
      <div className="groups-metrics-grid">
        <div className="group-metric-card">
          <div className="group-metric-icon icon-blue"><Icon name="columns" size={20} /></div>
          <div className="group-metric-content">
            <strong>{groups.length}</strong>
            <span>Grupos registrados</span>
          </div>
        </div>
        <div className="group-metric-card">
          <div className="group-metric-icon icon-teal"><Icon name="check" size={20} /></div>
          <div className="group-metric-content">
            <strong>{groups.filter(g => g.active).length}</strong>
            <span>Grupos activos</span>
          </div>
        </div>
        <div className="group-metric-card">
          <div className="group-metric-icon icon-indigo"><Icon name="users" size={20} /></div>
          <div className="group-metric-content">
            <strong>{groups.filter(g => g.group_type === 'COMMISSION').length}</strong>
            <span>Comisiones</span>
          </div>
        </div>
        <div className="group-metric-card">
          <div className="group-metric-icon icon-amber"><Icon name="archive" size={20} /></div>
          <div className="group-metric-content">
            <strong>{groups.filter(g => g.group_type !== 'COMMISSION').length}</strong>
            <span>Unidades y clubes</span>
          </div>
        </div>
      </div>

      <div className="groups-actions-grid">
        <section className="card group-action-panel animate-reveal">
          <div className="group-panel-header">
            <span className="section-icon"><Icon name="columns" /></span>
            <div>
              <span className="section-kicker">NUEVO GRUPO</span>
              <h2>Crear grupo</h2>
              <p className="field-help">Registra una comisión, unidad académica o club.</p>
            </div>
          </div>
          <ActionForm label="Crear grupo" dirty={dirtyCreates.includes('/admin/groups')} disabled={busy} onCancel={() => setDirtyCreates(keys => keys.filter(key => key !== '/admin/groups'))}><form onChange={() => markCreate('/admin/groups')} onSubmit={e => submit(e, '/admin/groups')} style={{ marginTop: 16 }}>
            <div className="grid"><label>Nombre del grupo<input name="name" required maxLength={160} placeholder="Ej: Comisión de Vinculación con la Sociedad" /></label><label>Tipo<select name="groupType"><option value="COMMISSION">Comisión</option><option value="UNIT">Unidad</option><option value="CLUB">Club</option><option value="OTHER">Otro</option></select></label></div>
            <button disabled={busy}><Icon name="users" size={16} />Crear grupo</button>
          </form></ActionForm>
        </section>

        <section className="card group-action-panel">
          <div className="group-panel-header">
            <span className="section-icon teal"><Icon name="users" /></span>
            <div>
              <span className="section-kicker">ASIGNACIÓN DE PERTENENCIAS</span>
              <h2>Asignar integrante</h2>
              <p className="field-help">Vincula personal docente como miembro o coordinador.</p>
            </div>
          </div>
          <ActionForm label="Asignar integrante" dirty={dirtyCreates.includes('membership')} disabled={busy} onCancel={() => setDirtyCreates(keys => keys.filter(key => key !== 'membership'))}><form onChange={() => markCreate('membership')} onSubmit={e => submit(e, 'membership')} style={{ marginTop: 16 }}>
            <div className="grid">
              <label>Grupo<select aria-label="Grupo" required name="groupId"><option value="">Seleccione</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>
              <label>Buscar usuario para asignar<input maxLength={200} placeholder="Buscar por nombre o correo…" value={memberSearch} onChange={e => { setMemberSearch(e.currentTarget.value); setMemberOptions([]) }} /></label>
            </div>
            <p className="field-help">Se muestran hasta 100 coincidencias activas; refine la búsqueda si no encuentra al usuario.</p>
            <div className="grid">
              <label>Usuario<select aria-label="Usuario" required name="userId"><option value="">Seleccione</option>{memberOptions.map(u => <option key={u.id} value={u.id}>{u.display_name}</option>)}</select></label>
              <label>Rol de pertenencia<select name="membershipRole"><option value="MEMBER">Miembro</option><option value="COORDINATOR">Coordinador</option></select></label>
            </div>
            <button disabled={busy}><Icon name="check" size={16} />Asignar integrante</button>
          </form></ActionForm>
        </section>
      </div>

      <section className="card group-members-card">
        <div className="section-heading"><span className="section-icon amber"><Icon name="users" /></span><div><span className="section-kicker">GESTIÓN DE MIEMBROS</span><h2>Gestionar integrantes</h2></div></div>
        <p className="muted" style={{ margin: '4px 0 16px' }}>Consulta los integrantes asignados a una comisión o unidad y retira membresías si es necesario.</p>
        <div className="group-select-row">
          <label>Grupo a gestionar<select aria-label="Grupo a gestionar" value={memberGroup} onChange={e => { setMemberGroup(e.target.value); setLoadedMemberGroup(''); setMembers([]) }}><option value="">Seleccione un grupo</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>
          {memberGroup && <>
            {loadedMemberGroup !== memberGroup ? (
              <p role="status">Cargando integrantes…</p>
            ) : members.length === 0 ? (
              <div className="empty-state" style={{ margin: '14px 0 0' }}>
                <Icon name="users" size={24} />
                <p>Este grupo no tiene integrantes asignados actualmente.</p>
              </div>
            ) : (
              <ul className="member-list" style={{ marginTop: 16 }}>
                {members.map(m => (
                  <li key={m.id} className="group-member-item">
                    <span className="group-member-info">
                      <span className="user-avatar-badge" aria-hidden="true">{m.display_name.charAt(0).toUpperCase()}</span>
                      <strong>{m.display_name}</strong>
                    </span>
                    <button className="secondary group-remove-btn" disabled={busy} onClick={() => remove(m)} aria-label={'Retirar a ' + m.display_name}>
                      <Icon name="x" size={14} />Retirar pertenencia
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </>}
        </div>
      </section>
      <DirectoryAdministration mode="groups" groups={groups} reload={reload} suggestedQuery="" onChange={async () => { await load(); await onChange() }} onDirtyChange={setProfileDirty} />
    </>}

    {page === 'periods' && <>
    <section id="admin-periods" className="card admin-block animate-reveal">
      <div className="section-heading"><span className="section-icon"><Icon name="calendar" /></span><div><span className="section-kicker">ORGANIZACIÓN ACADÉMICA</span><h2>Crear período</h2></div></div>
      <ActionForm label="Crear período" dirty={dirtyCreates.includes('/admin/periods')} disabled={busy} onCancel={() => setDirtyCreates(keys => keys.filter(key => key !== '/admin/periods'))}><form onChange={() => markCreate('/admin/periods')} onSubmit={e => submit(e, '/admin/periods')} style={{ marginTop: 16 }}>
        <label>Nombre del período<input name="name" required maxLength={120} placeholder="Ej: Octubre 2026 - Febrero 2027" /></label>
        <div className="period-dates-box">
          <span className="eyebrow" style={{ display: 'block', marginBottom: 8, color: 'var(--teal-700)', fontWeight: 700 }}>FECHAS DEL CICLO ACADÉMICO</span>
          <div className="grid">
            {([['startsOn','Inicio del período'],['endsOn','Fin del período']] as const).map(([name,label]) => <label key={name} style={{ margin: '8px 0' }}>{label}<input required type="date" name={name} /></label>)}
          </div>
        </div>
        <div className="grid" style={{ marginBottom: 16 }}>
          {([['preparationStartsOn','Inicio de elaboración'],['preparationEndsOn','Fin de elaboración'],['reviewStartsOn','Inicio de revisión'],['reviewEndsOn','Fin de revisión']] as const).map(([name,label]) => <label key={name}>{label}<input required={name !== 'preparationEndsOn'} type="date" name={name} /></label>)}
        </div>
        <p className="field-help">Fin de elaboración: déjelo vacío para calcular cinco días hábiles desde el inicio, excluyendo fines de semana y feriados registrados. Indique otra fecha para configurar una ventana diferente; los períodos existentes no cambian.</p><button disabled={busy}><Icon name="calendar" size={16} />Crear período</button>
      </form></ActionForm>
    </section>
    <PeriodControl periods={periods} onChange={onChange} onDirtyChange={setProfileDirty} />
    <DeadlineNotices />
    </>}
    {page === 'templates' && <TemplateAdministration onDirtyChange={setTemplateDirty} />}
    {(page === 'resources' || page === 'activities' || page === 'holidays') && <PlanningAdministration groups={groups} section={page} onDirtyChange={setProfileDirty} />}
    {page === 'positions' && <PositionAdministration periods={periods} onDirtyChange={setProfileDirty} />}
    {page === 'workflows' && <WorkflowAdministration groups={groups} users={users} suggestedQuery={directoryQuery} onDirtyChange={setFlowDirty} />}
    {page === 'audit' && <AuditViewer />}
    {page === 'certificates' && <CertificateAdministration onDirtyChange={setProfileDirty} />}
  </section>
}
