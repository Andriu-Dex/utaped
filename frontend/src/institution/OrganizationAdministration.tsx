import { confirmAction } from '../shared/confirm-action'
import { ActionForm } from '../shared/ActionForm'
import { useEffect, useState, type FormEvent } from 'react'
import { api, type Group, type Period, type User } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import './organization.css'
import { CareerEditor, type Career } from './CareerEditor'
import { ScopeHistory } from './ScopeHistory'

type Scope = { id: string; group_id: string; period_id: string; career_id: string | null; group_name: string; period_name: string; period_status: string; career_name: string | null; principal_user_id: string | null; principal_name: string | null; member_count: number; row_version: number }
type ScopeMember = { user_id: string; display_name: string; membership_role: string; position_label: string; active: boolean; user_active: boolean; password_reset_authorized: boolean; row_version: number }

export function OrganizationAdministration({ groups, periods, onDirtyChange, onChange }: { groups: Group[]; periods: Period[]; onDirtyChange: (dirty: boolean) => void; onChange: () => Promise<void> }) {
  const [careers, setCareers] = useState<Career[]>([]); const [scopes, setScopes] = useState<Scope[]>([])
  const [selected, setSelected] = useState(''); const [members, setMembers] = useState<ScopeMember[]>([])
  const [loading, setLoading] = useState(true); const [membersLoading, setMembersLoading] = useState(false)
  const [search, setSearch] = useState(''); const [users, setUsers] = useState<User[]>([])
  const [dirty, setDirty] = useState<string[]>([]); const [busy, setBusy] = useState(false)
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [reload, setReload] = useState(0)
  const scope = scopes.find(s => s.id === selected)
  function mark(key: string) { setDirty(d => d.includes(key) ? d : [...d, key]) }
  useEffect(() => { onDirtyChange(dirty.length > 0); return () => onDirtyChange(false) }, [dirty, onDirtyChange])
  useEffect(() => {
    let active = true
    Promise.all([api<Career[]>('/careers'), api<Scope[]>('/organization/scopes')]).then(([c, s]) => { if (active) { setCareers(c); setScopes(s); setLoading(false) } }).catch(e => { if (active) { setError(e.message); setLoading(false) } })
    return () => { active = false }
  }, [reload])
  useEffect(() => {
    let active = true
    if (selected) api<ScopeMember[]>('/organization/scopes/' + selected + '/members').then(m => { if (active) { setMembers(m); setMembersLoading(false) } }).catch(e => { if (active) { setError(e.message); setMembersLoading(false) } })
    return () => { active = false }
  }, [selected, reload])
  useEffect(() => {
    let active = true
    api<{ items: User[] }>('/admin/users/directory?' + new URLSearchParams({ query: search, active: 'true', size: '100' })).then(data => { if (active) setUsers(data.items) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [search])
  async function save(event: FormEvent<HTMLFormElement>, key: string, path: string, method: 'POST' | 'PUT') {
    event.preventDefault(); const form = event.currentTarget; const values = Object.fromEntries(new FormData(form))
    const data: Record<string, unknown> = { ...values }
    if (key === 'scope') data.careerId = values.careerId || null
    if (key === 'member') { data.rowVersion = members.find(m => m.user_id === values.userId)?.row_version ?? 0; data.active = true; data.passwordResetAuthorized = values.passwordResetAuthorized === 'on' }
    if (key === 'principal') data.rowVersion = scope?.row_version
    setBusy(true); setError(''); setNotice('')
    try { await api(path, method, data); form.reset(); setDirty(d => d.filter(k => k !== key)); setReload(r => r + 1); setNotice('Configuración guardada.'); await onChange() }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar. Los datos se conservan.') }
    finally { setBusy(false) }
  }
  async function retire(member: ScopeMember) {
    if (!(await confirmAction('¿Retirar la pertenencia de ' + member.display_name + ' de este ámbito? Se conserva su historial.'))) return
    setBusy(true); setError('')
    try { await api('/admin/organization/scopes/' + selected + '/members', 'PUT', { userId: member.user_id, membershipRole: member.membership_role, positionLabel: member.position_label, active: false, rowVersion: member.row_version }); setReload(r => r + 1); setNotice('Pertenencia retirada.'); await onChange() }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo retirar.') } finally { setBusy(false) }
  }
  return <div className="organization-workspace">
    <section className="card animate-reveal"><div className="section-heading"><span className="section-icon"><Icon name="users" /></span><div><span className="section-kicker">ESTRUCTURA INSTITUCIONAL</span><h2>Carreras y ámbitos de trabajo</h2></div></div><p className="muted">Un ámbito reúne grupo, carrera y período. Su responsable principal elabora el Plan; los integrantes mantienen su propia identidad.</p><div className="organization-metrics"><div><strong>{careers.length}</strong><span>Carreras registradas</span></div><div><strong>{scopes.length}</strong><span>Ámbitos configurados</span></div><div><strong>{scopes.filter(s => s.principal_user_id).length}</strong><span>Con responsable principal</span></div></div></section>
    {error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    <div className="organization-setup">
      <section className="card"><div className="section-heading"><span className="section-icon"><Icon name="document" /></span><div><span className="section-kicker">CATÁLOGO ACADÉMICO</span><h2>Registrar carrera</h2></div></div><ActionForm label="Crear carrera" dirty={dirty.includes('career')} disabled={busy} onCancel={() => setDirty(d => d.filter(k => k !== 'career'))}><form onChange={() => mark('career')} onSubmit={e => save(e, 'career', '/admin/careers', 'POST')}><fieldset disabled={busy}><label>Código de carrera<input name="code" required maxLength={30} pattern="[A-Z0-9_\-]{1,30}" placeholder="SOFTWARE" /></label><label>Nombre de carrera<input name="name" required maxLength={160} placeholder="Ingeniería de Software" /></label><button><Icon name="check" size={16} />Registrar carrera</button></fieldset></form></ActionForm>{careers.length > 0 && <ul className="organization-careers">{careers.map(c => <li key={c.id}><strong>{c.code}</strong><span>{c.name}</span></li>)}</ul>}<CareerEditor careers={careers} onSaved={() => setReload(r => r + 1)} onDirtyChange={value => setDirty(d => value ? [...d.filter(k => k !== 'career-edit'), 'career-edit'] : d.filter(k => k !== 'career-edit'))} /></section>
      <section className="card"><div className="section-heading"><span className="section-icon amber"><Icon name="calendar" /></span><div><span className="section-kicker">PLANIFICACIÓN POR PERÍODO</span><h2>Crear ámbito</h2></div></div><ActionForm label="Crear ámbito" dirty={dirty.includes('scope')} disabled={busy} onCancel={() => setDirty(d => d.filter(k => k !== 'scope'))}><form onChange={() => mark('scope')} onSubmit={e => save(e, 'scope', '/admin/organization/scopes', 'POST')}><fieldset disabled={busy}><label>Grupo del ámbito<select name="groupId" required><option value="">Seleccione un grupo</option>{groups.filter(g => g.active).map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label><div className="grid"><label>Período del ámbito<select name="periodId" required><option value="">Seleccione un período</option>{periods.filter(p => p.status === 'ACTIVE' && !p.workflow_locked_at).map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label><label>Carrera del ámbito<select name="careerId"><option value="">Unidad de facultad, sin carrera</option>{careers.filter(c => c.active).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label></div><p className="field-help">Cada combinación se registra una vez. El ámbito necesita integrantes y un responsable principal antes de crear su Plan.</p><button><Icon name="check" size={16} />Crear ámbito</button></fieldset></form></ActionForm></section>
    </div>
    <section className="card"><div className="section-heading"><span className="section-icon"><Icon name="settings" /></span><div><span className="section-kicker">RESPONSABILIDADES Y PERTENENCIAS</span><h2>Gestionar ámbito</h2></div></div>{loading ? <p role="status">Cargando organización…</p> : scopes.length === 0 ? <div className="empty-state"><Icon name="users" size={32} /><p>Aún no hay ámbitos configurados.</p><small>Registre el grupo y período, luego asigne sus integrantes.</small></div> : <label>Ámbito institucional<select aria-label="Ámbito institucional" value={selected} onChange={async e => { const nextValue = e.currentTarget.value; if (dirty.some(k => k === 'member' || k === 'principal') && !(await confirmAction('Hay cambios sin guardar. ¿Cambiar de ámbito?'))) return; setDirty(d => d.filter(k => k !== 'member' && k !== 'principal')); setSelected(nextValue); setMembers([]); setMembersLoading(!!nextValue) }}><option value="">Seleccione un ámbito</option>{scopes.map(s => <option key={s.id} value={s.id}>{s.group_name} · {s.career_name ?? 'Facultad'} · {s.period_name}</option>)}</select></label>}
      {scope && <div key={scope.id}><div className="organization-scope-summary"><span><strong>{scope.group_name}</strong><small>{scope.career_name ?? 'Ámbito de facultad'} · {scope.period_name}</small></span><span className="status-badge">Responsable: {scope.principal_name ?? 'Sin asignar'}</span></div>
        <label className="scope-import-id">ID del ámbito para importación CSV<input readOnly value={scope?.id ?? ''} onClick={e => e.currentTarget.select()} /></label>{membersLoading ? <p role="status">Cargando integrantes del ámbito…</p> : <><ActionForm label="Asignar o editar integrante" dirty={dirty.includes('member')} disabled={busy || scope.period_status === 'CLOSED'} onCancel={() => setDirty(d => d.filter(k => k !== 'member'))}><form onChange={() => mark('member')} onSubmit={e => save(e, 'member', '/admin/organization/scopes/' + scope.id + '/members', 'PUT')}><fieldset disabled={busy || scope.period_status === 'CLOSED'}><div className="grid"><label>Buscar integrante<input value={search} onChange={e => setSearch(e.target.value)} maxLength={200} placeholder="Nombre, usuario o correo" /></label><label>Integrante del ámbito<select name="userId" required onChange={e => { const member = members.find(m => m.user_id === e.target.value); const form = e.currentTarget.form; if (form) { (form.elements.namedItem('membershipRole') as HTMLSelectElement).value = member?.membership_role ?? 'MEMBER'; (form.elements.namedItem('positionLabel') as HTMLInputElement).value = member?.position_label ?? ''; (form.elements.namedItem('passwordResetAuthorized') as HTMLInputElement).checked = member?.password_reset_authorized ?? false } }}><option value="">Seleccione un usuario</option>{users.map(u => <option key={u.id} value={u.id}>{u.display_name}</option>)}</select></label></div><div className="grid"><label>Rol en el ámbito<select name="membershipRole"><option value="MEMBER">Miembro</option><option value="COORDINATOR">Coordinador</option></select></label><label>Cargo o función<input name="positionLabel" maxLength={160} placeholder="Denominación confirmada, cuando corresponda" /></label></div><label className="checkbox"><input type="checkbox" name="passwordResetAuthorized" />Autorizar recuperación de cuentas de este ámbito (solo coordinadores)</label><button><Icon name="users" size={16} />Guardar pertenencia</button></fieldset></form></ActionForm>
        <div className="table-scroll"><table><thead><tr><th>Integrante</th><th>Rol / función</th><th>Pertenencia</th><th>Acción</th></tr></thead><tbody>{members.map(m => <tr key={m.user_id}><td><strong>{m.display_name}</strong>{m.user_id === scope.principal_user_id && <small className="organization-principal">Responsable principal</small>}</td><td>{m.membership_role === 'COORDINATOR' ? 'Coordinador' : 'Miembro'}<small className="organization-principal">{m.position_label}</small></td><td><span className={'status-badge ' + (m.active ? 'active' : '')}>{m.active ? 'Activa' : 'Retirada'}</span></td><td><button type="button" className="secondary" title={'Retirar a ' + m.display_name} aria-label={'Retirar del ámbito a ' + m.display_name} disabled={busy || scope.period_status === 'CLOSED' || !m.active || m.user_id === scope.principal_user_id} onClick={() => retire(m)}><Icon name="x" size={16} /></button></td></tr>)}</tbody></table></div>{members.length === 0 && <p className="muted">Sin integrantes en este ámbito.</p>}
        <ActionForm label="Designar responsable principal" dirty={dirty.includes('principal')} disabled={busy || scope.period_status === 'CLOSED'} onCancel={() => setDirty(d => d.filter(k => k !== 'principal'))}><form onChange={() => mark('principal')} onSubmit={e => save(e, 'principal', '/admin/organization/scopes/' + scope.id + '/principal', 'PUT')}><fieldset disabled={busy || scope.period_status === 'CLOSED'}><label>Responsable principal<select required name="userId" defaultValue={scope.principal_user_id ?? ''} key={scope.row_version}><option value="">Seleccione un integrante activo</option>{members.filter(m => m.active && m.user_active).map(m => <option key={m.user_id} value={m.user_id}>{m.display_name}</option>)}</select></label><p className="field-help">La designación transfiere la edición y concede acceso explícito a los Planes de este ámbito. Se registra en auditoría; no cambia la identidad ni las firmas existentes.</p><button><Icon name="check" size={16} />Designar responsable principal</button></fieldset></form></ActionForm><ScopeHistory scopeId={scope.id} revision={reload} /></>}
      </div>}
    </section>
  </div>
}
