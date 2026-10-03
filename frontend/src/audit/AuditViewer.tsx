import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../shared/api'

interface AuditEvent { id: string; actorId: string | null; actorName: string | null; historicalActorName: boolean; action: string; targetId: string | null; subjectUserId: string | null; occurredAt: string }
interface AuditPage { items: AuditEvent[]; total: number; page: number; size: number }
const labels: Record<string, string> = {
  WORK_PLAN_CREATED: 'Plan creado', WORK_PLAN_DRAFT_SAVED: 'Borrador guardado', WORK_PLAN_MATRIX_SAVED: 'Matriz guardada', T1_PREVIEW_GENERATED: 'Previsualización T1 generada',
  DOCUMENT_ATTACHMENT_SETTINGS_SAVED: 'Opciones de anexos guardadas', DOCUMENT_ATTACHMENT_ADDED: 'Anexo agregado', DOCUMENT_ATTACHMENT_UPDATED: 'Anexo actualizado', DOCUMENT_ATTACHMENT_REMOVED: 'Anexo retirado', DOCUMENT_ATTACHMENTS_REORDERED: 'Anexos reordenados',
  USER_PROFILE_UPDATED: 'Perfil de usuario actualizado', USER_CREATED: 'Usuario creado', USER_STATUS_CHANGED: 'Acceso de usuario actualizado', GROUP_PROFILE_UPDATED: 'Perfil de grupo actualizado', GROUP_CREATED: 'Grupo creado', MEMBERSHIP_ASSIGNED: 'Integrante asignado', MEMBERSHIP_REMOVED: 'Pertenencia retirada', PERIOD_CREATED: 'Período creado',
  WORKFLOW_DRAFT_SAVED: 'Borrador de flujo guardado', WORKFLOW_REVISION_CONFIGURED: 'Revisión de flujo configurada', WORKFLOW_CONFIGURATION_DISABLED: 'Configuración de flujo deshabilitada',
  LOGIN_SUCCEEDED: 'Sesión iniciada', LOGOUT_SUCCEEDED: 'Sesión cerrada', ADMIN_BOOTSTRAPPED: 'Administrador inicial creado', PASSWORD_CHANGED: 'Contraseña cambiada', PASSWORD_RESET: 'Contraseña restablecida', PASSWORD_RESET_DELIVERY_FAILED: 'Correo de recuperación no entregado',
  PLANNING_CATALOG_SAVED: 'Catálogo guardado', ACTIVITY_CATALOG_SAVED: 'Actividad de catálogo guardada', GROUP_COLLECTIVE_LABEL_SAVED: 'Denominación colectiva guardada', HOLIDAY_SAVED: 'Feriado guardado', HOLIDAY_REMOVED: 'Feriado retirado', PERIOD_HOLIDAY_POLICY_SAVED: 'Restricción de feriados guardada',
}
const empty = { action: '', actorId: '', targetId: '', from: '', to: '', page: 0 }
export function AuditViewer({ targetDocId, onBack }: { targetDocId?: string; onBack?: () => void }) {
  const [draft, setDraft] = useState(empty); const [filter, setFilter] = useState(empty)
  const [actions, setActions] = useState<string[]>([]); const [result, setResult] = useState<AuditPage | null>(null); const [error, setError] = useState(''); const [busy, setBusy] = useState(false); const [reload, setReload] = useState(0)
  useEffect(() => { let active = true; if (!targetDocId) api<string[]>('/admin/audit-events/actions').then(data => { if (active) setActions(data) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [targetDocId, reload])
  useEffect(() => {
    let active = true; const params = new URLSearchParams({ page: String(filter.page) })
    if (!targetDocId) { for (const key of ['action', 'actorId', 'targetId'] as const) if (filter[key]) params.set(key, filter[key]); for (const key of ['from', 'to'] as const) if (filter[key]) params.set(key, new Date(filter[key]).toISOString()) }
    Promise.resolve().then(() => { if (active) { setBusy(true); setError('') } })
    api<AuditPage>((targetDocId ? '/work-plans/' + targetDocId + '/history' : '/admin/audit-events') + '?' + params).then(data => { if (active) setResult(data) }).catch(e => { if (active) { setError(e.message); setResult(null) } }).finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [targetDocId, filter, reload])
  function submit(e: FormEvent) { e.preventDefault(); if (draft.from && draft.to && draft.from > draft.to) { setError('El inicio debe preceder al fin del intervalo.'); return } setFilter({ ...draft, page: 0 }) }
  return <section className="card" aria-label={targetDocId ? 'Historial del Plan' : 'Auditoría administrativa'}>
    {onBack && <button className="secondary" onClick={onBack}>Volver al Plan</button>}
    <h2>{targetDocId ? 'Historial del Plan' : 'Auditoría administrativa'}</h2>
    <p>Registro de acciones realizadas. No representa firmas, aprobaciones ni una nueva versión formal del documento.</p>
    {!targetDocId && <form className="grid" onSubmit={submit}><label>Acción registrada<select aria-label="Acción registrada" value={draft.action} onChange={e => setDraft({ ...draft, action: e.target.value })}><option value="">Todas</option>{actions.map(action => <option key={action} value={action}>{labels[action] ?? action}</option>)}</select></label>
      {(['actorId', 'targetId'] as const).map(key => <label key={key}>{key === 'actorId' ? 'ID del actor' : 'ID del objeto'}<input value={draft[key]} placeholder="UUID" pattern="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" onChange={e => setDraft({ ...draft, [key]: e.target.value })} /></label>)}
      <label>Desde (hora local)<input type="datetime-local" value={draft.from} onChange={e => setDraft({ ...draft, from: e.target.value })} /></label><label>Hasta (hora local)<input type="datetime-local" value={draft.to} onChange={e => setDraft({ ...draft, to: e.target.value })} /></label><button disabled={busy}>Filtrar auditoría</button><button type="button" className="secondary" onClick={() => { setDraft(empty); setFilter(empty); setReload(n => n + 1) }}>Limpiar filtros</button></form>}
    <button type="button" className="secondary" disabled={busy} onClick={() => setReload(n => n + 1)}>Actualizar historial</button>
    {error && <p role="alert" className="error">{error}</p>}{busy && <p role="status">Consultando registro…</p>}
    {result && !busy && <><p>{result.total} eventos · Página {result.page + 1}</p><div className="table-scroll"><table className={targetDocId ? 'history-table' : undefined}><thead><tr><th>Fecha y hora local</th><th>Acción</th><th>Actor</th>{!targetDocId && <><th>Objeto</th><th>Usuario afectado</th></>}</tr></thead><tbody>{result.items.map(event => <tr key={event.id}><td data-label="Fecha y hora"><time dateTime={event.occurredAt}>{new Date(event.occurredAt).toLocaleString('es-EC')}</time></td><td data-label="Acción">{labels[event.action] ?? event.action}</td><td data-label="Actor">{event.actorName ?? 'Sistema'}{event.actorName && !event.historicalActorName && <small> · Nombre actual; evento anterior al registro de nombres</small>}{!targetDocId && event.actorId && <small className="audit-id">{event.actorId}</small>}</td>{!targetDocId && <><td className="audit-id">{event.targetId ?? '—'}</td><td className="audit-id">{event.subjectUserId ?? '—'}</td></>}</tr>)}</tbody></table></div>{result.items.length === 0 && <p>No hay eventos que coincidan.</p>}
      <div className="pagination"><button className="secondary" disabled={result.page === 0} onClick={() => setFilter(f => ({ ...f, page: f.page - 1 }))}>Eventos anteriores</button><button className="secondary" disabled={(result.page + 1) * result.size >= result.total} onClick={() => setFilter(f => ({ ...f, page: f.page + 1 }))}>Eventos siguientes</button></div></>}
  </section>
}
