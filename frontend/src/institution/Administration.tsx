import { useEffect, useState, type FormEvent } from 'react'
import { api, type Group, type User, type Member } from '../shared/api'

export function Administration({ onChange }: { onChange: () => Promise<void> }) {
  const [users, setUsers] = useState<User[]>([]); const [groups, setGroups] = useState<Group[]>([])
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const [memberGroup, setMemberGroup] = useState(''); const [members, setMembers] = useState<Member[]>([])
  useEffect(() => {
    let cancelled = false
    if (memberGroup) api<Member[]>('/groups/' + memberGroup + '/members').then(data => { if (!cancelled) setMembers(data) }).catch(e => setError(e.message))
    return () => { cancelled = true }
  }, [memberGroup])
  async function remove(member: Member) {
    setBusy(true); setError('')
    try { await api('/admin/groups/' + memberGroup + '/members/' + member.id, 'DELETE'); setMembers(await api<Member[]>('/groups/' + memberGroup + '/members')); setNotice('Pertenencia retirada.'); await onChange() }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo retirar la pertenencia.') } finally { setBusy(false) }
  }
  async function load() { const [u, g] = await Promise.all([api<User[]>('/admin/users'), api<Group[]>('/groups')]); setUsers(u); setGroups(g) }
  useEffect(() => { Promise.resolve().then(load).catch(e => setError(e.message)) }, [])
  async function submit(event: FormEvent<HTMLFormElement>, path: string) {
    event.preventDefault(); const form = event.currentTarget; setError(''); setNotice(''); setBusy(true)
    const data = Object.fromEntries(new FormData(form))
    try {
      const target = path === 'membership' ? '/admin/groups/' + data.groupId + '/members' : path
      await api(target, 'POST', data); form.reset(); setNotice('Cambios guardados.'); await load(); await onChange()
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.') } finally { setBusy(false) }
  }
  async function toggle(user: User) {
    setError(''); setBusy(true)
    try { await api('/admin/users/' + user.id + '/active', 'PATCH', { active: !user.active }); await load() }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo actualizar.') } finally { setBusy(false) }
  }
  return <section><h2>Administración institucional</h2>{error && <p className="error" role="alert">{error}</p>}{notice && <p className="notice" role="status">{notice}</p>}
    <div className="grid"><section className="card"><h3>Crear usuario</h3><form onSubmit={e => submit(e, '/admin/users')}>
      <label>Nombre<input name="displayName" required maxLength={160} /></label><label>Correo<input type="email" name="email" required maxLength={254} /></label>
      <label>Permiso del sistema<select name="systemRole"><option value="USER">Usuario</option><option value="ADMIN">Administrador</option></select></label>
      <label>Contraseña temporal<input type="password" name="temporaryPassword" required minLength={12} maxLength={72} autoComplete="new-password" /></label>
      <button disabled={busy}>Crear usuario</button></form></section>
    <section className="card"><h3>Crear grupo</h3><form onSubmit={e => submit(e, '/admin/groups')}>
      <label>Nombre del grupo<input name="name" required maxLength={160} /></label><label>Tipo<select name="groupType"><option value="COMMISSION">Comisión</option><option value="UNIT">Unidad</option><option value="CLUB">Club</option><option value="OTHER">Otro</option></select></label>
      <button disabled={busy}>Crear grupo</button></form>
      <h3>Asignar integrante</h3><form onSubmit={e => submit(e, 'membership')}><label>Grupo<select aria-label="Grupo" required name="groupId"><option value="">Seleccione</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>
        <label>Usuario<select aria-label="Usuario" required name="userId"><option value="">Seleccione</option>{users.filter(u => u.active).map(u => <option key={u.id} value={u.id}>{u.display_name}</option>)}</select></label>
        <label>Rol de pertenencia<select name="membershipRole"><option value="MEMBER">Miembro</option><option value="COORDINATOR">Coordinador</option></select></label><button disabled={busy}>Asignar integrante</button></form>
    </section></div>
    <section className="card"><h3>Gestionar integrantes</h3><label>Grupo a gestionar<select aria-label="Grupo a gestionar" value={memberGroup} onChange={e => { setMemberGroup(e.target.value); setMembers([]) }}><option value="">Seleccione</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>{memberGroup && <ul>{members.map(m => <li key={m.id}>{m.display_name} <button className="secondary" disabled={busy} onClick={() => remove(m)} aria-label={'Retirar a ' + m.display_name}>Retirar pertenencia</button></li>)}</ul>}</section>
    <section className="card"><h3>Crear período</h3><form className="grid" onSubmit={e => submit(e, '/admin/periods')}><label>Nombre del período<input name="name" required maxLength={120} /></label>
      {([['startsOn','Inicio del período'],['endsOn','Fin del período'],['preparationStartsOn','Inicio de elaboración'],['preparationEndsOn','Fin de elaboración'],['reviewStartsOn','Inicio de revisión'],['reviewEndsOn','Fin de revisión']] as const).map(([name,label]) => <label key={name}>{label}<input required type="date" name={name} /></label>)}<button disabled={busy}>Crear período</button></form></section>
    <section className="card"><h3>Usuarios</h3><p>Lista inicial limitada a 200 usuarios.</p><div className="table-scroll"><table><thead><tr><th>Nombre</th><th>Correo</th><th>Acceso</th><th>Estado</th><th>Acción</th></tr></thead><tbody>{users.map(user => <tr key={user.id}><td>{user.display_name}</td><td>{user.email}</td><td>{user.system_role === 'ADMIN' ? 'Administrador' : 'Usuario'}</td><td>{user.active ? 'Activo' : 'Inactivo'}</td><td><button disabled={busy} className="secondary" onClick={() => toggle(user)}>{user.active ? 'Desactivar' : 'Activar'}</button></td></tr>)}</tbody></table></div></section>
  </section>
}
