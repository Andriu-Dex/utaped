import { useEffect, useState, type FormEvent } from 'react'
import { ActionForm } from '../shared/ActionForm'
import { api, type Period } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'

type Position = { id: string; code: string; name: string; career_specific: boolean; active: boolean; row_version: number }
type Assignment = { id: string; position_id: string; career_id: string | null; user_id: string; position_name: string; career_name: string | null; user_name: string; user_active: boolean }
type Career = { id: string; code: string; name: string; active: boolean }
type Candidate = { id: string; display_name: string; username: string | null }

export function PositionAdministration({ periods, onDirtyChange }: { periods: Period[]; onDirtyChange: (dirty: boolean) => void }) {
  const [positions, setPositions] = useState<Position[]>([]), [careers, setCareers] = useState<Career[]>([]), [users, setUsers] = useState<Candidate[]>([])
  const [period, setPeriod] = useState(''), [assignments, setAssignments] = useState<Assignment[]>([]), [positionId, setPositionId] = useState('')
  const [busy, setBusy] = useState(false), [dirty, setDirty] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState(''), [reload, setReload] = useState(0)
  const position = positions.find(p => p.id === positionId)
  const writable = periods.find(p => p.id === period)?.status === 'ACTIVE'
  useEffect(() => { onDirtyChange(dirty); return () => onDirtyChange(false) }, [dirty, onDirtyChange])
  useEffect(() => { let active = true; Promise.all([api<Position[]>('/admin/positions'), api<Career[]>('/careers'), api<{ items: Candidate[] }>('/admin/users/directory?size=100&active=true')]).then(([p, c, u]) => { if (active) { setPositions(p); setCareers(c.filter(x => x.active)); setUsers(u.items.map(x => ({ id: x.id, display_name: x.display_name, username: x.username }))) } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [reload])
  useEffect(() => { let active = true; if (period) api<Assignment[]>('/admin/periods/' + period + '/position-assignments').then(a => { if (active) setAssignments(a) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [period, reload])
  async function run(action: () => Promise<unknown>, message: string, form?: HTMLFormElement) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); form?.reset(); setDirty(false); setNotice(message); setReload(r => r + 1) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo completar la operación.') } finally { setBusy(false) }
  }
  function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = event.currentTarget, data = new FormData(form)
    void run(() => api('/admin/positions', 'POST', { code: String(data.get('code')).trim().toUpperCase(), name: data.get('name'), careerSpecific: data.get('careerSpecific') === 'on' }), 'Cargo registrado.', form)
  }
  function assign(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = event.currentTarget, data = new FormData(form)
    void run(() => api('/admin/periods/' + period + '/position-assignments', 'PUT', { positionId, careerId: position?.career_specific ? data.get('careerId') : null, userId: data.get('userId') }), 'Asignación guardada. Los flujos ya activados conservan sus participantes.', form)
  }
  async function importCsv(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = event.currentTarget, file = new FormData(form).get('file')
    if (!(file instanceof File) || !file.size) return
    const csv = await file.text()
    setBusy(true); setError(''); setNotice('')
    try {
      const result = await api<{ assigned: number; errors: string[] }>('/admin/periods/' + period + '/position-assignments/import', 'POST', { csv })
      if (result.errors.length) setError(result.errors.join(' '))
      else { setNotice(result.assigned + ' asignaciones importadas.'); form.reset(); setDirty(false); setReload(r => r + 1) }
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo importar el archivo.') } finally { setBusy(false) }
  }
  return <section className="card">
    <div className="section-heading"><span className="section-icon"><Icon name="users" /></span><div><span className="section-kicker">FLUJOS POR CARGO</span><h2>Cargos institucionales</h2></div></div>
    <p className="muted">Defina los cargos que revisan o aprueban (UPE, coordinación, subdecanato, decanato) y quién los ocupa en cada período. Las etapas de flujo «Cargo institucional» se resuelven con estas asignaciones al activar el período.</p>
    {error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    <table><thead><tr><th>Cargo</th><th>Código</th><th>Alcance</th><th>Estado</th><th><span className="visually-hidden">Acciones</span></th></tr></thead><tbody>{positions.map(p => <tr key={p.id}><td>{p.name}</td><td><code>{p.code}</code></td><td>{p.career_specific ? 'Por carrera' : 'Facultad'}</td><td>{p.active ? 'Activo' : 'Inactivo'}</td><td><button className="link" disabled={busy} onClick={async () => { if (await confirmAction((p.active ? '¿Desactivar' : '¿Activar') + ' el cargo ' + p.name + '?')) void run(() => api('/admin/positions/' + p.id, 'PUT', { name: p.name, active: !p.active, rowVersion: p.row_version }), 'Cargo actualizado.') }}>{p.active ? 'Desactivar' : 'Activar'}</button></td></tr>)}</tbody></table>
    {!positions.length && <p className="muted">No hay cargos registrados.</p>}
    <ActionForm label="Registrar cargo" disabled={busy} dirty={dirty} onCancel={() => setDirty(false)}><form onChange={() => setDirty(true)} onSubmit={create}><fieldset disabled={busy}><div className="grid"><label>Nombre del cargo<input name="name" required maxLength={160} /></label><label>Código<input name="code" required maxLength={40} pattern="[A-Za-z0-9_]+" placeholder="SUBDEAN" /></label></div><label className="check"><input type="checkbox" name="careerSpecific" defaultChecked />Se asigna por carrera</label><button>Guardar cargo</button></fieldset></form></ActionForm>
    <label>Período<select value={period} disabled={busy} onChange={async e => { const next = e.currentTarget.value; if (dirty && !(await confirmAction('Hay cambios sin guardar. ¿Cambiar de período?'))) return; setDirty(false); setPeriod(next); setAssignments([]) }}><option value="">Seleccione un período</option>{periods.map(p => <option key={p.id} value={p.id}>{p.name}{p.status === 'CLOSED' ? ' · Cerrado' : ''}</option>)}</select></label>
    {period && <>
      <table><thead><tr><th>Cargo</th><th>Carrera</th><th>Persona</th><th><span className="visually-hidden">Acciones</span></th></tr></thead><tbody>{assignments.map(a => <tr key={a.id}><td>{a.position_name}</td><td>{a.career_name ?? 'Facultad'}</td><td>{a.user_name}{!a.user_active && ' (inactiva)'}</td><td>{writable && <button className="link" disabled={busy} onClick={async () => { if (await confirmAction('¿Quitar a ' + a.user_name + ' del cargo ' + a.position_name + '?')) void run(() => api('/admin/periods/' + period + '/position-assignments/' + a.id, 'DELETE'), 'Asignación retirada.') }}>Quitar</button>}</td></tr>)}</tbody></table>
      {!assignments.length && <p className="muted">No hay cargos asignados en este período.</p>}
      {writable && <ActionForm label="Asignar cargo" disabled={busy} dirty={dirty} onCancel={() => { setDirty(false); setPositionId('') }}><form onChange={() => setDirty(true)} onSubmit={assign}><fieldset disabled={busy}><div className="grid"><label>Cargo<select required value={positionId} onChange={e => setPositionId(e.currentTarget.value)}><option value="">Seleccione un cargo</option>{positions.filter(p => p.active).map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label>{position?.career_specific && <label>Carrera<select name="careerId" required><option value="">Seleccione una carrera</option>{careers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>}<label>Persona<select name="userId" required><option value="">Seleccione una persona</option>{users.map(u => <option key={u.id} value={u.id}>{u.display_name}{u.username ? ' · ' + u.username : ''}</option>)}</select></label></div><button>Guardar asignación</button></fieldset></form></ActionForm>}
      {writable && <ActionForm label="Importar asignaciones (CSV)" disabled={busy} dirty={dirty} onCancel={() => setDirty(false)}><form onChange={() => setDirty(true)} onSubmit={importCsv}><fieldset disabled={busy}><label>Archivo CSV<input type="file" name="file" accept=".csv,text/csv" required /></label><p className="field-help">Columnas: <code>cargo,carrera,usuario</code>. El cargo y la carrera usan su código; deje la carrera vacía para cargos de facultad. El usuario es el identificador o el correo. Si una fila falla, no se aplica ninguna.</p><button>Importar</button></fieldset></form></ActionForm>}
    </>}
  </section>
}
