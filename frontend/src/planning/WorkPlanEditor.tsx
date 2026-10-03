import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, ApiError } from '../shared/api'
import type { WorkPlan } from './types'

type DraftFields = Pick<WorkPlan, 'title' | 'institutionalUnit' | 'career' | 'justification' | 'objective'>
function fields(plan: WorkPlan): DraftFields {
  return { title: plan.title, institutionalUnit: plan.institutionalUnit, career: plan.career, justification: plan.justification, objective: plan.objective }
}

export function WorkPlanEditor({ initial, onBack, onDirtyChange }: { initial: WorkPlan; onBack: () => void; onDirtyChange: (dirty: boolean) => void }) {
  const [plan, setPlan] = useState(initial)
  const [draft, setDraft] = useState(() => fields(initial))
  const [section, setSection] = useState<'general' | 'content'>('general')
  const [error, setError] = useState(''); const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false); const [conflict, setConflict] = useState(false)
  const [latest, setLatest] = useState<WorkPlan | null>(null)
  const dirty = JSON.stringify(draft) !== JSON.stringify(fields(plan))
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
  return <section>
    <button type="button" className="secondary" onClick={back} disabled={busy}>Volver a documentos</button>
    <h1>{plan.title}</h1><p>Plan de Trabajo T1 · Borrador · Versión formal {plan.formalVersion}</p>
    <p role="status">{dirty ? 'Tiene cambios sin guardar.' : 'Sin cambios pendientes.'}</p>
    <div className="card"><dl className="plan-metadata"><div><dt>Elaborador</dt><dd>{plan.teacherName}</dd></div><div><dt>Grupo</dt><dd>{plan.groupName}</dd></div><div><dt>Período</dt><dd>{plan.periodName}</dd></div><div><dt>Fecha de elaboración</dt><dd>{plan.preparationDate.split('-').reverse().join('/')}</dd></div></dl>
      {!plan.editable && <p className="notice">Solo lectura: la ventana de elaboración está cerrada.</p>}
      <nav aria-label="Secciones del borrador"><button type="button" aria-current={section === 'general' ? 'page' : undefined} onClick={() => setSection('general')}>Información general</button><button type="button" aria-current={section === 'content' ? 'page' : undefined} onClick={() => setSection('content')}>Contenido</button></nav>
      <form onSubmit={save}><fieldset disabled={!plan.editable || busy}>
        {section === 'general' ? <>
          <label>Título del Plan<input required maxLength={200} value={draft.title} onChange={e => change('title', e.target.value)} /></label>
          <label>Unidad institucional<input maxLength={200} value={draft.institutionalUnit} onChange={e => change('institutionalUnit', e.target.value)} /></label>
          <label>Carrera, cuando corresponda<input maxLength={200} value={draft.career} onChange={e => change('career', e.target.value)} /></label>
        </> : <>
          <label>Justificación<textarea rows={9} maxLength={50000} value={draft.justification} onChange={e => change('justification', e.target.value)} /></label>
          <label>Objetivo<textarea rows={6} maxLength={50000} value={draft.objective} onChange={e => change('objective', e.target.value)} /></label>
          <p>El borrador admite contenido incompleto. La validación de finalización se incorporará con el flujo documental.</p>
        </>}
        <button disabled={!dirty || conflict}>{busy ? 'Guardando…' : 'Guardar borrador'}</button>
      </fieldset></form>
      {error && <p role="alert" className="error">{error}</p>}{notice && <p role="status" className="notice">{notice}</p>}
      {conflict && <div className="conflict"><p>Sus cambios locales se conservan. Consulte la versión del servidor antes de decidir cómo resolver el conflicto.</p><button type="button" className="secondary" onClick={compare}>Consultar versión actual</button>
        {latest && <><h3>Contenido actual del servidor</h3><dl><dt>Título</dt><dd>{latest.title}</dd><dt>Unidad institucional</dt><dd>{latest.institutionalUnit}</dd><dt>Carrera</dt><dd>{latest.career}</dd><dt>Justificación</dt><dd className="preserve-text">{latest.justification || 'Sin contenido'}</dd><dt>Objetivo</dt><dd className="preserve-text">{latest.objective || 'Sin contenido'}</dd></dl><button type="button" className="secondary" onClick={reload}>Descartar cambios y cargar versión actual</button></>}
      </div>}
    </div>
  </section>
}
