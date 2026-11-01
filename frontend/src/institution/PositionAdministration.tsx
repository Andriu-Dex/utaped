import { useEffect, useState, type FormEvent } from 'react'
import { ActionForm } from '../shared/ActionForm'
import { api, type Period } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'

type Position = { id: string; code: string; name: string; career_specific: boolean; active: boolean; row_version: number; can_view_history: boolean }
type Assignment = { id: string; position_id: string; career_id: string | null; user_id: string; position_name: string; career_name: string | null; user_name: string; user_active: boolean }
type Career = { id: string; code: string; name: string; active: boolean }
type Candidate = { id: string; display_name: string; username: string | null }

export function PositionAdministration({ periods, onDirtyChange }: { periods: Period[]; onDirtyChange: (dirty: boolean) => void }) {
  const [positions, setPositions] = useState<Position[]>([]), [careers, setCareers] = useState<Career[]>([]), [users, setUsers] = useState<Candidate[]>([])
  const [period, setPeriod] = useState(''), [assignments, setAssignments] = useState<Assignment[]>([]), [positionId, setPositionId] = useState('')
  const [busy, setBusy] = useState(false), [dirty, setDirty] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState(''), [reload, setReload] = useState(0)
  const [loaded, setLoaded] = useState(false), [loadFailed, setLoadFailed] = useState(false)
  const position = positions.find(p => p.id === positionId)
  const writable = periods.find(p => p.id === period)?.status === 'ACTIVE'
  useEffect(() => { onDirtyChange(dirty); return () => onDirtyChange(false) }, [dirty, onDirtyChange])
  useEffect(() => { let active = true; Promise.all([api<Position[]>('/admin/positions'), api<Career[]>('/careers'), api<{ items: Candidate[] }>('/admin/users/directory?size=100&active=true')]).then(([p, c, u]) => { if (active) { setPositions(p); setCareers(c.filter(x => x.active)); setUsers(u.items.map(x => ({ id: x.id, display_name: x.display_name, username: x.username }))); setLoaded(true); setLoadFailed(false); setError('') } }).catch(e => { if (active) { setError(e.message); setLoadFailed(true) } }); return () => { active = false } }, [reload])
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
  const retry = <button type="button" className="secondary" disabled={busy || dirty} onClick={() => { setError(''); setReload(value => value + 1) }}>Reintentar cargos institucionales</button>
  if (!loaded) return <section className="card"><h2>Cargos institucionales</h2>{error ? <><Toast message={error} />{retry}</> : <p role="status">Cargando cargos institucionales…</p>}</section>
  return <div className="positions-workspace">
    {error && <Toast message={error} onDismiss={() => setError('')} />}
    {loadFailed && retry}
    {notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}

    <section className="card positions-card animate-reveal">
      <div className="section-heading">
        <span className="section-icon indigo"><Icon name="users" /></span>
        <div>
          <span className="section-kicker">FLUJOS POR CARGO</span>
          <h2>Cargos institucionales</h2>
          <p className="muted" style={{ margin: '4px 0 0' }}>
            Defina los cargos que revisan o aprueban (UPE, coordinación, subdecanato, decanato) y quién los ocupa en cada período. Las etapas de flujo «Cargo institucional» se resuelven con estas asignaciones al activar el período.
          </p>
        </div>
      </div>

      <div className="table-scroll" style={{ marginTop: 16 }}>
        <table>
          <thead>
            <tr>
              <th>Cargo</th>
              <th>Código</th>
              <th>Alcance</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {positions.map(p => (
              <tr key={p.id}>
                <td><strong>{p.name}</strong></td>
                <td><code className="position-code-badge">{p.code}</code></td>
                <td><span className="scope-tag">{p.career_specific ? 'Por carrera' : 'Facultad'}</span></td>
                <td><span className={'status-badge ' + (p.active ? 'active' : 'inactive')}>{p.active ? 'Activo' : 'Inactivo'}</span></td>
                <td className="row-actions">
                  <button
                    type="button"
                    className="secondary"
                    disabled={busy}
                    title={(p.active ? 'Desactivar ' : 'Activar ') + p.name}
                    aria-label={(p.active ? 'Desactivar ' : 'Activar ') + p.name}
                    onClick={async () => {
                      if (await confirmAction((p.active ? '¿Desactivar' : '¿Activar') + ' el cargo ' + p.name + '?')) {
                        void run(() => api('/admin/positions/' + p.id, 'PUT', { name: p.name, active: !p.active, rowVersion: p.row_version }), 'Cargo actualizado.')
                      }
                    }}
                  >
                    {p.active ? 'Desactivar' : 'Activar'}
                  </button>{' '}
                  <button
                    type="button"
                    className="secondary"
                    disabled={busy}
                    title={(p.can_view_history ? 'Retirar' : 'Conceder') + ' la consulta de períodos anteriores al titular vigente de ' + p.name}
                    aria-label={(p.can_view_history ? 'Retirar' : 'Conceder') + ' la consulta de períodos anteriores al titular vigente de ' + p.name}
                    onClick={async () => {
                      if (await confirmAction((p.can_view_history ? '¿Retirar' : '¿Conceder') + ' la consulta de períodos anteriores al titular vigente de ' + p.name + '?')) {
                        void run(() => api('/admin/positions/' + p.id + '/history-permission', 'PUT', { enabled: !p.can_view_history, rowVersion: p.row_version }), 'Permiso histórico actualizado.')
                      }
                    }}
                  >
                    <Icon name="eye" size={14} />
                    {p.can_view_history ? 'Retirar historial' : 'Autorizar historial'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!positions.length && <p className="muted" style={{ margin: '14px 0' }}>No hay cargos registrados.</p>}

      <ActionForm label="Registrar cargo" disabled={busy} dirty={dirty} onCancel={() => setDirty(false)}>
        <form onChange={() => setDirty(true)} onSubmit={create} style={{ marginTop: 16 }}>
          <fieldset disabled={busy}>
            <div className="grid">
              <label>Nombre del cargo<input name="name" required maxLength={160} placeholder="Ej: Responsable de la Unidad de Planificación" /></label>
              <label>Código<input name="code" required maxLength={40} pattern="[A-Za-z0-9_]+" placeholder="SUBDEAN" /></label>
            </div>
            <label className="check" style={{ margin: '12px 0 16px' }}>
              <input type="checkbox" name="careerSpecific" defaultChecked />
              Se asigna por carrera (desmarcar para cargos a nivel de Facultad)
            </label>
            <button disabled={busy}><Icon name="check" size={16} />Guardar cargo</button>
          </fieldset>
        </form>
      </ActionForm>
    </section>

    <section className="card positions-assignments-card" style={{ marginTop: 24 }}>
      <div className="section-heading">
        <span className="section-icon amber"><Icon name="calendar" /></span>
        <div>
          <span className="section-kicker">ASIGNACIÓN POR CICLO</span>
          <h3>Asignaciones de cargos por período</h3>
          <p className="muted" style={{ margin: '4px 0 0' }}>
            Seleccione un período académico para consultar y gestionar los titulares de cada cargo institucional.
          </p>
        </div>
      </div>

      <div className="filter-toolbar" style={{ margin: '16px 0' }}>
        <label style={{ maxWidth: 380 }}>
          Período
          <select
            value={period}
            disabled={busy}
            onChange={async e => {
              const next = e.currentTarget.value
              if (dirty && !(await confirmAction('Hay cambios sin guardar. ¿Cambiar de período?'))) return
              setDirty(false)
              setPeriod(next)
              setAssignments([])
            }}
          >
            <option value="">Seleccione un período</option>
            {periods.map(p => (
              <option key={p.id} value={p.id}>
                {p.name}{p.status === 'CLOSED' ? ' · Cerrado' : ''}
              </option>
            ))}
          </select>
        </label>
      </div>

      {!period ? (
        <div className="empty-state" style={{ margin: '20px 0 8px' }}>
          <Icon name="calendar" size={32} />
          <p>Seleccione un período para ver y administrar las asignaciones de cargos.</p>
        </div>
      ) : (
        <>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Cargo</th>
                  <th>Carrera</th>
                  <th>Persona</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {assignments.map(a => (
                  <tr key={a.id}>
                    <td><strong>{a.position_name}</strong></td>
                    <td><span className="scope-tag">{a.career_name ?? 'Facultad'}</span></td>
                    <td>
                      <span className="user-avatar-badge" aria-hidden="true">{a.user_name.charAt(0).toUpperCase()}</span>
                      <span>{a.user_name}</span>
                      {!a.user_active && <span className="status-badge inactive" style={{ marginLeft: 8 }}>Inactiva</span>}
                    </td>
                    <td className="row-actions">
                      {writable && (
                        <button
                          type="button"
                          className="secondary danger-text"
                          disabled={busy}
                          title={'Quitar a ' + a.user_name + ' del cargo ' + a.position_name}
                          aria-label={'Quitar a ' + a.user_name + ' del cargo ' + a.position_name}
                          onClick={async () => {
                            if (await confirmAction('¿Quitar a ' + a.user_name + ' del cargo ' + a.position_name + '?')) {
                              void run(() => api('/admin/periods/' + period + '/position-assignments/' + a.id, 'DELETE'), 'Asignación retirada.')
                            }
                          }}
                        >
                          <Icon name="x" size={14} />Quitar
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!assignments.length && <p className="muted" style={{ margin: '14px 0' }}>No hay cargos asignados en este período.</p>}

          {writable && (
            <div className="positions-actions-stack" style={{ marginTop: 20 }}>
              <ActionForm
                label="Asignar cargo"
                disabled={busy}
                dirty={dirty}
                onCancel={() => {
                  setDirty(false)
                  setPositionId('')
                }}
              >
                <form onChange={() => setDirty(true)} onSubmit={assign} style={{ marginTop: 16 }}>
                  <fieldset disabled={busy}>
                    <div className="grid">
                      <label>
                        Cargo
                        <select required value={positionId} onChange={e => setPositionId(e.currentTarget.value)}>
                          <option value="">Seleccione un cargo</option>
                          {positions.filter(p => p.active).map(p => (
                            <option key={p.id} value={p.id}>{p.name}</option>
                          ))}
                        </select>
                      </label>
                      {position?.career_specific && (
                        <label>
                          Carrera
                          <select name="careerId" required>
                            <option value="">Seleccione una carrera</option>
                            {careers.map(c => (
                              <option key={c.id} value={c.id}>{c.name}</option>
                            ))}
                          </select>
                        </label>
                      )}
                      <label>
                        Persona
                        <select name="userId" required>
                          <option value="">Seleccione una persona</option>
                          {users.map(u => (
                            <option key={u.id} value={u.id}>
                              {u.display_name}{u.username ? ' · ' + u.username : ''}
                            </option>
                          ))}
                        </select>
                      </label>
                    </div>
                    <button disabled={busy} style={{ marginTop: 12 }}>
                      <Icon name="check" size={16} />Guardar asignación
                    </button>
                  </fieldset>
                </form>
              </ActionForm>

              <ActionForm
                label="Importar asignaciones (CSV)"
                disabled={busy}
                dirty={dirty}
                onCancel={() => setDirty(false)}
              >
                <form onChange={() => setDirty(true)} onSubmit={importCsv} style={{ marginTop: 16 }}>
                  <fieldset disabled={busy}>
                    <label>
                      Archivo CSV
                      <input type="file" name="file" accept=".csv,text/csv" required />
                    </label>
                    <p className="field-help">
                      Columnas: <code>cargo,carrera,usuario</code>. El cargo y la carrera usan su código; deje la carrera vacía para cargos de facultad. El usuario es el identificador o el correo. Si una fila falla, no se aplica ninguna.
                    </p>
                    <button disabled={busy} style={{ marginTop: 12 }}>
                      <Icon name="document" size={16} />Importar
                    </button>
                  </fieldset>
                </form>
              </ActionForm>
            </div>
          )}
        </>
      )}
    </section>
  </div>
}
