import { useCallback, useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'
import { Icon } from '../shared/Icon'
import { ActionForm } from '../shared/ActionForm'
import { confirmAction } from '../shared/confirm-action'
import type { WorkPlan } from './types'
import { PlanPreview, type PreparedPreview } from './PlanPreview'
import { WorkspaceResizer } from './WorkspaceResizer'
import { AttachmentEditor } from './AttachmentEditor'
import { DocumentPreparation } from './DocumentPreparation'
import { DocumentAuthorsEditor } from './DocumentAuthorsEditor'
import { DocumentTimeline } from './DocumentTimeline'
import './document-workspace.css'

type EventAction = { id: string; title: string; date: string | null; startsAt: string | null; endsAt: string | null; responsibleIds: string[]; responsibleDisplay?: string; resources: string; percentage: number | null; observations: string }
type EventData = {
  topic: string; eventType: string; otherType: string; startsOn: string | null; endsOn: string | null; startsAt: string | null; endsAt: string | null
  modality: string; otherModality: string; location: string; capacity: number | null; authorities: number | null; teachers: number | null; students: number | null; otherAttendees: number | null
  logistics: string; certificates: string; diffusion: string; generalObjective: string; specificObjectives: string; conclusions: string; source: string; privacyNoticeEnabled: boolean; privacyNotice: string; actions: EventAction[]
}
type EventState = { rowVersion: number; editable: boolean; enabled: boolean; data: EventData; members: { id: string; name: string }[]; document: WorkPlan }
const emptyAction = (): EventAction => ({ id: crypto.randomUUID(), title: '', date: null, startsAt: null, endsAt: null, responsibleIds: [], resources: '', percentage: null, observations: '' })
type Props = { initial: WorkPlan; onBack: () => void; onDirtyChange: (dirty: boolean) => void; onDocumentOpen?: (document: WorkPlan) => void; initialStep?: string | null; onStepChange?: (step: string) => void }

/** Loads only metadata to select the correct editor; failures never fall through to a different format. */
export function EventDocumentRouter({ children, ...props }: Props & { children: ReactNode }) {
  const [state, setState] = useState<EventState | null>(null), [error, setError] = useState(''), [retry, setRetry] = useState(0)
  useEffect(() => { let active = true; api<EventState>('/work-plans/' + props.initial.id + '/event').then(s => { if (active) setState(s) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [props.initial.id, retry])
  if (!state) return <section className="card">{error ? <><Toast message={error} /><button type="button" className="secondary" onClick={() => { setError(''); setRetry(n => n + 1) }}>Reintentar documento</button></> : <p role="status">Cargando documento…</p>}</section>
  return state.enabled ? <EventWorkspace {...props} state={state} /> : children
}

function EventWorkspace({ state: initialState, ...props }: Props & { state: EventState }) {
  const [state, setState] = useState(initialState), [data, setData] = useState(initialState.data), [justification, setJustification] = useState(initialState.document.justification)
  const [baseline, setBaseline] = useState(JSON.stringify([initialState.data, initialState.document.justification]))
  const [step, setStep] = useState(['general', 'content', 'matrix', 'attachments', 'signing'].indexOf(props.initialStep ?? 'general') < 0 ? 0 : ['general', 'content', 'matrix', 'attachments', 'signing'].indexOf(props.initialStep ?? 'general'))
  const [busy, setBusy] = useState(false), [childDirty, setChildDirty] = useState(false), [error, setError] = useState(''), [width, setWidth] = useState(46), [panel, setPanel] = useState(false)
  const [prepared, setPrepared] = useState<PreparedPreview | null>(null), [action, setAction] = useState<EventAction>(emptyAction), [actionSignal, setActionSignal] = useState(''), [actionKey, setActionKey] = useState(0), [actionDirty, setActionDirty] = useState(false)
  const { onDirtyChange } = props
  const failed = useRef(''), alive = useRef(true), saving = useRef(false)
  const fingerprint = JSON.stringify([data, justification]), dirty = fingerprint !== baseline
  const plan = state.document, isPlan = plan.documentType === 'T1'
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  useEffect(() => { onDirtyChange(dirty || busy || childDirty || actionDirty); return () => onDirtyChange(false) }, [dirty, busy, childDirty, actionDirty, onDirtyChange])
  const save = useCallback(async () => {
    if (saving.current || !state.editable || !dirty) return !dirty
    saving.current = true; setBusy(true); setError('')
    const captured = fingerprint
    try {
      const saved = await api<EventState>('/work-plans/' + plan.id + '/event', 'PUT', { rowVersion: state.rowVersion, enabled: true, data, justification })
      if (!alive.current) return false
      setState(saved); setBaseline(captured); failed.current = ''; return true
    } catch (e) { if (alive.current) { failed.current = captured; setError(e instanceof Error ? e.message : 'No se pudo guardar el evento.') }; return false }
    finally { saving.current = false; if (alive.current) setBusy(false) }
  }, [state.rowVersion, state.editable, dirty, fingerprint, plan.id, data, justification, setState, setError, setBusy, setBaseline])
  useEffect(() => { if (!dirty || busy || !state.editable || failed.current === fingerprint) return; const timer = setTimeout(() => { void save() }, 900); return () => clearTimeout(timer) }, [dirty, busy, state.editable, fingerprint, save])
  useEffect(() => { const prevent = (e: BeforeUnloadEvent) => { if (dirty || busy || childDirty || actionDirty) { e.preventDefault(); e.returnValue = '' } }; window.addEventListener('beforeunload', prevent); return () => window.removeEventListener('beforeunload', prevent) }, [dirty, busy, childDirty, actionDirty])
  async function navigate(next: number) {
    if (busy) return
    if (childDirty || actionDirty) { setError('Guarde o cancele los cambios del formulario abierto antes de cambiar de sección.'); return }
    if (step === 2 && next === 3 && (!data.actions.length || data.actions.some(a => !a.title.trim() || !a.date || !a.startsAt || !a.endsAt || !a.responsibleIds.length || !a.resources.trim() || !isPlan && a.percentage == null))) {
      setError('Complete al menos una actividad con fecha, horario, responsables, recursos y resultados que correspondan.'); return
    }
    if (dirty && !(await save())) return
    if (step === 3) {
      try { const latest=await api<EventState>('/work-plans/'+plan.id+'/event');setState(latest);setData(latest.data);setJustification(latest.document.justification);setBaseline(JSON.stringify([latest.data,latest.document.justification])) }
      catch(e) { setError(e instanceof Error ? e.message : 'No se pudo actualizar el documento.');return }
    }
    setStep(next); setPrepared(null); props.onStepChange?.(['general', 'content', 'matrix', 'attachments', 'signing'][next])
  }
  async function back() { if (busy) return; if ((dirty || childDirty || actionDirty) && !(await confirmAction('¿Descartar los cambios sin guardar y volver a Documentos?'))) return; props.onBack() }
  function field<K extends keyof EventData>(key: K, value: EventData[K]) { setData(d => ({ ...d, [key]: value })) }
  const childChanged = useCallback((value: boolean) => setChildDirty(value), [])
  const updatedPlan = useCallback((value: WorkPlan) => { setState(s => ({ ...s, document: value, rowVersion: value.rowVersion, editable: value.editable })) }, [setState])
  const preparedChanged = useCallback((value: PreparedPreview) => setPrepared(value), [])
  const steps = ['Datos del evento', isPlan ? 'Justificación y objetivos' : 'Conclusiones', 'Desarrollo del evento', 'Anexos y opciones', 'Revisar y firmar']
  if (panel) return <DocumentTimeline documentId={plan.id} title={plan.title} documentState={plan.documentState} documentType={plan.documentType} onBack={() => setPanel(false)} onDirtyChange={props.onDirtyChange} />
  return <section className="plan-wizard">
    <header className="plan-wizard-header"><span className="section-kicker">{isPlan ? 'PLANIFICACIÓN' : 'INFORME DE EJECUCIÓN'} DE EVENTO</span><div className="plan-header-title-row"><button type="button" className="plan-back-btn" title="Volver a Documentos" aria-label="Volver a Documentos" disabled={busy} onClick={() => void back()}><Icon name="arrowLeft" size={20} /></button><h1>{data.topic || plan.title}</h1></div><div className="document-status-line"><span className="status-badge">{plan.documentState === 'VALIDATED' ? 'Validado internamente' : plan.documentState === 'APPROVED' ? 'Aprobado' : plan.documentState === 'IN_REVIEW' ? 'En revisión' : 'Borrador'}</span><span role="status">{busy ? 'Guardando…' : dirty ? 'Cambios pendientes' : 'Guardado'}</span><button type="button" className="link" disabled={dirty || busy || childDirty || actionDirty} onClick={() => setPanel(true)}>Línea de tiempo</button></div></header>
    <nav className="document-steps" aria-label="Pasos del evento">{steps.map((label, i) => <button type="button" className="secondary" key={label} aria-current={step === i ? 'step' : undefined} disabled={busy} onClick={() => void navigate(i)}><span>{i + 1}</span>{label}</button>)}</nav>
    {error && <Toast message={error} />}
    <div className="plan-persistent-workspace" style={{ '--editor-width': width + '%' } as CSSProperties}>
      <div className="plan-wizard-workspace plan-editor-pane live-preview-editor"><section className="card"><div className="section-heading"><span className="section-icon"><Icon name="calendar" /></span><div><span className="section-kicker">PASO {step + 1} DE 5</span><h2>{steps[step]}</h2></div></div>
        {step < 3 && <fieldset disabled={!state.editable || busy}>
          {step === 0 && <><label>Tema del evento<input maxLength={500} value={data.topic} onChange={e => field('topic', e.target.value)} /></label><div className="grid"><label>Tipo de evento<select value={data.eventType} onChange={e => field('eventType', e.target.value)}><option value="">Seleccione</option>{[['ACADEMIC', 'Académico'], ['CULTURAL', 'Cultural'], ['SOCIAL', 'Social'], ['SPORTS', 'Deportivo'], ['RESEARCH', 'Investigativo'], ['OTHER', 'Otro']].map(([v,l]) => <option key={v} value={v}>{l}</option>)}</select></label>{data.eventType === 'OTHER' && <label>Otro tipo<input required maxLength={200} value={data.otherType} onChange={e => field('otherType', e.target.value)} /></label>}</div><div className="grid">{(['startsOn', 'endsOn', 'startsAt', 'endsAt'] as const).map((key,i) => <label key={key}>{['Fecha inicial','Fecha final','Hora inicial','Hora final'][i]}<input type={i < 2 ? 'date' : 'time'} value={data[key] ?? ''} onChange={e => field(key, e.target.value || null)} /></label>)}</div><div className="grid"><label>Modalidad<select value={data.modality} onChange={e => field('modality', e.target.value)}><option value="">Seleccione</option><option value="PRESENTIAL">Presencial</option><option value="VIRTUAL">Virtual</option><option value="OTHER">Otra</option></select></label>{data.modality === 'OTHER' && <label>Otra modalidad<input required maxLength={200} value={data.otherModality} onChange={e => field('otherModality', e.target.value)} /></label>}<label>Lugar o dirección virtual<input maxLength={2000} value={data.location} onChange={e => field('location', e.target.value)} /></label></div><div className="grid">{(['capacity', 'authorities', 'teachers', 'students', 'otherAttendees'] as const).filter(k => isPlan || k !== 'capacity').map(key => <label key={key}>{{ capacity:'Aforo',authorities:'Autoridades',teachers:'Docentes',students:'Estudiantes',otherAttendees:'Otros asistentes' }[key]}<input type="number" min={0} step={1} value={data[key] ?? ''} onChange={e => field(key, e.target.value === '' ? null : Number(e.target.value))} /></label>)}</div>{(['logistics','certificates','diffusion','source'] as const).map(key => <label key={key}>{{logistics:'Recursos logísticos',certificates:'Certificados',diffusion:'Difusión del evento',source:'Fuente'}[key]}<textarea rows={2} maxLength={key === 'source' ? 2000 : 4000} value={data[key]} onChange={e => field(key,e.target.value)} /></label>)}</>}
          {step === 1 && (isPlan ? <><label>Justificación<textarea rows={8} maxLength={50000} value={justification} onChange={e => setJustification(e.target.value)} /></label><label>Objetivo general<textarea rows={4} maxLength={10000} value={data.generalObjective} onChange={e => field('generalObjective',e.target.value)} /></label><label>Objetivos específicos<textarea rows={6} maxLength={10000} value={data.specificObjectives} onChange={e => field('specificObjectives',e.target.value)} /></label></> : <label>Conclusiones y/o recomendaciones<textarea rows={12} maxLength={10000} value={data.conclusions} onChange={e => field('conclusions',e.target.value)} /></label>)}
          {step === 2 && <><p>{data.actions.length} actividades</p>{data.actions.map(a => <article className="card" key={a.id}><h3>{a.title || 'Actividad pendiente'}</h3><p>{a.date || 'Fecha pendiente'} · {a.startsAt || '—'} a {a.endsAt || '—'}</p><p>{a.responsibleIds.map(id => state.members.find(m => m.id === id)?.name ?? a.responsibleDisplay ?? 'Responsable no disponible').join(', ')}</p><div className="row-actions"><button type="button" className="secondary" aria-label={'Editar ' + a.title} title="Editar actividad" onClick={() => { setAction(a); setActionDirty(false); setActionSignal(crypto.randomUUID()) }}><Icon name="edit" size={16} /></button><button type="button" className="secondary" aria-label={'Eliminar ' + a.title} title="Eliminar actividad" onClick={async () => { if (await confirmAction('¿Eliminar esta actividad del evento?')) field('actions', data.actions.filter(v => v.id !== a.id)) }}><Icon name="trash" size={16} /></button></div></article>)}</>}
        </fieldset>}
        {step < 3 && <button type="button" className="secondary" disabled={!dirty || busy || !state.editable} onClick={() => void save()}>Guardar borrador</button>}
        {step === 2 && <ActionForm key={actionKey} label="Agregar actividad" modal openSignal={actionSignal} disabled={!state.editable || busy} dirty={actionDirty} onCancel={() => { setAction(emptyAction()); setActionDirty(false); setActionSignal('') }}><form onChange={() => setActionDirty(true)} onSubmit={e => { e.preventDefault(); field('actions',data.actions.some(a => a.id === action.id) ? data.actions.map(a => a.id === action.id ? action : a) : [...data.actions,action]); setAction(emptyAction()); setActionDirty(false); setActionSignal(''); setActionKey(k => k + 1) }}><label>Actividad<input required maxLength={2000} value={action.title} onChange={e => setAction(a => ({...a,title:e.target.value}))} /></label><div className="grid"><label>Fecha<input type="date" value={action.date ?? ''} onChange={e => setAction(a => ({...a,date:e.target.value || null}))} /></label><label>Hora inicial<input type="time" value={action.startsAt ?? ''} onChange={e => setAction(a => ({...a,startsAt:e.target.value || null}))} /></label><label>Hora final<input type="time" value={action.endsAt ?? ''} onChange={e => setAction(a => ({...a,endsAt:e.target.value || null}))} /></label></div><fieldset><legend>Responsables</legend><button type="button" className="secondary" onClick={() => { setActionDirty(true); setAction(a => ({...a,responsibleIds:state.members.map(m => m.id)})) }}>Seleccionar todos</button>{state.members.map(m => <label className="check" key={m.id}><input type="checkbox" checked={action.responsibleIds.includes(m.id)} onChange={e => setAction(a => ({...a,responsibleIds:e.target.checked ? [...a.responsibleIds,m.id] : a.responsibleIds.filter(id => id !== m.id)}))} />{m.name}</label>)}</fieldset><label>Recursos y/o presupuesto<textarea maxLength={4000} value={action.resources} onChange={e => setAction(a => ({...a,resources:e.target.value}))} /></label>{!isPlan && <><label>Porcentaje de cumplimiento<input type="number" min={0} max={100} step="0.1" value={action.percentage ?? ''} onChange={e => setAction(a => ({...a,percentage:e.target.value === '' ? null : Number(e.target.value)}))} /></label><label>Observaciones<textarea maxLength={4000} value={action.observations} onChange={e => setAction(a => ({...a,observations:e.target.value}))} /></label></>}<button>Aplicar actividad</button></form></ActionForm>}
        {step === 3 && <AttachmentEditor targetDocId={plan.id} documentType={plan.documentType} onBack={() => void navigate(2)} onDirtyChange={childChanged} />}
        {step === 4 && <DocumentPreparation targetDocId={plan.id} documentType={plan.documentType} mode="sign" externalPreview onPreparedChange={preparedChanged} onDocumentChange={updatedPlan} onDirtyChange={childChanged} onBack={() => void navigate(3)} onFinish={async () => props.onBack()} />}
        {step === 0 && <fieldset disabled={!state.editable || busy}><label className="check"><input type="checkbox" checked={data.privacyNoticeEnabled} onChange={e => field('privacyNoticeEnabled',e.target.checked)} />Incluir nota de protección de datos</label>{data.privacyNoticeEnabled && <label>Nota<textarea required rows={4} maxLength={4000} value={data.privacyNotice} onChange={e => field('privacyNotice',e.target.value)} /></label>}</fieldset>}
        {step === 0 && <DocumentAuthorsEditor targetDocId={plan.id} version={state.rowVersion} disabled={dirty || busy} onDirtyChange={childChanged} onSaved={async () => { const next=await api<EventState>('/work-plans/'+plan.id+'/event');setState(next) }} />}
        {step < 4 && <div className="step-pagination"><button type="button" className="secondary" disabled={step === 0 || busy} onClick={() => void navigate(step - 1)}>Anterior</button><button type="button" disabled={busy} onClick={() => void navigate(step + 1)}>Siguiente: {steps[step + 1]}<Icon name="arrow" size={16} /></button></div>}
      </section></div>
      <WorkspaceResizer value={width} onChange={setWidth} />
      <PlanPreview event plan={plan} draft={{title:plan.title,institutionalUnit:plan.institutionalUnit,career:plan.career,justification,objective:data.generalObjective}} step={(['general','content','matrix','attachments','signing'] as const)[step]} focus={isPlan ? 'JUSTIFICATION' : 'OBJECTIVE'} dirty={dirty} textDirty={dirty && justification !== plan.justification} prepared={prepared} />
    </div>
    {isPlan && plan.documentState === 'APPROVED' && plan.canManage && !plan.archivedAt && <button type="button" disabled={dirty || busy || childDirty} onClick={async () => { try { const report=await api<WorkPlan>('/work-plans/'+plan.id+'/report','POST');props.onDocumentOpen?.(report) } catch(e) { setError(e instanceof Error ? e.message : 'No se pudo crear el informe.') } }}>Crear o abrir informe de ejecución del evento</button>}
  </section>
}
