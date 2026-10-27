import { StepPagination } from './StepPagination'
import { InstitutionalProvenance } from './InstitutionalProvenance'
import { DocumentAuthorsEditor } from './DocumentAuthorsEditor'
import { PlanPreview, type PreparedPreview } from './PlanPreview'
import { WorkspaceResizer } from './WorkspaceResizer'
import { confirmAction } from '../shared/confirm-action'
import { Toast } from '../shared/Toast'
import { useCallback, useEffect, useRef, useState, type CSSProperties, type FormEvent } from 'react'
import { api, ApiError } from '../shared/api'
import type { WorkPlan } from './types'
import { MatrixEditor } from './MatrixEditor'
import { AttachmentEditor } from './AttachmentEditor'
import { DocumentPreparation } from './DocumentPreparation'
import { AuditViewer } from '../audit/AuditViewer'
import { ReviewHistory } from '../workflow/ReviewHistory'
import { ExecutionWorkspace } from './ExecutionWorkspace'
import { ReportEditor } from './ReportEditor'
import { DocumentLifecycle } from './DocumentLifecycle'
import { WritingAssistant } from './WritingAssistant'
import { Icon } from '../shared/Icon'
import { PlanStepNavigationContext, planSteps, type PlanStep, type PlanStepGuard } from './PlanStepNavigation'
import './document-workspace.css'

type DraftFields = Pick<WorkPlan, 'title' | 'institutionalUnit' | 'career' | 'justification' | 'objective'>
function fields(plan: WorkPlan): DraftFields {
  return { title: plan.title, institutionalUnit: plan.institutionalUnit, career: plan.career, justification: plan.justification, objective: plan.objective }
}

export type WorkPlanSection = 'general' | 'content' | 'matrix' | 'attachments' | 'preparation' | 'signing' | 'rounds' | 'execution' | 'history'
const validSections: WorkPlanSection[] = ['general', 'content', 'matrix', 'attachments', 'preparation', 'signing', 'rounds', 'execution', 'history']

export const INSTITUTIONAL_JUSTIFICATION =
  'El presente Plan de Trabajo se formula en cumplimiento del Estatuto de la Universidad Técnica de Ambato, ' +
  'el Reglamento de Carrera y Escalafón del Personal Académico del Sistema de Educación Superior y los lineamientos ' +
  'académicos de la Facultad de Ingeniería en Sistemas, Electrónica e Industrial (FISEI). Su propósito es articular, ' +
  'planificar y ejecutar de manera eficiente las actividades académicas, pedagógicas, de tutoría, investigación ' +
  'formativa y vinculación con la sociedad correspondientes al presente período académico, garantizando el rigor ' +
  'formativo y la calidad institucional.'

