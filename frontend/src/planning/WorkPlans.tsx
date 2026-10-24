import { confirmAction } from '../shared/confirm-action'
import { Toast } from '../shared/Toast'
import { useCallback, useEffect, useRef, useState } from 'react'
import { api, download } from '../shared/api'
import { WorkPlanEditor } from './WorkPlanEditor'
import type { WorkPlan, PlanOptions, PlanPage } from './types'
import { Icon } from '../shared/Icon'
import { ExecutionReportEditor } from './ExecutionReportEditor'
import { ReportEditor } from './ReportEditor'
import { DocumentCreation } from './DocumentCreation'
import { ActionForm } from '../shared/ActionForm'
import { DocumentArchiveDialog } from './DocumentArchiveDialog'

export function WorkPlans({
  onDirtyChange,
  initialDocId,
  initialStep,
  onDocumentSelect,
  onStepChange,
}: {
  onDirtyChange: (dirty: boolean) => void
  initialDocId?: string | null
  initialStep?: string | null
  onDocumentSelect?: (docId: string | null, step?: string | null) => void
  onStepChange?: (step: string) => void
}) {
  const [options, setOptions] = useState<PlanOptions | null>(null)
  const [result, setResult] = useState<PlanPage | null>(null)
  const [selected, setSelected] = useState<WorkPlan | null>(null)
  const [error, setError] = useState(''); const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false)
  const [filters, setFilters] = useState({ groupId: '', periodId: '', query: '', sort: 'UPDATED_DESC', page: 0 })
  const [search, setSearch] = useState('')
  const [creationDirty, setCreationDirty] = useState(false); const [creationKey, setCreationKey] = useState(0)
  const creationChanged = useCallback((dirty: boolean) => { setCreationDirty(dirty) }, [])
  const [archiveTarget, setArchiveTarget] = useState<PlanPage['items'][number] | null>(null)
  const [archiveDirty, setArchiveDirty] = useState(false), [success, setSuccess] = useState('')
  const listHeading = useRef<HTMLHeadingElement>(null), focusList = useRef(false)
  useEffect(() => { onDirtyChange(creationDirty || archiveDirty) }, [creationDirty, archiveDirty, onDirtyChange])
  useEffect(() => { if (!loading && focusList.current) { listHeading.current?.focus(); focusList.current = false } }, [loading, result])
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true
    if (initialDocId) {
      api<WorkPlan>('/work-plans/' + initialDocId)
        .then(plan => { if (active) setSelected(plan) })
        .catch(e => {
          if (active) {
            setError(e.message)
            setSelected(null)
            onDocumentSelect?.(null)
          }
        })
    } else {
      Promise.resolve().then(() => {
        if (active) setSelected(current => (current ? null : current))
      })
    }
    return () => { active = false }
  }, [initialDocId, onDocumentSelect])

  useEffect(() => {
    let cancelled = false
    const query = new URLSearchParams({ page: String(filters.page), query: filters.query, sort: filters.sort })
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
    try {
      const plan = await api<WorkPlan>('/work-plans/' + id)
      setSelected(plan)
      onDocumentSelect?.(plan.id, 'general')
    }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo abrir el Plan.') } finally { setBusy(false) }
  }

  if (selected?.documentType === 'T3') return <ExecutionReportEditor key={selected.id} initial={selected} initialStep={initialStep} onStepChange={onStepChange} onDirtyChange={onDirtyChange} onBack={() => { setSelected(null); setReload(n => n + 1); onDocumentSelect?.(null) }} />
  if (selected?.documentType === 'T2') {
    return (
      <ReportEditor
        key={selected.id}
        initial={selected}
        initialStep={initialStep}
        onStepChange={step => onStepChange?.(String(step))}
        onDirtyChange={onDirtyChange}
        onDocumentOpen={document => { setSelected(document); onDocumentSelect?.(document.id, document.documentType === 'T2' ? '0' : 'general') }}
        onBack={() => {
          setSelected(null)
          setLoading(true)
          setReload(n => n + 1)
          onDocumentSelect?.(null)
        }}
      />
    )
  }
  if (selected) {
    return (
      <WorkPlanEditor
        key={selected.id}
        initial={selected}
        defaultJustification={options?.defaultJustification}
        initialSection={initialStep}
        onSectionChange={sec => onStepChange?.(sec)}
        onDirtyChange={onDirtyChange}
        onDocumentOpen={document => { setSelected(document); onDocumentSelect?.(document.id, document.documentType === 'T2' ? '0' : 'general') }}
        onBack={() => {
          setSelected(null)
          setLoading(true)
          setReload(n => n + 1)
          onDocumentSelect?.(null)
        }}
      />
    )
  }
  if (initialDocId && !selected && !error) {
    return (
      <section>
        <span className="section-kicker">ESPACIO DOCUMENTAL</span>
        <h1>Gestión Documental Académica</h1>
        <p role="status">Cargando documento…</p>
      </section>
    )
  }

  return <section>
    <span className="section-kicker">ESPACIO DOCUMENTAL</span>
    <h1>Gestión Documental Académica</h1>
    <p className="muted">Planes de Trabajo T1 e Informes T2 de sus ámbitos institucionales. La edición corresponde al responsable principal.</p>
    {error && <Toast message={error} />}
    {success && <Toast message={success} tone="success" onDismiss={() => setSuccess('')} />}
    {archiveTarget && <DocumentArchiveDialog key={archiveTarget.id} plan={archiveTarget} onDirtyChange={setArchiveDirty} onCancel={() => setArchiveTarget(null)} onComplete={() => {
      setSuccess(archiveTarget.documentState === 'DRAFT' ? 'Borrador descartado.' : 'Documento archivado. Se conservan sus firmas e historial.')
      setArchiveTarget(null); focusList.current = true; setLoading(true)
      if (result?.items.length === 1 && filters.page > 0 && archiveTarget.documentState === 'DRAFT') setFilters(f => ({ ...f, page: f.page - 1 }))
      else setReload(n => n + 1)
    }} />}
    {options && <ActionForm modal label="Crear documento" dirty={creationDirty} disabled={busy || loading} onCancel={() => { setCreationKey(n => n + 1); creationChanged(false) }}><DocumentCreation key={creationKey} options={options} busy={busy || loading} onBusyChange={setBusy} onDirtyChange={creationChanged} onCreated={document => { creationChanged(false); setSelected(document); onDocumentSelect?.(document.id, 'general') }} /></ActionForm>}
    {loading ? <p role="status">Cargando documentos…</p> : <>
      <section className="card">
        <div className="section-heading">
          <span className="section-icon"><Icon name="calendar" /></span>
          <div>
            <span className="section-kicker">BANDEJA INSTITUCIONAL</span>
            <h2 ref={listHeading} tabIndex={-1}>Mis documentos</h2>
          </div>
        </div>
        <form className="filter-toolbar document-list-filters" onSubmit={e => { e.preventDefault(); setLoading(true); setFilters(f => ({ ...f, query: search, page: 0 })) }}>
          <label>Buscar por título<input maxLength={200} placeholder="Escriba palabras del título…" value={search} onChange={e => setSearch(e.target.value)} /></label>
          <label>Filtrar por grupo<select aria-label="Filtrar por grupo" value={filters.groupId} onChange={e => { setLoading(true); setFilters(f => ({ ...f, groupId: e.target.value, page: 0 })) }}><option value="">Todos mis grupos</option>{options?.groups.map(g => <option value={g.id} key={g.id}>{g.name}</option>)}</select></label>
          <label>Filtrar por período<select aria-label="Filtrar por período" value={filters.periodId} onChange={e => { setLoading(true); setFilters(f => ({ ...f, periodId: e.target.value, page: 0 })) }}><option value="">Todos los períodos</option>{options?.periods.map(p => <option value={p.id} key={p.id}>{p.name}{p.status === 'CLOSED' ? ' · Cerrado' : ''}</option>)}</select></label>
          <label>Ordenar por<select aria-label="Ordenar por" value={filters.sort} onChange={e => { setLoading(true); setFilters(f => ({ ...f, sort: e.target.value, page: 0 })) }}><option value="UPDATED_DESC">Actualizados recientemente</option><option value="UPDATED_ASC">Actualizados hace más tiempo</option><option value="TITLE_ASC">Título: A–Z</option><option value="TITLE_DESC">Título: Z–A</option></select></label>
          <button><Icon name="refresh" size={16} />Buscar</button>
        </form>
        {options?.periods.find(p => p.id === filters.periodId)?.status === 'CLOSED' && <div className="notice"><p>Período cerrado: los documentos se conservan en solo lectura. El ZIP contiene únicamente los documentos que tiene autorizados en este período.</p><button type="button" className="secondary" disabled={busy} onClick={async () => { setBusy(true); try { await download('/periods/' + filters.periodId + '/archive', 'historial-utaped-' + filters.periodId + '.zip') } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo descargar el archivo.') } finally { setBusy(false) } }}><Icon name="document" size={16} />Descargar archivo del período</button></div>}
        {!result?.items.length ? <div className="empty-state"><Icon name="document" size={32} /><p>No hay documentos que coincidan con los filtros.</p><small>Prueba ajustando los criterios de búsqueda o seleccionando otro grupo.</small></div> : <div className="table-scroll"><table className="documents-table"><thead><tr><th>Documento</th><th>Grupo</th><th>Período</th><th className="doc-col-status">Estado</th><th className="doc-col-action">Acciones</th></tr></thead><tbody>{result.items.map(plan => <tr key={plan.id}><td><div className="doc-title-cell"><span className={'doc-type-pill doc-type-' + plan.documentType.toLowerCase()}>{plan.documentType === 'T1' ? 'Plan T1' : plan.documentType === 'T3' ? 'Informe de ejecución' : 'Informe final T2'}</span><strong>{plan.title}</strong></div><small className="field-help">Versión formal {plan.formalVersion}</small></td><td>{plan.groupName}</td><td>{plan.periodName}</td><td className="doc-col-status"><span className={'status-badge ' + (plan.archivedAt ? 'status-archived' : plan.documentState === 'DRAFT' ? 'status-draft' : plan.documentState === 'APPROVED' ? 'status-approved' : 'status-in_review')}>{plan.archivedAt ? 'Archivado' : plan.documentState === 'DRAFT' ? 'Borrador' : plan.documentState === 'APPROVED' ? 'Aprobado' : 'En revisión'}</span></td><td className="doc-col-action"><div className="document-row-actions"><button className="secondary table-action-icon-btn" title={plan.editable ? 'Editar borrador' : 'Ver documento'} aria-label={(plan.editable ? 'Editar ' : 'Ver ') + plan.title + ' · Versión ' + plan.formalVersion} disabled={busy} onClick={() => open(plan.id)}><Icon name={plan.editable ? 'edit' : 'eye'} size={16} /></button>{plan.canManage && !plan.archivedAt && plan.documentState !== 'IN_REVIEW' && <button type="button" className={'secondary table-action-icon-btn document-lifecycle-action' + (plan.documentState === 'DRAFT' ? ' document-discard-action' : '')} disabled={busy} title={plan.documentState === 'DRAFT' ? 'Descartar borrador' : 'Archivar documento'} aria-label={(plan.documentState === 'DRAFT' ? 'Descartar borrador: ' : 'Archivar documento: ') + plan.title} aria-haspopup="dialog" onClick={() => { setError(''); setSuccess(''); setArchiveTarget(plan) }}><Icon name={plan.documentState === 'DRAFT' ? 'trash' : 'archive'} size={16} /></button>}</div></td></tr>)}</tbody></table></div>}
        {!!result?.total && <div className="pagination"><span>{result.total} documentos · Página {result.page+1}</span><button className="secondary" disabled={result.page === 0} onClick={() => { setLoading(true); setFilters(f => ({ ...f, page: f.page-1 })) }}>Anterior</button><button className="secondary" disabled={(result.page+1)*result.size >= result.total} onClick={() => { setLoading(true); setFilters(f => ({ ...f, page: f.page+1 })) }}>Siguiente</button></div>}
      </section>
    </>}
  </section>
}
