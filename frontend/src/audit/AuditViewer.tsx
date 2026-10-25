import { auditActionLabels } from './auditLabels'
import { Toast } from '../shared/Toast'
import { useEffect, useState, type FormEvent } from 'react'
import { api, download } from '../shared/api'
import { Icon } from '../shared/Icon'
import './audit.css'

interface AuditEvent { id: string; actorId: string | null; actorName: string | null; historicalActorName: boolean; action: string; targetId: string | null; subjectUserId: string | null; occurredAt: string }
interface AuditPage { items: AuditEvent[]; total: number; page: number; size: number }

const empty = { action: '', actorId: '', targetId: '', from: '', to: '', page: 0 }
export function AuditViewer({ targetDocId, onBack }: { targetDocId?: string; onBack?: () => void }) {
  const [exporting, setExporting] = useState(false)
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
  async function exportCsv() {
    setExporting(true); setError('');const params = new URLSearchParams()
    for (const key of ['action', 'actorId', 'targetId'] as const) if (filter[key]) params.set(key, filter[key])
    for (const key of ['from', 'to'] as const) if (filter[key]) params.set(key, new Date(filter[key]).toISOString())
    try { await download('/admin/audit-events/export?' + params, 'utaped-auditoria.csv') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo exportar.') } finally { setExporting(false) }
  }
  return <section className="card" aria-label={targetDocId ? 'Historial del documento' : 'Auditoría administrativa'}>
    {onBack && <button className="secondary" onClick={onBack}>Volver al documento</button>}
    <div className="section-heading">
      <div className="section-heading-icon" aria-hidden="true"><Icon name="eye" size={24} /></div>
      <div>
        <h2>{targetDocId ? 'Historial del documento' : 'Auditoría administrativa'}</h2>
        <p className="muted">Registro de acciones realizadas. No representa firmas, aprobaciones ni una nueva versión formal del documento.</p>
      </div>
    </div>
    {!targetDocId && <form className="audit-filter-panel" onSubmit={submit}>
      <div className="grid">
        <label>Acción registrada<select aria-label="Acción registrada" value={draft.action} onChange={e => setDraft({ ...draft, action: e.target.value })}><option value="">Todas</option>{actions.map(action => <option key={action} value={action}>{auditActionLabels[action] ?? action}</option>)}</select></label>
        {(['actorId', 'targetId'] as const).map(key => <label key={key}>{key === 'actorId' ? 'ID del actor' : 'ID del objeto'}<input value={draft[key]} placeholder="UUID" pattern="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" onChange={e => setDraft({ ...draft, [key]: e.target.value })} /></label>)}
        <label>Desde (hora local)<input type="datetime-local" value={draft.from} onChange={e => setDraft({ ...draft, from: e.target.value })} /></label>
        <label>Hasta (hora local)<input type="datetime-local" value={draft.to} onChange={e => setDraft({ ...draft, to: e.target.value })} /></label>
      </div>
      <div className="audit-action-buttons">
        <button disabled={busy}>Filtrar auditoría</button>
        <button type="button" className="secondary" onClick={() => { setDraft(empty); setFilter(empty); setReload(n => n + 1) }}>Limpiar filtros</button>
      </div>
    </form>}
    <div className="audit-toolbar">
      <button type="button" className="secondary" disabled={busy} onClick={() => setReload(n => n + 1)}>Actualizar historial</button>
    </div>
    {!targetDocId && <div className="audit-export-bar">
      <p style={{ margin: 0, fontSize: '0.88rem' }}>La exportación incluye los resultados de los filtros aplicados, más allá de la página visible. Los valores con apariencia de fórmula se identifican como texto.</p>
      <button type="button" className="secondary" disabled={busy || exporting || !result?.total} onClick={() => void exportCsv()}>{exporting ? 'Exportando…' : 'Exportar CSV filtrado'}</button>
    </div>}
    {error && <Toast message={error} />}{busy && <p role="status">Consultando registro…</p>}
    {result && !busy && <>
      <p>{result.total} eventos · Página {result.page + 1}</p>
      <div className="table-scroll">
        <table className={targetDocId ? 'history-table' : undefined}>
          <thead><tr><th>Fecha y hora local</th><th>Acción</th><th>Actor</th>{!targetDocId && <><th>Objeto</th><th>Usuario afectado</th></>}</tr></thead>
          <tbody>{result.items.map(event => <tr key={event.id}>
            <td data-label="Fecha y hora"><time dateTime={event.occurredAt}>{new Date(event.occurredAt).toLocaleString('es-EC')}</time></td>
            <td data-label="Acción"><span className="audit-action-chip">{auditActionLabels[event.action] ?? event.action}</span></td>
            <td data-label="Actor"><div className="audit-actor-cell"><span className="audit-actor-name">{event.actorName ?? 'Sistema'}</span>{event.actorName && !event.historicalActorName && <small className="muted">· Nombre actual; evento anterior al registro de nombres</small>}{!targetDocId && event.actorId && <small className="audit-id">{event.actorId}</small>}</div></td>
            {!targetDocId && <><td className="audit-id">{event.targetId ?? '—'}</td><td className="audit-id">{event.subjectUserId ?? '—'}</td></>}
          </tr>)}</tbody>
        </table>
      </div>
      {result.items.length === 0 && <p>No hay eventos que coincidan.</p>}
      <div className="pagination">
        <button className="secondary" disabled={result.page === 0} onClick={() => setFilter(f => ({ ...f, page: f.page - 1 }))}>Eventos anteriores</button>
        <button className="secondary" disabled={(result.page + 1) * result.size >= result.total} onClick={() => setFilter(f => ({ ...f, page: f.page + 1 }))}>Eventos siguientes</button>
      </div>
    </>}
  </section>
}
