import { useEffect, useState } from 'react'
import { api, type Group, type User } from '../shared/api'
interface Directory { items: User[]; total: number; page: number; size: number }
export function DirectoryAdministration({ groups, reload, suggestedQuery, onChange, onDirtyChange }: { groups: Group[]; reload: number; suggestedQuery: string; onChange: () => Promise<void>; onDirtyChange: (dirty: boolean) => void }) {
  const [query, setQuery] = useState(''); const [filter, setFilter] = useState({ query: '', active: '', page: 0 }); const [directory, setDirectory] = useState<Directory | null>(null)
  const [user, setUser] = useState<User | null>(null); const [group, setGroup] = useState<Group | null>(null)
  const [baseline, setBaseline] = useState(''); const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false); const [refresh, setRefresh] = useState(0)
  const dirty = !!user || !!group ? JSON.stringify(user ?? group) !== baseline : false
  useEffect(() => { onDirtyChange(dirty); const prevent = (e: BeforeUnloadEvent) => { if (dirty) { e.preventDefault(); e.returnValue = '' } }; window.addEventListener('beforeunload', prevent); return () => { onDirtyChange(false); window.removeEventListener('beforeunload', prevent) } }, [dirty, onDirtyChange])
  const [previousSuggestion, setPreviousSuggestion] = useState(suggestedQuery)
  if (previousSuggestion !== suggestedQuery) { setPreviousSuggestion(suggestedQuery); setQuery(suggestedQuery); setFilter({ query: suggestedQuery, active: '', page: 0 }) }
  useEffect(() => { let active = true; const params = new URLSearchParams({ query: filter.query, page: String(filter.page) }); if (filter.active) params.set('active', filter.active); api<Directory>('/admin/users/directory?' + params).then(data => { if (active) setDirectory(data) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [filter, reload, refresh])
  function canLeave() { return !dirty || window.confirm('¿Descartar los cambios del perfil sin guardar?') }
  function editUser(value: User) { if (canLeave()) { setUser({ ...value }); setGroup(null); setBaseline(JSON.stringify(value)); setError(''); setNotice('') } }
  function editGroup(value: Group) { if (canLeave()) { setGroup({ ...value }); setUser(null); setBaseline(JSON.stringify(value)); setError(''); setNotice('') } }
  async function save() {
    setBusy(true); setError(''); setNotice('')
    try {
      if (user) await api('/admin/users/' + user.id, 'PUT', { rowVersion: user.row_version, displayName: user.display_name, systemRole: user.system_role })
      if (group) await api('/admin/groups/' + group.id, 'PUT', { rowVersion: group.row_version, name: group.name, groupType: group.group_type, active: group.active })
      setUser(null); setGroup(null); setRefresh(n => n + 1); setNotice('Perfil actualizado.'); await onChange()
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar el perfil.') } finally { setBusy(false) }
  }
  async function toggle(value: User) {
    if (!canLeave()) return; setBusy(true); setError(''); setNotice('')
    try { await api('/admin/users/' + value.id + '/active', 'PATCH', { rowVersion: value.row_version, active: !value.active }); setUser(null); setGroup(null); setRefresh(n => n + 1); setNotice('Acceso actualizado.'); await onChange() }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo cambiar el acceso.') } finally { setBusy(false) }
  }
  async function reloadProfile() {
    if (!canLeave()) return; setError('')
    try { if (user) { const latest = await api<User>('/admin/users/' + user.id); setUser(latest); setBaseline(JSON.stringify(latest)) } if (group) { const latest = (await api<Group[]>('/admin/groups')).find(g => g.id === group.id); if (latest) { setGroup(latest); setBaseline(JSON.stringify(latest)) } } }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo recargar.') }
  }
  return <section className="card"><h3>Directorio y perfiles</h3>
    {error && <p className="error" role="alert">{error}</p>}{notice && <p className="notice" role="status">{notice}</p>}
    <form className="grid" onSubmit={e => { e.preventDefault(); setFilter(f => ({ ...f, query, page: 0 })) }}><label>Buscar usuarios<input maxLength={200} value={query} onChange={e => setQuery(e.target.value)} /></label><label>Estado de acceso<select value={filter.active} onChange={e => setFilter(f => ({ ...f, active: e.target.value, page: 0 }))}><option value="">Todos</option><option value="true">Activos</option><option value="false">Inactivos</option></select></label><button>Buscar usuarios</button></form>
    {!directory ? <p>Cargando directorio…</p> : <><div className="table-scroll"><table><thead><tr><th>Nombre</th><th>Correo</th><th>Acceso</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{directory.items.map(u => <tr key={u.id}><td>{u.display_name}</td><td>{u.email}</td><td>{u.system_role === 'ADMIN' ? 'Administrador' : 'Usuario'}</td><td>{u.active ? 'Activo' : 'Inactivo'}</td><td><button type="button" disabled={busy} className="secondary" title={'Editar ' + u.display_name} aria-label={'Editar perfil de ' + u.display_name} onClick={() => editUser(u)}>✎</button>{' '}<button disabled={busy} className="secondary" onClick={() => void toggle(u)} aria-label={(u.active ? 'Desactivar ' : 'Activar ') + u.display_name}>{u.active ? 'Desactivar' : 'Activar'}</button></td></tr>)}</tbody></table></div>{directory.items.length === 0 && <p>No hay usuarios que coincidan.</p>}
      <div className="pagination"><span>{directory.total} usuarios · Página {directory.page + 1}</span><button className="secondary" disabled={directory.page === 0} onClick={() => setFilter(f => ({ ...f, page: f.page - 1 }))}>Usuarios anteriores</button><button className="secondary" disabled={(directory.page + 1) * directory.size >= directory.total} onClick={() => setFilter(f => ({ ...f, page: f.page + 1 }))}>Usuarios siguientes</button></div></>}
    <label>Perfil de grupo<select aria-label="Perfil de grupo" value={group?.id ?? ''} disabled={busy} onChange={e => { const selected = groups.find(g => g.id === e.target.value); if (selected) editGroup(selected); else if (canLeave()) { setGroup(null); setUser(null) } }}><option value="">Seleccione</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}{!g.active && ' · Inactivo'}</option>)}</select></label>
    {(user || group) && <form onSubmit={e => { e.preventDefault(); void save() }}><fieldset disabled={busy}><legend>Editar perfil</legend>
      {user && <><label>Nombre actualizado del usuario<input required maxLength={160} value={user.display_name} onChange={e => setUser({ ...user, display_name: e.target.value })} /></label><p>Correo de acceso: {user.email}</p><label>Permiso actualizado del sistema<select value={user.system_role} onChange={e => setUser({ ...user, system_role: e.target.value })}><option value="USER">Usuario</option><option value="ADMIN">Administrador</option></select></label><p>Cambiar permisos revoca las sesiones de esa cuenta. Debe conservarse al menos un administrador activo.</p></>}
      {group && <><label>Nombre actualizado del grupo<input required maxLength={160} value={group.name} onChange={e => setGroup({ ...group, name: e.target.value })} /></label><label>Tipo actualizado del grupo<select value={group.group_type} onChange={e => setGroup({ ...group, group_type: e.target.value })}><option value="COMMISSION">Comisión</option><option value="UNIT">Unidad</option><option value="CLUB">Club</option><option value="OTHER">Otro</option></select></label><label className="check"><input type="checkbox" checked={group.active} onChange={e => setGroup({ ...group, active: e.target.checked })} />Grupo activo</label><p>Desactivar oculta el grupo y bloquea el acceso de sus integrantes a los documentos. Conserva documentos y pertenencias.</p></>}
      <button disabled={!dirty}>Guardar perfil</button>{' '}<button type="button" className="secondary" onClick={() => { if (canLeave()) { setUser(null); setGroup(null) } }}>Cerrar edición</button>{' '}<button type="button" className="secondary" onClick={() => void reloadProfile()}>Recargar perfil</button>
    </fieldset></form>}
  </section>
}
