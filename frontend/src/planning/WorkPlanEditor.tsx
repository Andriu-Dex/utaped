import { Toast } from '../shared/Toast'
import { useEffect, useRef, useState, type FormEvent } from 'react'
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

export function WorkPlanEditor({ initial, onBack, onDirtyChange }: { initial: WorkPlan; onBack: () => void; onDirtyChange: (dirty: boolean) => void }) {
  const [plan, setPlan] = useState(initial)
  const [draft, setDraft] = useState(() => fields(initial))
  const [section, setSection] = useState<'general' | 'content' | 'matrix' | 'attachments' | 'preparation' | 'signing' | 'rounds' | 'execution' | 'history'>('general')
  const [report, setReport] = useState<WorkPlan | null>(null)
  const [error, setError] = useState(''); const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false); const [conflict, setConflict] = useState(false)
  const [latest, setLatest] = useState<WorkPlan | null>(null)
  const [lifecycleDirty, setLifecycleDirty] = useState(false)
  const contentDirty = JSON.stringify(draft) !== JSON.stringify(fields(plan))
  const dirty = contentDirty || lifecycleDirty
  const dirtyRef = useRef(dirty)
  useEffect(() => { dirtyRef.current = dirty; onDirtyChange(dirty) }, [dirty, onDirtyChange])
  useEffect(() => () => onDirtyChange(false), [onDirtyChange])
  useEffect(() => {
    const preventClose = (event: BeforeUnloadEvent) => { if (dirtyRef.current) { event.preventDefault(); event.returnValue = '' } }
    window.addEventListener('beforeunload', preventClose)
    return () => window.removeEventListener('beforeunload', preventClose)
  }, [])
  function change(name: keyof DraftFields, value: string) { setDraft(current => ({ ...current, [name]: value })); setNotice('') }
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
  function reload() {
    if (!latest || !window.confirm('¿Descartar sus cambios locales y cargar la versión del servidor?')) return
    setPlan(latest); setDraft(fields(latest)); setLatest(null); setConflict(false); setNotice('Versión del servidor cargada.'); setError('')
  }
  function back() { if (!dirty || window.confirm('Hay cambios sin guardar. ¿Salir y descartarlos?')) onBack() }
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
    try { setReport(await api<WorkPlan>('/work-plans/' + plan.id + '/report', 'POST')) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo crear el Informe.') } finally { setBusy(false) }
  }
  return <section>
    <button type="button" className="secondary" onClick={back} disabled={busy}>Volver a documentos</button>
    <span className="section-kicker review-kicker">PLAN DE TRABAJO · T1</span>
    <h1>{plan.title}</h1><div className="document-status-line"><span className="status-badge">{plan.documentState === 'DRAFT' ? 'Borrador' : plan.documentState === 'APPROVED' ? 'Aprobado' : 'En revisión'}</span><span>Versión formal {plan.formalVersion} · Ronda {plan.reviewRound}</span></div>
    <DocumentLifecycle plan={plan} disabled={busy || contentDirty} onDirtyChange={setLifecycleDirty} onChange={p => { setPlan(p); setDraft(fields(p)); setSection('general') }} />
    <p role="status">{dirty ? 'Tiene cambios sin guardar.' : 'Sin cambios pendientes.'}</p>
    <nav className="document-steps" aria-label="Pasos del Plan"><button className="secondary" type="button" aria-current={section === 'general' ? 'step' : undefined} onClick={() => setSection('general')}><span>1</span>Información general</button><button className="secondary" type="button" aria-current={section === 'content' ? 'step' : undefined} onClick={() => setSection('content')}><span>2</span>Contenido</button><button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('matrix')}><span>3</span>Actividades y matriz</button>
    <button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('attachments')}><span>4</span>Anexos y opciones</button>
    <button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('preparation')}><span>5</span>Preparación y previsualización</button>
    <button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('signing')}><span>6</span>Firma y envío</button></nav>
    <div className="review-actions"><button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('rounds')}>Rondas y observaciones</button><button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('execution')}>Ejecución y evidencias</button>{plan.documentState === 'APPROVED' && plan.canManage && !plan.archivedAt && <button type="button" disabled={dirty || busy || !!plan.archivedAt} onClick={() => void createReport()}><Icon name="document" size={16} />Crear o abrir Informe T2</button>}
    <button className="secondary" type="button" disabled={dirty || busy} onClick={() => setSection('history')}>Historial del Plan</button></div>
    {dirty && <p>Guarde los cambios del Plan antes de abrir otra sección.</p>}
    <div className="card"><dl className="plan-metadata"><div><dt>Responsable actual</dt><dd>{plan.teacherName}</dd></div><div><dt>Grupo</dt><dd>{plan.groupName}</dd></div><div><dt>Período</dt><dd>{plan.periodName}</dd></div><div><dt>Fecha de elaboración</dt><dd>{plan.preparationDate.split('-').reverse().join('/')}</dd></div></dl>
      {!plan.editable && <p className="field-help">Solo lectura según su permiso, estado documental, archivo y ventana de elaboración.</p>}
      <form onSubmit={save}><fieldset disabled={!plan.editable || busy}>
        {section === 'general' ? <>
          <label>Título del Plan<input required maxLength={200} value={draft.title} onChange={e => change('title', e.target.value)} /></label>
          <label>Unidad institucional<input maxLength={200} readOnly={!!plan.scopeId} value={draft.institutionalUnit} onChange={e => change('institutionalUnit', e.target.value)} /></label>
          <label>Carrera, cuando corresponda<input maxLength={200} readOnly={!!plan.scopeId} value={draft.career} onChange={e => change('career', e.target.value)} /></label>
        </> : <>
          <label>Justificación<textarea rows={9} maxLength={50000} value={draft.justification} onChange={e => change('justification', e.target.value)} /></label>
          <WritingAssistant documentId={plan.id} rowVersion={plan.rowVersion} field="JUSTIFICATION" label="Justificación" value={draft.justification} disabled={busy || !plan.editable || conflict} onApply={text => change('justification', text)} />
          <label>Objetivo<textarea rows={6} maxLength={50000} value={draft.objective} onChange={e => change('objective', e.target.value)} /></label>
          <WritingAssistant documentId={plan.id} rowVersion={plan.rowVersion} field="OBJECTIVE" label="Objetivo" value={draft.objective} disabled={busy || !plan.editable || conflict} onApply={text => change('objective', text)} />
          <p>El borrador admite contenido incompleto. La preparación comprueba los campos antes de generar la previsualización.</p>
        </>}
        <button disabled={!dirty || conflict}>{busy ? 'Guardando…' : 'Guardar borrador'}</button>
      </fieldset></form>
      {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
      {conflict && <div className="conflict"><p>Sus cambios locales se conservan. Consulte la versión del servidor antes de decidir cómo resolver el conflicto.</p><button type="button" className="secondary" onClick={compare}>Consultar versión actual</button>
        {latest && <><h3>Contenido actual del servidor</h3><dl><dt>Título</dt><dd>{latest.title}</dd><dt>Unidad institucional</dt><dd>{latest.institutionalUnit}</dd><dt>Carrera</dt><dd>{latest.career}</dd><dt>Justificación</dt><dd className="preserve-text">{latest.justification || 'Sin contenido'}</dd><dt>Objetivo</dt><dd className="preserve-text">{latest.objective || 'Sin contenido'}</dd></dl><button type="button" className="secondary" onClick={reload}>Descartar cambios y cargar versión actual</button></>}
      </div>}
    </div>
  </section>
}