export function WorkPlanEditor({
  initial,
  defaultJustification,
  initialSection,
  onSectionChange,
  onBack,
  onDirtyChange,
  onDocumentOpen,
}: {
  initial: WorkPlan
  defaultJustification?: string
  onDocumentOpen?: (document: WorkPlan) => void
  initialSection?: string | null
  onSectionChange?: (section: WorkPlanSection) => void
  onBack: () => void
  onDirtyChange: (dirty: boolean) => void
}) {
  const [plan, setPlan] = useState(initial)
  const [draft, setDraft] = useState(() => fields(initial))
  const [section, setSectionState] = useState<WorkPlanSection>(() => {
    return validSections.find(value => value === initialSection) ?? 'general'
  })
  const [previousSection, setPreviousSection] = useState(initialSection)
  if (previousSection !== initialSection) { setPreviousSection(initialSection); setSectionState(validSections.find(value => value === initialSection) ?? 'general') }
  function setSection(next: WorkPlanSection) {
    setSectionState(next)
    onSectionChange?.(next)
  }
  const [previewFocus, setPreviewFocus] = useState<'JUSTIFICATION' | 'OBJECTIVE'>('JUSTIFICATION')
  const [previewPane, setPreviewPane] = useState<'split' | 'edit' | 'preview'>('split')
  const [editorWidth, setEditorWidth] = useState(46)
  const [isResizing, setIsResizing] = useState(false)
  const [preparedPreview, setPreparedPreview] = useState<PreparedPreview | null>(null)
  const previewPrepared = useCallback((value: PreparedPreview) => setPreparedPreview(previous => value.busy && !value.artifact && previous?.artifact ? { ...value, artifact: previous.artifact } : value), [])
  const [report, setReport] = useState<WorkPlan | null>(null)
  const [error, setError] = useState(''); const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false); const [conflict, setConflict] = useState(false)
  const [latest, setLatest] = useState<WorkPlan | null>(null)
  const [lifecycleDirty, setLifecycleDirty] = useState(false)
  const contentDirty = JSON.stringify(draft) !== JSON.stringify(fields(plan))
  const [authorsDirty, setAuthorsDirty] = useState(false)
  const [stepDirty, setStepDirty] = useState(false), [stepBusy, setStepBusy] = useState(false), [navigating, setNavigating] = useState(false)
  const stepGuard = useRef<PlanStepGuard | null>(null), navigationLock = useRef(false)
  const stepHeading = useRef<HTMLDivElement>(null)
  const registerStep = useCallback((guard: PlanStepGuard, pending: boolean) => {
    stepGuard.current = guard; setStepBusy(pending)
    return () => { if (stepGuard.current === guard) { stepGuard.current = null; setStepBusy(false) } }
  }, [])
  const refreshPlan = useCallback(async () => {
    try { const current = await api<WorkPlan>('/work-plans/' + initial.id); setPlan(current); setDraft(fields(current)) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo actualizar la vista del Plan.') }
  }, [initial.id])
  const synchronizePlan = useCallback((current: WorkPlan) => { setPlan(current); setDraft(fields(current)) }, [])
  const dirty = contentDirty || lifecycleDirty || authorsDirty || stepDirty

  useEffect(() => {
    const topbar = document.querySelector('.topbar'), navigation = stepHeading.current
    if (!topbar || !navigation) return
    const measure = () => { const height = Math.ceil(topbar.getBoundingClientRect().height) + 'px'; navigation.style.setProperty('--plan-topbar-height', height); navigation.closest<HTMLElement>('.plan-wizard')?.style.setProperty('--plan-topbar-height', height) }
    measure(); const observer = new ResizeObserver(measure); observer.observe(topbar, { box: 'border-box' })
    window.addEventListener('resize', measure)
    return () => { observer.disconnect(); window.removeEventListener('resize', measure) }
  }, [section])
  async function restoreJustification() {
    const template = defaultJustification?.trim() || INSTITUTIONAL_JUSTIFICATION
    if (draft.justification.trim() && draft.justification.trim() !== template.trim()) {
      const confirmed = await confirmAction('¿Desea reemplazar el texto actual de la justificación por la plantilla institucional modelo?')
      if (!confirmed) return
    }
    change('justification', template)
    setNotice('Plantilla institucional cargada en el formulario.')
  }
  const dirtyRef = useRef(dirty)
  useEffect(() => { dirtyRef.current = dirty; onDirtyChange(dirty) }, [dirty, onDirtyChange])
  useEffect(() => () => onDirtyChange(false), [onDirtyChange])
  useEffect(() => {
    const preventClose = (event: BeforeUnloadEvent) => { if (dirtyRef.current) { event.preventDefault(); event.returnValue = '' } }
    window.addEventListener('beforeunload', preventClose)
    return () => window.removeEventListener('beforeunload', preventClose)
  }, [])
  function change(name: keyof DraftFields, value: string) { if (name === 'justification') setPreviewFocus('JUSTIFICATION'); if (name === 'objective') setPreviewFocus('OBJECTIVE'); setDraft(current => ({ ...current, [name]: value })); setNotice('') }
  async function save(event: FormEvent) {
    event.preventDefault(); setError(''); setNotice(''); setBusy(true)
    try {
      const saved = await api<WorkPlan>('/work-plans/' + plan.id, 'PUT', { ...draft, rowVersion: plan.rowVersion })
      setPlan(saved); setDraft(fields(saved)); setNotice('Borrador guardado en el servidor.')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar.')
      if (e instanceof ApiError && e.status === 409) setConflict(true)
    } finally { setBusy(false) }
  }
  async function compare() {
    setError('')
    try { setLatest(await api<WorkPlan>('/work-plans/' + plan.id)) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo recuperar la versión actual.') }
  }
  async function reload() {
    if (!latest || !(await confirmAction('¿Descartar sus cambios locales y cargar la versión del servidor?'))) return
    setPlan(latest); setDraft(fields(latest)); setLatest(null); setConflict(false); setNotice('Versión del servidor cargada.'); setError('')
  }
  async function back() { if (!dirty || (await confirmAction('Hay cambios sin guardar. ¿Salir y descartarlos?'))) onBack() }
  async function navigateStep(next: WorkPlanSection) {
    const current = await api<WorkPlan>('/work-plans/' + plan.id)
    if (current.editable && ['attachments', 'signing'].includes(next)) {
      const readiness = await api<{ ready: boolean; blockers: { section: string; message: string }[] }>('/work-plans/' + plan.id + '/artifacts/readiness')
      const blockers = next === 'signing' ? readiness.blockers : readiness.blockers.filter(item => item.section === 'Actividades y matriz')
      if (blockers.length) throw new Error(blockers.map(item => item.message).join(' '))
    }
    setPlan(current); setDraft(fields(current)); setSection(next); setConflict(false); setError(''); setNotice('')
    requestAnimationFrame(() => stepHeading.current?.focus())
  }
  async function requestStep(next: PlanStep) {
    if (next === section || navigationLock.current || busy || stepBusy) return
    navigationLock.current = true; setNavigating(true); setError('')
    try {
      // These two views share the same draft: changing tabs must retain unsaved writing.
      if (['general', 'content'].includes(section) && ['general', 'content'].includes(next)) {
        if ((authorsDirty || lifecycleDirty) && !(await confirmAction('Hay cambios de configuración sin guardar. ¿Cambiar de paso y descartarlos?'))) return
        setSection(next); requestAnimationFrame(() => stepHeading.current?.focus()); return
      }
      if (stepGuard.current) { if (!(await stepGuard.current(next))) return }
      else if (dirty && !(await confirmAction('Hay cambios sin guardar. ¿Cambiar de paso y descartarlos?'))) return
      await navigateStep(next)
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo cambiar de paso.') }
    finally { navigationLock.current = false; setNavigating(false) }
  }
  async function returnToPlan() {
    try { await navigateStep('general') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo recargar el Plan.') }
  }
  if (section === 'rounds') return <ReviewHistory documentId={plan.id} title={plan.title} onDirtyChange={onDirtyChange} onBack={() => void returnToPlan()} />
  if (section === 'execution') return <ExecutionWorkspace targetDocId={plan.id} onDirtyChange={onDirtyChange} onBack={() => void returnToPlan()} />
  if (section === 'history') return <AuditViewer targetDocId={plan.id} onBack={() => void returnToPlan()} />
  if (report) return <ReportEditor initial={report} onDirtyChange={onDirtyChange} onBack={() => setReport(null)} />
  async function createReport() {
    setBusy(true); setError('')
    try { const next = await api<WorkPlan>('/work-plans/' + plan.id + '/report', 'POST'); if (onDocumentOpen) onDocumentOpen(next); else setReport(next) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo crear el Informe.') } finally { setBusy(false) }
  }
  const childStep = section === 'matrix' || section === 'attachments' || section === 'preparation' || section === 'signing'
  return <PlanStepNavigationContext.Provider value={registerStep}><section className="plan-wizard">
    <header className="plan-wizard-header">
      <span className="section-kicker">PLAN DE TRABAJO · T1</span>
      <div className="plan-header-title-row">
        <button
          type="button"
          className="plan-back-btn"
          onClick={back}
          disabled={busy || stepBusy || navigating}
          title="Volver a documentos"
          aria-label="Volver a documentos"
        >
          <Icon name="arrowLeft" size={20} />
          <span className="sr-only">Volver a documentos</span>
        </button>
        <h1>{plan.title}</h1>
      </div>
      <div className="document-status-line"><span className="status-badge">{plan.documentState === 'DRAFT' ? 'Borrador' : plan.documentState === 'APPROVED' ? 'Aprobado' : 'En revisión'}</span><span>Versión formal {plan.formalVersion} · Ronda {plan.reviewRound}</span><span className={'document-dirty-indicator ' + (dirty ? 'is-dirty' : 'is-clean')}>{dirty ? 'Cambios sin guardar' : 'Sin cambios pendientes'}</span></div>
    </header>
    <div className="plan-wizard-navigation" ref={stepHeading} tabIndex={-1}>
      <nav className="document-steps" aria-label="Pasos del Plan">{planSteps.map((step, index) => <button key={step.key} className="secondary" type="button" aria-current={section === step.key ? 'step' : undefined} disabled={busy || stepBusy || navigating} onClick={() => void requestStep(step.key)}><span>{index + 1}</span>{step.label}</button>)}</nav>
    </div>
    <div className="plan-step-location"><span role="status">Paso {planSteps.findIndex(step => step.key === section) + 1} de 6 · {planSteps.find(step => step.key === section)?.label}</span><span className="plan-step-document" title={plan.title}>{plan.title}</span></div>
    {error && <Toast message={error} onDismiss={() => setError('')} />}
    {section !== 'preparation' && (
      <div className="plan-preview-controls" role="group" aria-label="Distribución del espacio de trabajo">
        <div className="live-preview-tabs">
          {([{ key: 'split', label: 'Vista dividida', icon: 'columns' }, { key: 'edit', label: 'Editar contenido', icon: 'edit' }, { key: 'preview', label: 'Previsualizar documento', icon: 'document' }] as const).map(view => <button key={view.key} type="button" className={'preview-tab-btn' + (previewPane === view.key ? ' is-active' : '')} aria-pressed={previewPane === view.key} onClick={() => setPreviewPane(view.key)}><Icon name={view.icon} size={15} />{view.label}</button>)}
        </div>
      </div>
    )}
    {section === 'matrix' && previewPane === 'split' && <p className="field-help">Puede ampliar la matriz con «Ver matriz completa» o consultar el documento en paralelo.</p>}
    <div className={'plan-persistent-workspace' + (isResizing ? ' is-resizing' : '')} data-step={section} data-pane={previewPane} style={{ '--editor-width': editorWidth + '%' } as CSSProperties}>
    <div className="plan-wizard-workspace plan-editor-pane live-preview-editor">
    {childStep ? <>
      {section === 'matrix' && <MatrixEditor onSaved={refreshPlan} targetDocId={plan.id} onDirtyChange={setStepDirty} onBack={returnToPlan} onPrevious={() => navigateStep('content')} onNext={() => navigateStep('attachments')} isSplitView={previewPane === 'split'} onToggleSplit={() => setPreviewPane(current => current === 'split' ? 'edit' : 'split')} />}
      {section === 'attachments' && <AttachmentEditor onSaved={refreshPlan} nextLabel="Revisión final" targetDocId={plan.id} onDirtyChange={setStepDirty} onBack={() => void returnToPlan()} onPrevious={() => navigateStep('matrix')} onNext={() => navigateStep('preparation')} />}
      {(section === 'preparation' || section === 'signing') && <DocumentPreparation externalPreview onPreparedChange={previewPrepared} key={section} onDocumentChange={synchronizePlan} onDirtyChange={setStepDirty} targetDocId={plan.id} onResolveSection={navigateStep} mode={section === 'signing' ? 'sign' : 'preview'} onBack={() => void returnToPlan()} onPrevious={() => navigateStep(section === 'signing' ? 'preparation' : 'attachments')} onNext={section === 'preparation' ? () => navigateStep('signing') : undefined} />}
    </> : <>
    <DocumentLifecycle key={section} plan={plan} disabled={busy || contentDirty || authorsDirty} onDirtyChange={setLifecycleDirty} onChange={p => { if (p.archivedAt && p.documentState === 'DRAFT') { onBack(); return }; if (p.id !== plan.id && onDocumentOpen) onDocumentOpen(p); else { setPlan(p); setDraft(fields(p)); setSection('general') } }} />
    <div className="review-actions"><button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('rounds')}>Rondas y observaciones</button><button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('execution')}>Ejecución y evidencias</button>{plan.documentState === 'APPROVED' && plan.canManage && !plan.archivedAt && <button type="button" disabled={dirty || busy || !!plan.archivedAt} onClick={() => void createReport()}><Icon name="document" size={16} />Crear o abrir Informe T2</button>}
    <button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('history')}>Historial del Plan</button></div>
    {dirty && <p className="field-help">{contentDirty ? 'Puede alternar entre Información general y Contenido sin perder lo escrito. Guarde el borrador para conservarlo antes de avanzar.' : 'Guarde la configuración pendiente o confirme su descarte al cambiar de paso.'}</p>}
    <div className="card">
      <div className="step-card-header">
        <span className="section-icon"><Icon name="document" /></span>
        <div>
          <span className="section-kicker">PASO {section === 'general' ? '1 DE 6' : '2 DE 6'}</span>
          <h2>{section === 'general' ? 'Información general del Plan' : 'Contenido y redacción académica'}</h2>
        </div>
      </div>
      <dl className="plan-metadata"><div><dt>Responsable actual</dt><dd>{plan.teacherName}</dd></div><div><dt>Grupo</dt><dd>{plan.groupName}</dd></div><div><dt>Período</dt><dd>{plan.periodName}</dd></div><div><dt>Fecha de elaboración</dt><dd>{plan.documentState === 'DRAFT' ? 'Se fija al preparar la firma' : plan.preparationDate.split('-').reverse().join('/')}</dd></div></dl>
      {!plan.editable && <p className="field-help">Solo lectura según su permiso, estado documental, archivo y ventana de elaboración.</p>}
      <form onSubmit={save}><fieldset disabled={!plan.editable || busy || authorsDirty || lifecycleDirty}>
        {section === 'general' ? <>
          <label>Título del Plan<input required maxLength={200} value={draft.title} onChange={e => change('title', e.target.value)} /></label>
          <InstitutionalProvenance key={plan.id + ':' + plan.rowVersion} unit={draft.institutionalUnit} career={draft.career} unitType={plan.institutionalUnitType} inherited={!!plan.scopeId || !!plan.basePlanId} disabled={busy || authorsDirty || lifecycleDirty || !plan.editable} onChange={(unit, career) => { change('institutionalUnit', unit); change('career', career) }} />
          <div className="step-save-row">
            <span role="status" className={'step-save-status' + (dirty ? ' is-dirty' : '')}>
              {dirty ? 'Hay cambios sin guardar' : 'Contenido sincronizado'}
            </span>
            <button
              disabled={!contentDirty || conflict}
              className={dirty ? 'primary-save-btn' : 'secondary'}
            >
              <Icon name="check" size={14} />
              <span>{busy ? 'Guardando…' : 'Guardar borrador'}</span>
            </button>
          </div>
        </> : <>
          <div className="field-header-row">
            <label htmlFor={'justification-' + plan.id}>Justificación</label>
            <div className="field-header-actions">
              {plan.editable && (
                <button
                  type="button"
                  className="template-action-btn"
                  onClick={restoreJustification}
                  title={draft.justification.trim() ? 'Restaurar plantilla institucional oficial' : 'Cargar plantilla institucional'}
                  aria-label="Restaurar plantilla institucional de Justificación"
                >
                  <Icon name="refresh" size={13} />
                  <span>{draft.justification.trim() ? 'Restaurar texto institucional' : 'Cargar plantilla institucional'}</span>
                </button>
              )}
              <WritingAssistant documentId={plan.id} rowVersion={plan.rowVersion} field="JUSTIFICATION" label="Justificación" value={draft.justification} disabled={busy || !plan.editable || conflict} onApply={text => change('justification', text)} />
            </div>
          </div>
          <textarea id={'justification-' + plan.id} onFocus={() => setPreviewFocus('JUSTIFICATION')} rows={9} maxLength={50000} value={draft.justification} onChange={e => change('justification', e.target.value)} />
          <div className="field-header-row">
            <label htmlFor={'objective-' + plan.id}>Objetivo</label>
            <div className="field-header-actions">
              <WritingAssistant documentId={plan.id} rowVersion={plan.rowVersion} field="OBJECTIVE" label="Objetivo" value={draft.objective} disabled={busy || !plan.editable || conflict} onApply={text => change('objective', text)} />
            </div>
          </div>
          <textarea id={'objective-' + plan.id} onFocus={() => setPreviewFocus('OBJECTIVE')} rows={6} maxLength={50000} value={draft.objective} onChange={e => change('objective', e.target.value)} />
          <p className="field-help">El borrador admite contenido incompleto. La revisión final comprueba los campos antes de preparar el documento para firma.</p>
          <div className="document-step-actions">
            <div className="step-save-row">
              <span role="status" className={'step-save-status' + (dirty ? ' is-dirty' : '')}>
                {dirty ? 'Hay cambios sin guardar' : 'Contenido sincronizado'}
              </span>
              <button
                disabled={!contentDirty || conflict}
                className={dirty ? 'primary-save-btn' : 'secondary'}
              >
                <Icon name="check" size={14} />
                <span>{busy ? 'Guardando…' : 'Guardar borrador'}</span>
              </button>
            </div>
          </div>
        </>}
      </fieldset></form>
      {section === 'general' && <>
        <DocumentAuthorsEditor targetDocId={plan.id} version={plan.rowVersion} disabled={busy || contentDirty} onDirtyChange={setAuthorsDirty} onSaved={async () => { const current = await api<WorkPlan>('/work-plans/' + plan.id); setPlan(current); setDraft(fields(current)) }} />
      </>}
      {notice && <Toast tone="success" message={notice} />}
      {conflict && <div className="conflict"><p>Sus cambios locales se conservan. Consulte la versión del servidor antes de decidir cómo resolver el conflicto.</p><button type="button" className="secondary" onClick={compare}>Consultar versión actual</button>
        {latest && <><h3>Contenido actual del servidor</h3><dl><dt>Título</dt><dd>{latest.title}</dd><dt>Unidad institucional</dt><dd>{latest.institutionalUnit}</dd><dt>Carrera</dt><dd>{latest.career}</dd><dt>Justificación</dt><dd className="preserve-text">{latest.justification || 'Sin contenido'}</dd><dt>Objetivo</dt><dd className="preserve-text">{latest.objective || 'Sin contenido'}</dd></dl><button type="button" className="secondary" onClick={reload}>Descartar cambios y cargar versión actual</button></>}
      </div>}
    </div>
    <StepPagination previous={section === 'content' ? { label: 'Información general', action: () => void requestStep('general') } : undefined} next={{ label: section === 'general' ? 'Contenido' : 'Actividades y matriz', action: () => void requestStep(section === 'general' ? 'content' : 'matrix') }} disabled={busy || navigating} nextDisabled={dirty} />
    </>}
    </div>
    {previewPane === 'split' && section !== 'preparation' && section !== 'signing' && (
      <WorkspaceResizer
        value={editorWidth}
        onChange={setEditorWidth}
        onResizingChange={setIsResizing}
      />
    )}
    <PlanPreview plan={plan} draft={draft} step={section as PlanStep} focus={previewFocus} dirty={dirty} textDirty={contentDirty} prepared={preparedPreview} />
    </div>
  </section></PlanStepNavigationContext.Provider>
}
