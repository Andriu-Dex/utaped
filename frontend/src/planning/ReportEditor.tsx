import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'
import { DocumentLifecycle } from './DocumentLifecycle'
import { Icon } from '../shared/Icon'
import type { WorkPlan } from './types'
import { AttachmentEditor } from './AttachmentEditor'
import { DocumentPreparation } from './DocumentPreparation'
import { ReviewHistory } from '../workflow/ReviewHistory'
import { AuditViewer } from '../audit/AuditViewer'
import './document-workspace.css'

type Result = { activityId: string; percentage: number | null; observations: string }
type Delegation = { purpose: string; location: string; institution: string; contact: string; contactDetails: string; topic: string; agreement: string }
type Data = { background: string; conclusions: string; improvements: string; results: Result[]; delegations: Delegation[]; basePlanId: string; baseFormalVersion: string }
type State = { document: WorkPlan; data: Data; activities: { id: string; title: string }[]; evidence: { requiredMedia: number; coveredMedia: number } }
const steps = ['Información general', 'Antecedentes', 'Resultados', 'Conclusiones', 'Oportunidades de mejora', 'Delegaciones', 'Anexos', 'Previsualizar y firmar']

export function ReportEditor({ initial, onBack, onDirtyChange }: { initial: WorkPlan; onBack: () => void; onDirtyChange: (dirty: boolean) => void }) {
  const [document, setDocument] = useState(initial); const [lifecycleDirty, setLifecycleDirty] = useState(false)
  const [state, setState] = useState<State | null>(null); const [draft, setDraft] = useState<Data | null>(null); const [title, setTitle] = useState(initial.title); const [step, setStep] = useState(0)
  const [panel, setPanel] = useState<'edit' | 'attachments' | 'preparation' | 'rounds' | 'history'>('edit')
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false); const [reload, setReload] = useState(0)
  const contentDirty = !!state && (JSON.stringify(state.data) !== JSON.stringify(draft) || state.document.title !== title)
  const dirty = contentDirty || lifecycleDirty
  useEffect(() => { onDirtyChange(dirty); return () => onDirtyChange(false) }, [dirty, onDirtyChange])
  useEffect(() => { let active = true; api<State>('/reports/' + document.id).then(s => { if (active) { setState(s); setDraft(s.data); setTitle(s.document.title) } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [document.id, reload])
  async function save(event: FormEvent) {
    event.preventDefault(); if (!state || !draft) return; setBusy(true); setError('')
    try { const s = await api<State>('/reports/' + document.id, 'PUT', { ...draft, title, rowVersion: state.document.rowVersion }); setState(s); setDraft(s.data); setNotice('Informe guardado. El Plan base se conserva.'); }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar. Sus cambios se conservan.') } finally { setBusy(false) }
  }
  const returnToReport = () => { setPanel('edit'); setStep(0); setReload(r => r + 1) }
  if (panel === 'attachments') return <AttachmentEditor targetDocId={document.id} onDirtyChange={onDirtyChange} onBack={returnToReport} />
  if (panel === 'preparation') return <DocumentPreparation targetDocId={document.id} documentType="T2" onBack={returnToReport} />
  if (panel === 'rounds') return <ReviewHistory documentId={document.id} title={title} onBack={returnToReport} onDirtyChange={onDirtyChange} />
  if (panel === 'history') return <AuditViewer targetDocId={document.id} onBack={returnToReport} />
  function choose(next: number) { if (next < 0 || next >= steps.length) return; if (next >= 6 && dirty) { setError('Guarde los cambios antes de abrir anexos o firma.'); return } setStep(next); if (next === 6) setPanel('attachments'); if (next === 7) setPanel('preparation') }
  const blankDelegation: Delegation = { purpose: '', location: '', institution: '', contact: '', contactDetails: '', topic: '', agreement: '' }
  return <section><button type="button" className="secondary" disabled={busy} onClick={() => { if (!dirty || window.confirm('Hay cambios sin guardar. ¿Salir?')) onBack() }}>Volver a documentos</button><span className="section-kicker review-kicker">INFORME INSTITUCIONAL · T2</span><h1>{title}</h1>{error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    {!state || !draft ? <p role="status">Cargando Informe…</p> : <><div className="document-status-line"><span className="status-badge">{state.document.documentState === 'DRAFT' ? 'Borrador' : state.document.documentState === 'APPROVED' ? 'Aprobado' : 'En revisión'}</span><span>Versión formal {state.document.formalVersion} · Ronda {state.document.reviewRound}</span><button type="button" className="link" disabled={dirty} onClick={() => setPanel('rounds')}>Rondas y observaciones</button><button type="button" className="link" disabled={dirty} onClick={() => setPanel('history')}>Historial</button></div><DocumentLifecycle plan={state.document} disabled={busy || contentDirty} onDirtyChange={setLifecycleDirty} onChange={p => { setDocument(p); setState(null); setStep(0); setReload(n => n + 1) }} /><nav className="document-steps" aria-label="Pasos del Informe">{steps.map((label, i) => <button key={label} type="button" className="secondary" aria-current={step === i ? 'step' : undefined} onClick={() => choose(i)} disabled={busy}><span>{i + 1}</span>{label}</button>)}</nav>
    <section className="card"><div className="section-heading"><span className="section-icon"><Icon name="document" /></span><div><span className="section-kicker">PASO {step + 1} DE 8</span><h2>{steps[step]}</h2></div></div><form onSubmit={save}><fieldset disabled={busy || !state.document.editable}>
      {step === 0 && <><label>Título del Informe<input required maxLength={200} value={title} onChange={e => setTitle(e.target.value)} /></label><dl className="plan-metadata"><div><dt>Plan base</dt><dd>Versión {draft.baseFormalVersion}</dd></div><div><dt>Ámbito</dt><dd>{state.document.groupName} · {state.document.career || 'Facultad'}</dd></div><div><dt>Período</dt><dd>{state.document.periodName}</dd></div><div><dt>Elaborador</dt><dd>{state.document.teacherName}</dd></div></dl><p className="field-help">Este Informe tiene identidad, artefactos y flujo propios. Sus actividades proceden del Plan aprobado.</p></>}
      {step === 1 && <label>Antecedentes<textarea rows={12} maxLength={50000} value={draft.background} onChange={e => setDraft({ ...draft, background: e.target.value })} placeholder="Documente el origen real del Plan y el propósito del Informe…" /></label>}
      {step === 2 && <><p className="field-help">Cobertura de medios: {state.evidence.coveredMedia}/{state.evidence.requiredMedia}. El porcentaje de ejecución se declara por actividad; no se calcula a partir de la cantidad de PDFs.</p>{draft.results.map((r, i) => <div className="report-result" key={r.activityId}><h3>{state.activities.find(a => a.id === r.activityId)?.title}</h3><label>Porcentaje de ejecución de actividad {i + 1}<input type="number" min="0" max="100" step="0.01" value={r.percentage ?? ''} onChange={e => setDraft({ ...draft, results: draft.results.map(v => v.activityId === r.activityId ? { ...v, percentage: e.target.value === '' ? null : Number(e.target.value) } : v) })} /></label><label>Observaciones de actividad {i + 1}<textarea maxLength={4000} rows={3} value={r.observations} onChange={e => setDraft({ ...draft, results: draft.results.map(v => v.activityId === r.activityId ? { ...v, observations: e.target.value } : v) })} /></label></div>)}</>}
      {step === 3 && <label>Conclusiones<textarea rows={12} maxLength={50000} value={draft.conclusions} onChange={e => setDraft({ ...draft, conclusions: e.target.value })} /></label>}
      {step === 4 && <label>Oportunidades de mejora<textarea rows={12} maxLength={50000} value={draft.improvements} onChange={e => setDraft({ ...draft, improvements: e.target.value })} /></label>}
      {step === 5 && <><p className="field-help">Complete esta sección únicamente cuando existan delegaciones o gestiones aplicables.</p>{draft.delegations.map((d, i) => <div className="report-result" key={i}><h3>Delegación {i + 1}</h3><div className="grid">{([['purpose', 'Nombre o propósito'], ['location', 'Ciudad, país o institución'], ['institution', 'Entidad'], ['contact', 'Persona y cargo de contacto'], ['contactDetails', 'Correo o teléfono'], ['topic', 'Tema tratado'], ['agreement', 'Compromiso, responsable y plazo']] as const).map(([key, label]) => <label key={key}>{label}<input maxLength={key === 'topic' || key === 'agreement' ? 2000 : 500} required={key === 'purpose'} value={d[key]} onChange={e => setDraft({ ...draft, delegations: draft.delegations.map((v, n) => n === i ? { ...v, [key]: e.target.value } : v) })} /></label>)}</div><button type="button" className="secondary" onClick={() => setDraft({ ...draft, delegations: draft.delegations.filter((_, n) => n !== i) })}>Eliminar delegación {i + 1}</button></div>)}<button type="button" className="secondary" disabled={draft.delegations.length >= 100} onClick={() => setDraft({ ...draft, delegations: [...draft.delegations, blankDelegation] })}>Agregar delegación</button></>}
      <div className="document-save"><span role="status">{dirty ? 'Cambios sin guardar' : 'Contenido guardado'}</span><button disabled={!dirty}>{busy ? 'Guardando…' : 'Guardar Informe'}<Icon name="check" size={16} /></button></div>
    </fieldset></form><div className="review-pagination"><button type="button" className="secondary" disabled={step === 0 || busy} onClick={() => choose(step - 1)}>Paso anterior</button><button type="button" disabled={busy || (step >= 5 && dirty)} onClick={() => choose(step + 1)}>Siguiente paso</button></div></section></>}
  </section>
}
