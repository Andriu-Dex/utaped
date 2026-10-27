import { confirmAction } from '../shared/confirm-action'
import { ActionForm } from '../shared/ActionForm'
import { EvidenceMedium, type Medium } from './EvidenceMedium'
import './execution.css'
import { FormalSubmission } from './FormalSubmission'
import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'

type Execution = { id: string; title: string; startsOn: string; endsOn: string; deadline: string; extensionVersion: number; canUpload: boolean; complete: boolean; responsible: { id: string; name: string; deadline: string; extensionVersion: number }[]; media: Medium[] }
type ExtensionChange = { id: string; activity_id: string; responsible_name: string; old_deadline: string; new_deadline: string; reason: string; authorized_by_name: string; created_at: string }
type State = { documentState: string; coordinator: boolean; requiredMedia: number; coveredMedia: number; completedActivities: number; maxFileBytes: number; activities: Execution[] }

export function ExecutionWorkspace({ targetDocId, onBack, onDirtyChange }: { targetDocId: string; onBack: () => void; onDirtyChange: (dirty: boolean) => void }) {
  const [state, setState] = useState<State | null>(null); const [reload, setReload] = useState(0); const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false); const [dirty, setDirty] = useState<string[]>([])
  const [formalDirty, setFormalDirty] = useState(false)
  const [loadFailed, setLoadFailed] = useState(false)
  const [history, setHistory] = useState<ExtensionChange[]>([])
  const path = '/work-plans/' + targetDocId + '/execution'
  useEffect(() => { onDirtyChange(dirty.length > 0 || formalDirty); return () => onDirtyChange(false) }, [dirty, formalDirty, onDirtyChange])
  useEffect(() => {
    const prevent = (event: BeforeUnloadEvent) => { if (dirty.length > 0 || formalDirty) { event.preventDefault(); event.returnValue = '' } }
    window.addEventListener('beforeunload', prevent); return () => window.removeEventListener('beforeunload', prevent)
  }, [dirty, formalDirty])
  useEffect(() => { let active = true; Promise.all([api<State>(path), api<ExtensionChange[]>(path + '/extensions/history')]).then(([s, h]) => { if (active) { setState(s); setHistory(h); setLoadFailed(false) } }).catch(e => { if (active) { setError(e.message); setLoadFailed(true) } }); return () => { active = false } }, [path, reload])
  function mark(key: string) { setDirty(d => d.includes(key) ? d : [...d, key]) }
  async function extend(event: FormEvent<HTMLFormElement>, activity: Execution) {
    event.preventDefault(); const form = event.currentTarget, data = Object.fromEntries(new FormData(form)); setBusy(true); setError('')
    try { await api(path + '/activities/' + activity.id + '/extension', 'PUT', { ...data, rowVersion: activity.responsible.find(r => r.id === data.userId)?.extensionVersion ?? -1 }); form.reset(); setDirty(d => d.filter(k => k !== 'extension:' + activity.id)); setNotice('Extensión registrada.'); setReload(r => r + 1) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo registrar.') } finally { setBusy(false) }
  }
  return <section className="execution-workspace"><button type="button" className="secondary" disabled={busy} onClick={async () => { if ((!dirty.length && !formalDirty) || (await confirmAction('Hay cambios sin guardar. ¿Salir?'))) onBack() }}>Volver al Plan</button><div className="execution-intro"><span className="section-icon"><Icon name="document" size={30} /></span><div><span className="section-kicker review-kicker">SEGUIMIENTO DE EJECUCIÓN</span><h1>Actividades y evidencias</h1><p className="muted">Una evidencia vigente por medio: PDF privado o enlace HTTPS, según la matriz del Plan. La cobertura documental no representa por sí sola el porcentaje de ejecución física.</p></div></div>{error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    <FormalSubmission targetDocId={targetDocId} onDirtyChange={setFormalDirty} onRegistered={() => setReload(value => value + 1)} />
    {!state ? loadFailed ? <div className="notice"><p>No se pudo cargar la ejecución.</p><button type="button" className="secondary" onClick={() => { setError(''); setLoadFailed(false); setReload(value => value + 1) }}>Reintentar carga de ejecución</button></div> : <p role="status">Cargando ejecución…</p> : <><div className="organization-metrics"><div><strong>{state.coveredMedia}/{state.requiredMedia}</strong><span>Medios con evidencia vigente</span></div><div><strong>{state.completedActivities}/{state.activities.length}</strong><span>Actividades con evidencias completas</span></div><div><strong>{Math.round(state.maxFileBytes / 1048576)} MB</strong><span>Límite técnico por PDF</span></div></div>{state.documentState !== 'APPROVED' && <p className="field-help">La carga de evidencias se habilita cuando el Plan está aprobado.</p>}
    {state.activities.map(a => <section className="card execution-activity" key={a.id}><div className="section-heading"><span className="section-icon"><Icon name="document" /></span><div><span className="section-kicker">ACTIVIDAD PLANIFICADA</span><h2>{a.title}</h2></div></div><div className="review-summary"><span className={'status-badge ' + (a.complete ? 'active' : '')}>{a.complete ? 'Evidencias completas' : 'Evidencias pendientes'}</span><span className="muted">Plazo de carga: {a.deadline} · 23:59, hora institucional</span></div>
      <div className="evidence-grid">{a.media.map(m => <EvidenceMedium key={m.index} path={path} activityId={a.id} medium={m} canUpload={a.canUpload} onDirty={value => { if (value) mark(a.id + ':' + m.index); else setDirty(d => d.filter(k => k !== a.id + ':' + m.index)) }} onSaved={() => { setNotice('Evidencia guardada. Las versiones anteriores se conservan.'); setReload(r => r + 1) }} onError={setError} />)}</div>
      {history.some(h => h.activity_id === a.id) && <details className="scope-history"><summary>Historial de prórrogas individuales</summary><ol className="scope-timeline">{history.filter(h => h.activity_id === a.id).map(h => <li key={h.id}><strong>{h.responsible_name}: {h.old_deadline.slice(0, 10).split('-').reverse().join('/')} → {h.new_deadline.slice(0, 10).split('-').reverse().join('/')}</strong><span>{h.reason}</span><small>{h.authorized_by_name} · {new Date(h.created_at).toLocaleString('es-EC')}</small></li>)}</ol></details>}
      {state.coordinator && state.documentState === 'APPROVED' && <ActionForm label="Conceder extensión de plazo" disabled={busy} dirty={dirty.includes('extension:' + a.id)} onCancel={() => setDirty(keys => keys.filter(k => k !== 'extension:' + a.id))}><form onChange={() => mark('extension:' + a.id)} onSubmit={e => extend(e, a)}><fieldset disabled={busy}><label>Responsable de la prórroga<select name="userId" required><option value="">Seleccione una persona</option>{a.responsible.map(r => <option key={r.id} value={r.id}>{r.name} · Plazo vigente: {r.deadline}</option>)}</select></label><div className="grid"><label>Nuevo plazo<input type="date" name="endsOn" required /></label><label>Motivo<input name="reason" required maxLength={1000} /></label></div><p className="field-help">Máximo acumulado de días hábiles configurado desde el vencimiento original; no cuentan fines de semana ni feriados registrados.</p><button>Registrar extensión</button></fieldset></form></ActionForm>}
    </section>)}</>}
  </section>
}
