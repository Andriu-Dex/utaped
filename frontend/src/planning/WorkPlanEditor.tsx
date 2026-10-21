import { DocumentAuthorsEditor } from './DocumentAuthorsEditor'
import { DraftPreview } from './DraftPreview'
import { confirmAction } from '../shared/confirm-action'
import { Toast } from '../shared/Toast'
import { useEffect, useRef, useState, type CSSProperties, type FormEvent } from 'react'
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
  const [report, setReport] = useState<WorkPlan | null>(null)
  const [error, setError] = useState(''); const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false); const [conflict, setConflict] = useState(false)
  const [latest, setLatest] = useState<WorkPlan | null>(null)
  const [lifecycleDirty, setLifecycleDirty] = useState(false)
  const contentDirty = JSON.stringify(draft) !== JSON.stringify(fields(plan))
  const [authorsDirty, setAuthorsDirty] = useState(false)
  const dirty = contentDirty || lifecycleDirty || authorsDirty

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
  async function returnToPlan() {
    try {
    const current = await api<WorkPlan>('/work-plans/' + plan.id)
    setPlan(current); setDraft(fields(current)); setSection('general'); setConflict(false); setError(''); setNotice('')
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo recargar el Plan.'); setSection('general') }
  }
  if (section === 'matrix') return <MatrixEditor targetDocId={plan.id} onDirtyChange={onDirtyChange} onBack={returnToPlan} />
  if (section === 'attachments') return <AttachmentEditor targetDocId={plan.id} onDirtyChange={onDirtyChange} onBack={returnToPlan} />
  if (section === 'preparation' || section === 'signing') return <DocumentPreparation targetDocId={plan.id} mode={section === 'signing' ? 'sign' : 'preview'} onBack={returnToPlan} />
  if (section === 'rounds') return <ReviewHistory documentId={plan.id} title={plan.title} onDirtyChange={onDirtyChange} onBack={returnToPlan} />
  if (section === 'execution') return <ExecutionWorkspace targetDocId={plan.id} onDirtyChange={onDirtyChange} onBack={returnToPlan} />
  if (section === 'history') return <AuditViewer targetDocId={plan.id} onBack={returnToPlan} />
  if (report) return <ReportEditor initial={report} onDirtyChange={onDirtyChange} onBack={() => setReport(null)} />
  async function createReport() {
    setBusy(true); setError('')
    try { const next = await api<WorkPlan>('/work-plans/' + plan.id + '/report', 'POST'); if (onDocumentOpen) onDocumentOpen(next); else setReport(next) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo crear el Informe.') } finally { setBusy(false) }
  }
  return <section>
    <button type="button" className="secondary" onClick={back} disabled={busy}>Volver a documentos</button>
    <span className="section-kicker review-kicker">PLAN DE TRABAJO · T1</span>
    <h1>{plan.title}</h1><div className="document-status-line"><span className="status-badge">{plan.documentState === 'DRAFT' ? 'Borrador' : plan.documentState === 'APPROVED' ? 'Aprobado' : 'En revisión'}</span><span>Versión formal {plan.formalVersion} · Ronda {plan.reviewRound}</span></div>
    <DocumentLifecycle plan={plan} disabled={busy || contentDirty || authorsDirty} onDirtyChange={setLifecycleDirty} onChange={p => { if (p.id !== plan.id && onDocumentOpen) onDocumentOpen(p); else { setPlan(p); setDraft(fields(p)); setSection('general') } }} />
    <p role="status" className={'document-dirty-indicator ' + (dirty ? 'is-dirty' : 'is-clean')}>{dirty ? 'Tiene cambios sin guardar.' : 'Sin cambios pendientes.'}</p>
    <nav className="document-steps" aria-label="Pasos del Plan"><button className="secondary" type="button" aria-current={section === 'general' ? 'step' : undefined} onClick={() => setSection('general')}><span>1</span>Información general</button><button className="secondary" type="button" aria-current={section === 'content' ? 'step' : undefined} onClick={() => setSection('content')}><span>2</span>Contenido</button><button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('matrix')}><span>3</span>Actividades y matriz</button>
    <button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('attachments')}><span>4</span>Anexos y opciones</button>
    <button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('preparation')}><span>5</span>Preparación y previsualización</button>
    <button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('signing')}><span>6</span>Firma y Finalización</button></nav>
    <div className="review-actions"><button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('rounds')}>Rondas y observaciones</button><button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('execution')}>Ejecución y evidencias</button>{plan.documentState === 'APPROVED' && plan.canManage && !plan.archivedAt && <button type="button" disabled={dirty || busy || !!plan.archivedAt} onClick={() => void createReport()}><Icon name="document" size={16} />Crear o abrir Informe T2</button>}
    <button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('history')}>Historial del Plan</button></div>
    {dirty && <p>Guarde los cambios del Plan antes de abrir otra sección.</p>}
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
      {section === 'content' && plan.editable && (
        <div className="live-preview-controls">
          <div className="live-preview-tabs" role="group" aria-label="Vista del contenido">
            <button
              type="button"
              className={`preview-tab-btn ${previewPane === 'split' ? 'is-active' : ''}`}
              aria-pressed={previewPane === 'split'}
              onClick={() => setPreviewPane('split')}
              title="Ver editor y documento lado a lado"
            >
              <Icon name="columns" size={15} />
              <span>Vista dividida</span>
            </button>
            <button
              type="button"
              className={`preview-tab-btn ${previewPane === 'edit' ? 'is-active' : ''}`}
              aria-pressed={previewPane === 'edit'}
              onClick={() => setPreviewPane('edit')}
              title="Ampliar editor a ancho completo para redactar cómodamente"
            >
              <Icon name="edit" size={15} />
              <span>Editar contenido</span>
            </button>
            <button
              type="button"
              className={`preview-tab-btn ${previewPane === 'preview' ? 'is-active' : ''}`}
              aria-pressed={previewPane === 'preview'}
              onClick={() => setPreviewPane('preview')}
              title="Ver el documento A4 generado"
            >
              <Icon name="document" size={15} />
              <span>Previsualizar documento</span>
            </button>
          </div>
          {previewPane === 'split' && (
            <div className="live-preview-width-wrapper">
              <label className="live-preview-width" htmlFor={'editor-width-' + plan.id}>
                <span className="live-preview-width-label">
                  <Icon name="columns" size={14} />
                  <span>Ancho del formulario:</span>
                  <strong>{editorWidth}%</strong>
                </span>
                <input
                  id={'editor-width-' + plan.id}
                  aria-label="Ancho del formulario"
                  type="range"
                  min="38"
                  max="58"
                  value={editorWidth}
                  onChange={e => setEditorWidth(Number(e.target.value))}
                />
              </label>
            </div>
          )}
        </div>
      )}
      <div className={section === 'content' && plan.editable ? 'live-preview-layout' : undefined} data-pane={previewPane} style={{ '--editor-width': editorWidth + '%' } as CSSProperties}><div className="live-preview-editor">
      <form onSubmit={save}><fieldset disabled={!plan.editable || busy}>
        {section === 'general' ? <>
          <label>Título del Plan<input required maxLength={200} value={draft.title} onChange={e => change('title', e.target.value)} /></label>
          <label>Unidad institucional<input maxLength={200} readOnly={!!plan.scopeId} value={draft.institutionalUnit} onChange={e => change('institutionalUnit', e.target.value)} /></label>
          <label>Carrera, cuando corresponda<input maxLength={200} readOnly={!!plan.scopeId} value={draft.career} onChange={e => change('career', e.target.value)} /></label>
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
          <p className="field-help">El borrador admite contenido incompleto. La preparación comprueba los campos antes de generar la previsualización.</p>
        </>}
        <div className="document-step-actions">
          <div className="step-save-row">
            <span role="status" className={'step-save-status' + (dirty ? ' is-dirty' : '')}>
              {dirty ? '• Hay cambios sin guardar' : '• Contenido sincronizado'}
            </span>
            <button
              disabled={!dirty || conflict}
              className={dirty ? 'primary-save-btn' : 'secondary'}
            >
              <Icon name="check" size={14} />
              <span>{busy ? 'Guardando…' : 'Guardar borrador'}</span>
            </button>
          </div>
          <div className="step-pagination-row">
            {section === 'content' ? (
              <button
                type="button"
                className="secondary step-prev-btn"
                disabled={busy}
                onClick={() => setSection('general')}
              >
                ← Información general
              </button>
            ) : <span className="step-nav-spacer" />}
            {section === 'general' && (
              <button
                type="button"
                className="secondary step-next-btn"
                disabled={busy}
                onClick={() => setSection('content')}
              >
                Siguiente: Contenido →
              </button>
            )}
            {section === 'content' && (
              <button
                type="button"
                className="secondary step-next-btn"
                disabled={dirty || busy}
                onClick={() => setSection('matrix')}
                title={dirty ? 'Guarda el borrador antes de continuar al siguiente paso' : undefined}
              >
                Siguiente: Actividades y matriz →
              </button>
            )}
          </div>
        </div>
      </fieldset></form></div>
      {section === 'content' && plan.editable && <DraftPreview key={plan.id} targetDocId={plan.id} rowVersion={plan.rowVersion} title={draft.title} institutionalUnit={draft.institutionalUnit} career={draft.career} justification={draft.justification} objective={draft.objective} focus={previewFocus} dirty={contentDirty} />}</div>
      {section === 'general' && <DocumentAuthorsEditor targetDocId={plan.id} version={plan.rowVersion} disabled={busy || contentDirty} onDirtyChange={setAuthorsDirty} onSaved={async () => { const current = await api<WorkPlan>('/work-plans/' + plan.id); setPlan(current); setDraft(fields(current)) }} />}
      {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
      {conflict && <div className="conflict"><p>Sus cambios locales se conservan. Consulte la versión del servidor antes de decidir cómo resolver el conflicto.</p><button type="button" className="secondary" onClick={compare}>Consultar versión actual</button>
        {latest && <><h3>Contenido actual del servidor</h3><dl><dt>Título</dt><dd>{latest.title}</dd><dt>Unidad institucional</dt><dd>{latest.institutionalUnit}</dd><dt>Carrera</dt><dd>{latest.career}</dd><dt>Justificación</dt><dd className="preserve-text">{latest.justification || 'Sin contenido'}</dd><dt>Objetivo</dt><dd className="preserve-text">{latest.objective || 'Sin contenido'}</dd></dl><button type="button" className="secondary" onClick={reload}>Descartar cambios y cargar versión actual</button></>}
      </div>}
    </div>
  </section>
}
