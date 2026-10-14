import { confirmAction } from '../shared/confirm-action'
import { Toast } from '../shared/Toast'
import { useCallback, useEffect, useState } from 'react'
import { api, download } from '../shared/api'
import { WorkPlanEditor } from './WorkPlanEditor'
import type { WorkPlan, PlanOptions, PlanPage } from './types'
import { Icon } from '../shared/Icon'
import { ReportEditor } from './ReportEditor'
import { DocumentCreation } from './DocumentCreation'
import { ActionForm } from '../shared/ActionForm'

export function WorkPlans({ onDirtyChange, initialDocId }: { onDirtyChange: (dirty: boolean) => void; initialDocId?: string | null }) {
  const [options, setOptions] = useState<PlanOptions | null>(null)
  const [result, setResult] = useState<PlanPage | null>(null)
  const [selected, setSelected] = useState<WorkPlan | null>(null)
  const [error, setError] = useState(''); const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false)
  const [filters, setFilters] = useState({ groupId: '', periodId: '', query: '', page: 0 })
  const [search, setSearch] = useState('')
  const [creationDirty, setCreationDirty] = useState(false); const [creationKey, setCreationKey] = useState(0)
  const creationChanged = useCallback((dirty: boolean) => { setCreationDirty(dirty); onDirtyChange(dirty) }, [onDirtyChange])
  const [reload, setReload] = useState(0)

  useEffect(() => { let active = true; if (initialDocId) api<WorkPlan>('/work-plans/' + initialDocId).then(plan => { if (active) setSelected(plan) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [initialDocId])
  useEffect(() => {
    let cancelled = false
    const query = new URLSearchParams({ page: String(filters.page), query: filters.query })
    if (filters.groupId) query.set('groupId', filters.groupId)
    if (filters.periodId) query.set('periodId', filters.periodId)
    Promise.all([api<PlanOptions>('/work-plans/options'), api<PlanPage>('/work-plans?' + query)]).then(([o,p]) => {
      if (!cancelled) { setOptions(o); setResult(p); setLoading(false) }
    }).catch(e => { if (!cancelled) { setError(e.message); setLoading(false) } })
    return () => { cancelled = true }
  }, [filters, reload])
  async function open(id: string) {
    if (creationDirty && !(await confirmAction('¿Descartar los datos del nuevo documento?'))) return
    setCreationKey(n => n + 1); creationChanged(false)
    setBusy(true); setError('')
    try { setSelected(await api<WorkPlan>('/work-plans/' + id)) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo abrir el Plan.') } finally { setBusy(false) }
  }
  if (selected?.documentType === 'T2') return <ReportEditor key={selected.id} initial={selected} onDirtyChange={onDirtyChange} onBack={() => { setSelected(null); setLoading(true); setReload(n => n+1) }} />
  if (selected) return <WorkPlanEditor key={selected.id} initial={selected} onDirtyChange={onDirtyChange} onBack={() => { setSelected(null); setLoading(true); setReload(n => n+1) }} />
  return <section>
    <span className="section-kicker">ESPACIO DOCUMENTAL</span>
    <h1>Gestión Documental Académica</h1>
    <p className="muted">Planes de Trabajo T1 e Informes T2 de sus ámbitos institucionales. La edición corresponde al responsable principal.</p>
    {error && <Toast message={error} />}
    {options && <ActionForm modal label="Crear documento" dirty={creationDirty} disabled={busy || loading} onCancel={() => { setCreationKey(n => n + 1); creationChanged(false) }}><DocumentCreation key={creationKey} options={options} busy={busy || loading} onBusyChange={setBusy} onDirtyChange={creationChanged} onCreated={document => { creationChanged(false); setSelected(document) }} /></ActionForm>}
    {loading ? <p role="status">Cargando documentos…</p> : <>
      <section className="card">
        <div className="section-heading">
          <span className="section-icon"><Icon name="calendar" /></span>
          <div>
            <span className="section-kicker">BANDEJA INSTITUCIONAL</span>
            <h2>Mis documentos</h2>
          </div>
        </div>
        <form className="grid" style={{ marginTop: 16 }} onSubmit={e => { e.preventDefault(); setLoading(true); setFilters(f => ({ ...f, query: search, page: 0 })) }}>
          <label>Buscar por título<input maxLength={200} placeholder="Escriba palabras del título…" value={search} onChange={e => setSearch(e.target.value)} /></label>
          <label>Filtrar por grupo<select aria-label="Filtrar por grupo" value={filters.groupId} onChange={e => { setLoading(true); setFilters(f => ({ ...f, groupId: e.target.value, page: 0 })) }}><option value="">Todos mis grupos</option>{options?.groups.map(g => <option value={g.id} key={g.id}>{g.name}</option>)}</select></label>
          <label>Filtrar por período<select aria-label="Filtrar por período" value={filters.periodId} onChange={e => { setLoading(true); setFilters(f => ({ ...f, periodId: e.target.value, page: 0 })) }}><option value="">Todos los períodos</option>{options?.periods.map(p => <option value={p.id} key={p.id}>{p.name}{p.status === 'CLOSED' ? ' · Cerrado' : ''}</option>)}</select></label>
          <button><Icon name="refresh" size={16} />Buscar</button>
        </form>
        {options?.periods.find(p => p.id === filters.periodId)?.status === 'CLOSED' && <div className="notice"><p>Período cerrado: los documentos se conservan en solo lectura. El ZIP contiene únicamente los documentos que tiene autorizados en este período.</p><button type="button" className="secondary" disabled={busy} onClick={async () => { setBusy(true); try { await download('/periods/' + filters.periodId + '/archive', 'historial-utaped-' + filters.periodId + '.zip') } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo descargar el archivo.') } finally { setBusy(false) } }}><Icon name="document" size={16} />Descargar archivo del período</button></div>}
        {!result?.items.length ? <div className="empty-state"><Icon name="document" size={32} /><p>No hay documentos que coincidan con los filtros.</p><small>Prueba ajustando los criterios de búsqueda o seleccionando otro grupo.</small></div> : <div className="table-scroll"><table><thead><tr><th>Documento</th><th>Grupo</th><th>Período</th><th>Estado</th><th>Acción</th></tr></thead><tbody>{result.items.map(plan => <tr key={plan.id}><td><strong>{plan.title}</strong><small className="field-help">{plan.documentType} · Versión {plan.formalVersion}</small></td><td>{plan.groupName}</td><td>{plan.periodName}</td><td><span className={'status-badge ' + (plan.editable ? 'active' : '')}>{plan.archivedAt ? 'Archivado' : plan.documentState === 'DRAFT' ? 'Borrador' : plan.documentState === 'APPROVED' ? 'Aprobado' : 'En revisión'}</span><small className="field-help">{plan.editable ? 'Puede editar' : 'Solo lectura'}</small></td><td><button className="secondary" title={plan.editable ? 'Editar borrador' : 'Ver documento'} aria-label={(plan.editable ? 'Editar ' : 'Ver ') + plan.title + ' · Versión ' + plan.formalVersion} disabled={busy} onClick={() => open(plan.id)} style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}><Icon name={plan.editable ? 'document' : 'eye'} size={15} /><span>{plan.editable ? 'Editar' : 'Ver'}</span></button></td></tr>)}</tbody></table></div>}
        {!!result?.total && <div className="pagination"><span>{result.total} documentos · Página {result.page+1}</span><button className="secondary" disabled={result.page === 0} onClick={() => { setLoading(true); setFilters(f => ({ ...f, page: f.page-1 })) }}>Anterior</button><button className="secondary" disabled={(result.page+1)*result.size >= result.total} onClick={() => { setLoading(true); setFilters(f => ({ ...f, page: f.page+1 })) }}>Siguiente</button></div>}
      </section>
    </>}
  </section>
}
