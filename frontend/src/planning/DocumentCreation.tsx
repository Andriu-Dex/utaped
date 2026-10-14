import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import type { PlanOptions, WorkPlan } from './types'
import './document-creation.css'

type ReportOption = { id: string; title: string; groupName: string; periodName: string; formalVersion: string; suggestedTitle: string; reportId: string | null }
type ReportOptions = { items: ReportOption[]; total: number; page: number; size: number }
export function DocumentCreation({ options, busy, onBusyChange, onCreated, onDirtyChange }: { options: PlanOptions; busy: boolean; onBusyChange: (busy: boolean) => void; onCreated: (document: WorkPlan) => void; onDirtyChange: (dirty: boolean) => void }) {
  const [kind, setKind] = useState<'T1' | 'T2'>('T1')
  const [scopeId, setScopeId] = useState(''); const [titleOverride, setTitleOverride] = useState<string | null>(null)
  const [requestKey, setRequestKey] = useState(() => crypto.randomUUID())
  const [reportId, setReportId] = useState(''); const [candidates, setCandidates] = useState<ReportOptions | null>(null)
  const [search, setSearch] = useState(''); const [filter, setFilter] = useState({ query: '', page: 0 })
  const [loading, setLoading] = useState(false); const [error, setError] = useState('')
  const scope = options.scopes.find(s => s.id === scopeId && s.canCreate)
  const report = candidates?.items.find(p => p.id === reportId)
  const dirty = kind === 'T1' ? !!scopeId || titleOverride !== null : !!reportId
  useEffect(() => { onDirtyChange(dirty) }, [dirty, onDirtyChange])
  useEffect(() => {
    if (kind !== 'T2') return
    let active = true
    api<ReportOptions>('/reports/options?' + new URLSearchParams({ query: filter.query, page: String(filter.page), size: '20' })).then(data => {
      if (active) { setCandidates(data); setLoading(false) }
    }).catch(e => { if (active) { setError(e.message); setLoading(false) } })
    return () => { active = false }
  }, [kind, filter])
  function switchKind(next: typeof kind) {
    if (kind === next || busy) return
    if (dirty && !window.confirm('¿Descartar los datos de creación para cambiar de tipo documental?')) return
    setScopeId(''); setTitleOverride(null); setReportId(''); setError(''); setRequestKey(crypto.randomUUID()); setKind(next)
    if (next === 'T2') { setCandidates(null); setLoading(true); setSearch(''); setFilter({ query: '', page: 0 }) }
  }
  function searchPlans(page = 0, query = search) { setLoading(true); setReportId(''); setError(''); setFilter({ query, page }) }
  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); onBusyChange(true)
    try {
      if (kind === 'T1' && scope) onCreated(await api<WorkPlan>('/work-plans', 'POST', { scopeId: scope.id, groupId: scope.groupId, periodId: scope.periodId, title: titleOverride, requestKey }))
      else if (kind === 'T2' && report) onCreated(await api<WorkPlan>('/work-plans/' + report.id + '/report', 'POST'))
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo crear el documento.') }
    finally { onBusyChange(false) }
  }
  return <section className="card animate-reveal document-creation">
    <div className="section-heading"><span className="section-icon"><Icon name="document" /></span><div><span className="section-kicker">NUEVO DOCUMENTO</span><h2>¿Qué documento desea crear?</h2></div></div>
    <p className="muted">Seleccione el formato y su ámbito institucional. El nombre se completa automáticamente.</p>
    <div className="document-kind-options" role="group" aria-label="Tipo de documento">
      {([['T1', 'Plan de Trabajo', 'Organice objetivos, actividades y responsables.'], ['T2', 'Informe', 'Documente resultados a partir de un Plan aprobado.']] as const).map(([type, label, help]) => <label key={type} className={'document-kind-card ' + (kind === type ? 'selected' : '')}>
        <input type="radio" name="document-kind" value={type} checked={kind === type} disabled={busy} onChange={() => switchKind(type)} />
        <span className="document-kind-icon"><Icon name={type === 'T1' ? 'calendar' : 'document'} size={24} /></span>
        <span><strong>{label}</strong><small>{help}</small><span className="document-kind-format">UTA-SGC-A-2-1-P7-{type}</span></span>
      </label>)}
    </div>
    {error && <Toast message={error} />}
    {kind === 'T1' ? !options.scopes.some(s => s.canCreate) ? <div className="empty-state"><Icon name="users" size={28} /><p>No tiene ámbitos disponibles para crear un Plan.</p><small>Requiere designación como responsable principal, ventana abierta y un ámbito sin otro Plan activo.</small></div> :
      <form onSubmit={create}><fieldset disabled={busy} className="document-create-fields">
        <label>Ámbito del nuevo Plan<select required value={scopeId} onChange={e => { setScopeId(e.target.value); setTitleOverride(null); setRequestKey(crypto.randomUUID()) }}><option value="">Seleccione grupo, carrera y período</option>{options.scopes.filter(s => s.canCreate).map(s => <option key={s.id} value={s.id}>{s.groupName} · {s.careerName ?? 'Facultad'} · {s.periodName}</option>)}</select></label>
        <label>Título del nuevo Plan<input required maxLength={200} disabled={!scope || busy} placeholder="Se completa al seleccionar el ámbito" value={titleOverride ?? scope?.suggestedTitle ?? ''} onChange={e => { setTitleOverride(e.target.value); setRequestKey(crypto.randomUUID()) }} /><small className="field-help">Nombre propuesto según el grupo o unidad. Puede ajustarlo antes de crear.</small></label>
        <button disabled={busy || !scope || (titleOverride !== null && !titleOverride.trim())}><Icon name="document" size={17} />{busy ? 'Creando…' : 'Crear borrador'}</button>
      </fieldset></form> : <>
      <form className="document-base-search" onSubmit={e => { e.preventDefault(); searchPlans() }}><label>Buscar Plan aprobado<input maxLength={200} value={search} disabled={busy} placeholder="Título, grupo o período" onChange={e => setSearch(e.target.value)} /></label><button type="submit" className="secondary" disabled={busy || loading}><Icon name="search" size={17} />Buscar Plan</button></form>
      {loading ? <p role="status">Cargando Planes aprobados…</p> : candidates?.items.length ? <form onSubmit={create}><fieldset disabled={busy} className="document-create-fields">
        <label>Plan base del Informe<select required value={reportId} onChange={e => setReportId(e.target.value)}><option value="">Seleccione un Plan aprobado</option>{candidates.items.map(p => <option key={p.id} value={p.id}>{p.title} · {p.periodName} · Versión {p.formalVersion}{p.reportId ? ' · Informe existente' : ''}</option>)}</select></label>
        <label>Nombre del Informe<input readOnly placeholder="Se completa al seleccionar el Plan" value={report?.suggestedTitle ?? ''} /><small className="field-help">Se mantienen la relación, actividades y datos del Plan fuente.{report?.reportId && ' Ya existe un Informe: se abrirá ese documento.'}</small></label>
        <button disabled={busy || !report}><Icon name="document" size={17} />{busy ? 'Procesando…' : report?.reportId ? 'Abrir Informe' : 'Crear Informe'}</button>
      </fieldset></form> : candidates && <div className="empty-state"><Icon name="document" size={28} /><p>{filter.query ? 'No hay Planes aprobados que coincidan con la búsqueda.' : 'Aún no tiene un Plan aprobado disponible para elaborar su Informe.'}</p><small>El Informe Final requiere un Plan vigente del que sea responsable principal.</small></div>}
      {!!candidates?.total && !loading && <div className="pagination"><span>{candidates.total} Planes · Página {candidates.page + 1}</span><button className="secondary" disabled={busy || candidates.page === 0} onClick={() => searchPlans(candidates.page - 1, filter.query)}>Anterior</button><button className="secondary" disabled={busy || (candidates.page + 1) * candidates.size >= candidates.total} onClick={() => searchPlans(candidates.page + 1, filter.query)}>Siguiente</button></div>}
    </>}
  </section>
}
