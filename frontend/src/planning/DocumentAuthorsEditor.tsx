import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../shared/api'
import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { Icon } from '../shared/Icon'

type Author = { id: string; name: string; position: string }
type State = { rowVersion: number; editable: boolean; authors: Author[]; candidates: Author[] }
export function DocumentAuthorsEditor({ targetDocId, version, disabled, onDirtyChange, onSaved }: {
  targetDocId: string; version: number; disabled: boolean; onDirtyChange: (dirty: boolean) => void; onSaved: () => Promise<void>
}) {
  const [state, setState] = useState<State | null>(null), [selected, setSelected] = useState<string[]>([])
  const [error, setError] = useState(''), [notice, setNotice] = useState(''), [busy, setBusy] = useState(false)
  const principal = state?.authors[0]
  const dirty = !!state && JSON.stringify(selected) !== JSON.stringify(state.authors.slice(1).map(person => person.id))
  useEffect(() => { onDirtyChange(dirty); return () => onDirtyChange(false) }, [dirty, onDirtyChange])
  useEffect(() => {
    let active = true
    api<State>('/work-plans/' + targetDocId + '/authors').then(value => { if (active) { setState(value); setSelected(value.authors.slice(1).map(person => person.id)) } }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [targetDocId, version])
  async function save(event: FormEvent) {
    event.preventDefault(); if (!state || busy) return
    setBusy(true); setError('')
    try { const value = await api<State>('/work-plans/' + targetDocId + '/authors', 'PUT', { rowVersion: state.rowVersion, coauthorIds: selected }); setState(value); setSelected(value.authors.slice(1).map(person => person.id)); await onSaved(); setNotice('Elaboradores guardados. Cada persona firmará con su propia identidad.') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudieron guardar los elaboradores.') }
    finally { setBusy(false) }
  }
  return <section className="card document-authors" aria-label="Elaboradores del documento">
    <div className="section-heading"><span className="section-icon"><Icon name="users" /></span><div><span className="section-kicker">AUTORÍA DOCUMENTAL</span><h2>Elaboradores y firmas</h2></div></div>
    <p className="muted">El responsable principal conserva la edición. Los coautores firman el mismo documento, en el orden indicado. La primera firma congela su contenido y la última lo envía a revisión.</p>
    {state && <ol className="scope-timeline">{state.authors.map((person, index) => <li key={person.id}><strong>{person.name}</strong><span> · {person.position || 'Cargo sin asignar'}{index === 0 ? ' · Editor principal' : ' · Coautor firmante'}</span></li>)}</ol>}
    {error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    {state?.editable && <ActionForm key={state.rowVersion} label="Configurar elaboradores" dirty={dirty} disabled={disabled || busy} onCancel={() => setSelected(state.authors.slice(1).map(person => person.id))}>
      <form onSubmit={event => void save(event)}><fieldset disabled={busy || disabled}><legend>Coautores del ámbito</legend><p className="field-help">La selección conserva su orden. El cargo procede de la pertenencia institucional configurada por Administración.</p><div className="choice-chip-grid">
        {state.candidates.filter(person => person.id !== principal?.id).map(person => {
          const isSelected = selected.includes(person.id)
          return <label className={'check choice-chip' + (isSelected ? ' is-selected' : '')} key={person.id}><input type="checkbox" checked={isSelected} onChange={event => setSelected(current => event.target.checked ? [...current, person.id] : current.filter(id => id !== person.id))} />{person.name}</label>
        })}
      </div>
      <div className="action-form-actions">
        <button type="button" className="secondary" onClick={() => setSelected([])}>Conservar solo al responsable principal</button>
        <button disabled={!dirty}>Guardar elaboradores</button>
      </div>
      </fieldset></form>
    </ActionForm>}
  </section>
}
