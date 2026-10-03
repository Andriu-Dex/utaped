import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, ApiError } from '../shared/api'
import { categoryLabels, type Activity, type Matrix } from './matrixTypes'

function selection(matrix: Matrix) { return { source: matrix.source, activities: matrix.activities } }
export function MatrixEditor({ targetDocId, onBack, onDirtyChange }: { targetDocId: string; onBack: () => Promise<void>; onDirtyChange: (dirty: boolean) => void }) {
  const [saved, setSaved] = useState<Matrix | null>(null); const [draft, setDraft] = useState<ReturnType<typeof selection> | null>(null)
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const [conflict, setConflict] = useState(false); const [latest, setLatest] = useState<Matrix | null>(null)
  const [catalogId, setCatalogId] = useState(''); const [otherTitle, setOtherTitle] = useState('')
  const [attempted, setAttempted] = useState(false)
  const formRef = useRef<HTMLFormElement>(null)
  const dirty = !!saved && !!draft && JSON.stringify(selection(saved)) !== JSON.stringify(draft)
  useEffect(() => { onDirtyChange(dirty) }, [dirty, onDirtyChange])
  useEffect(() => () => onDirtyChange(false), [onDirtyChange])
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => { if (dirty) { e.preventDefault(); e.returnValue = '' } }
    window.addEventListener('beforeunload', handler); return () => window.removeEventListener('beforeunload', handler)
  }, [dirty])
  useEffect(() => {
    let cancelled = false
    api<Matrix>('/work-plans/' + targetDocId + '/matrix').then(m => { if (!cancelled) { setSaved(m); setDraft(selection(m)) } }).catch(e => { if (!cancelled) setError(e.message) })
    return () => { cancelled = true }
  }, [targetDocId])
  function update(id: string, patch: Partial<Activity>) { setDraft(d => d && ({ ...d, activities: d.activities.map(a => a.id === id ? { ...a, ...patch } : a) })); setNotice('') }
  function add() {
    if (!saved || !draft) return
    const definition = saved.definitions.find(d => d.id === catalogId)
    if (!definition && !otherTitle.trim()) { setError('Escriba el título de la actividad Otra.'); return }
    const activity: Activity = { id: crypto.randomUUID(), catalogId: definition?.id ?? null, title: definition?.title ?? otherTitle.trim(), category: definition?.category ?? 'OTHER', mandatory: definition?.mandatory ?? false, startsOn: null, endsOn: null, responsibleIds: [], collective: false, resources: [], means: [] }
    setDraft({ ...draft, activities: [...draft.activities, activity] }); setCatalogId(''); setOtherTitle(''); setError(''); setNotice('')
  }
  function toggleChoice(row: Activity, key: 'resources' | 'means', id: string | null, checked: boolean) {
    const values = row[key].filter(c => c.catalogId !== id)
    if (checked) values.push({ catalogId: id, other: id === null ? '' : null, label: null })
    update(row.id, { [key]: values })
  }
  async function save(e: FormEvent) {
    e.preventDefault(); if (!saved || !draft) return
    setAttempted(true); setError(''); setNotice('')
    const emptyOther = formRef.current?.querySelector<HTMLInputElement>('input[data-other="true"][aria-invalid="true"]')
    if (emptyOther) { setError('Complete la descripción de Otro.'); emptyOther.focus(); return }
    if (draft.activities.some(a => !a.responsibleIds.length || !a.resources.length || !a.means.length)) { setError('Cada actividad requiere al menos un responsable, un recurso y un medio de verificación.'); return }
    setBusy(true)
    try {
      const m = await api<Matrix>('/work-plans/' + targetDocId + '/matrix', 'PUT', { ...draft, rowVersion: saved.rowVersion })
      setSaved(m); setDraft(selection(m)); setNotice('Matriz guardada en el servidor.'); setAttempted(false)
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.'); if (e instanceof ApiError && e.status === 409) setConflict(true) }
    finally { setBusy(false) }
  }
  async function back() {
    if (dirty && !window.confirm('Hay cambios sin guardar en la matriz. ¿Salir y descartarlos?')) return
    setBusy(true)
    try { await onBack() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo volver.'); setBusy(false) }
  }
  async function compare() { try { setLatest(await api<Matrix>('/work-plans/' + targetDocId + '/matrix')) } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo consultar.') } }
  function reload() {
    if (!latest || !window.confirm('¿Descartar cambios locales y cargar la matriz actual del servidor?')) return
    setSaved(latest); setDraft(selection(latest)); setLatest(null); setConflict(false); setAttempted(false); setError(''); setNotice('Versión del servidor cargada.')
  }
  if (!saved || !draft) return <section><button className="secondary" onClick={back}>Volver al Plan</button>{error ? <p role="alert" className="error">{error}</p> : <p role="status">Cargando matriz…</p>}</section>
  function choices(row: Activity, key: 'resources' | 'means', title: string) {
    if (!saved) return null
    const available = saved.catalogs.filter(c => c.kind === (key === 'resources' ? 'RESOURCE' : 'MEANS') && (c.active || row[key].some(v => v.catalogId === c.id)))
    const other = row[key].find(c => c.catalogId === null)
    return <fieldset className="choice-group"><legend>{title}</legend>{available.map(c => <label className="check" key={c.id}><input type="checkbox" checked={row[key].some(v => v.catalogId === c.id)} onChange={e => toggleChoice(row, key, c.id, e.target.checked)} />{row[key].find(v => v.catalogId === c.id)?.label || c.label}{!c.active ? ' (inactivo)' : ''}</label>)}
      <label className="check"><input type="checkbox" checked={!!other} onChange={e => toggleChoice(row, key, null, e.target.checked)} />Otro</label>
      {other && <><label>Descripción de Otro ({title})<input data-other="true" aria-invalid={!other.other?.trim()} aria-describedby={attempted && !other.other?.trim() ? `${row.id}-${key}-error` : undefined} maxLength={500} value={other.other ?? ''} onChange={e => update(row.id, { [key]: row[key].map(c => c.catalogId === null ? { ...c, other: e.target.value, label: null } : c) })} /></label>{attempted && !other.other?.trim() && <span className="error" id={`${row.id}-${key}-error`}>Escriba una descripción.</span>}</>}
      <p>{row[key].filter(c => c.catalogId !== null || c.other?.trim()).length} seleccionados</p>
    </fieldset>
  }
  return <section><button className="secondary" disabled={busy} onClick={back}>Volver al Plan</button><h1>Actividades y matriz T1</h1><p role="status">{dirty ? 'Tiene cambios sin guardar.' : 'Sin cambios pendientes.'}</p>
    {!saved.editable && <p className="notice">Solo lectura: ventana de elaboración cerrada.</p>}
    <p>Elaborado por: {saved.elaboratedBy}</p>
    <form ref={formRef} onSubmit={save}><fieldset disabled={busy || !saved.editable}><label>Fuente de la matriz<input maxLength={500} value={draft.source} onChange={e => { setDraft({ ...draft, source: e.target.value }); setNotice('') }} /></label>
      <div className="card"><h2>Seleccionar actividades</h2><label>Actividad del catálogo<select value={catalogId} onChange={e => setCatalogId(e.target.value)}><option value="">Otra actividad</option>{saved.definitions.filter(d => d.active && !draft.activities.some(a => a.catalogId === d.id)).map(d => <option key={d.id} value={d.id}>{categoryLabels[d.category]} · {d.title}</option>)}</select></label>
        {!catalogId && <label>Título de Otra actividad<input maxLength={500} value={otherTitle} onChange={e => setOtherTitle(e.target.value)} /></label>}<button type="button" disabled={draft.activities.length >= 100} onClick={add}>Agregar actividad</button></div>
      {draft.activities.length === 0 && <p>No hay actividades seleccionadas.</p>}
      {draft.activities.map((row, index) => <section className="card" key={row.id} aria-label={`Actividad ${index + 1}`}><h2>{index + 1}. {row.title}</h2><p>{categoryLabels[row.category]}{row.mandatory ? ' · Obligatoria' : ''}</p>
        {!row.catalogId && <label>Título de la actividad<input required maxLength={500} value={row.title} onChange={e => update(row.id, { title: e.target.value })} /></label>}
        {!row.mandatory && <button type="button" className="secondary" title="Quitar actividad" aria-label={'Quitar ' + row.title} onClick={() => { setDraft({ ...draft, activities: draft.activities.filter(a => a.id !== row.id) }); setNotice('') }}>Quitar actividad</button>}
        <div className="grid"><label>Desde<input type="date" required min={saved.periodStartsOn} max={saved.periodEndsOn} value={row.startsOn ?? ''} onChange={e => update(row.id, { startsOn: e.target.value })} /></label><label>Hasta<input type="date" required min={row.startsOn || saved.periodStartsOn} max={saved.periodEndsOn} value={row.endsOn ?? ''} onChange={e => update(row.id, { endsOn: e.target.value })} /></label></div>
        <fieldset className="choice-group"><legend>Responsables</legend><button type="button" className="secondary" onClick={() => update(row.id, { responsibleIds: saved.members.map(m => m.id), collective: false })}>Seleccionar todos</button>
          {saved.members.map(member => <label className="check" key={member.id}><input type="checkbox" checked={row.responsibleIds.includes(member.id)} onChange={e => update(row.id, { responsibleIds: e.target.checked ? [...row.responsibleIds, member.id] : row.responsibleIds.filter(id => id !== member.id), collective: false })} />{member.name}</label>)}
          {row.responsibleIds.some(id => !saved.members.some(m => m.id === id)) && <p className="error">Hay responsables que ya no están disponibles. Vuelva a seleccionarlos.</p>}
          <label className="check"><input type="checkbox" disabled={!saved.collectiveLabel || row.responsibleIds.length !== saved.members.length || !saved.members.length} checked={row.collective} onChange={e => update(row.id, { collective: e.target.checked })} />Usar denominación colectiva{saved.collectiveLabel ? ': ' + saved.collectiveLabel : ' (sin configurar)'}</label>
          <p>{row.responsibleIds.length} responsables seleccionados</p>
        </fieldset>
        <div className="grid">{choices(row, 'resources', 'Recursos')}{choices(row, 'means', 'Medios de verificación')}</div>
      </section>)}
      <button disabled={conflict}>{busy ? 'Guardando…' : 'Guardar matriz'}</button></fieldset></form>
    {!!draft.activities.length && <section className="card"><h2>Resumen de la matriz</h2><div className="table-scroll"><table><thead><tr><th>Actividad</th><th>Desde</th><th>Hasta</th><th>Responsables</th><th>Recursos</th><th>Medios de verificación</th></tr></thead><tbody>{draft.activities.map(a => <tr key={a.id}><td>{a.title}</td><td>{a.startsOn?.split('-').reverse().join('/') || 'Pendiente'}</td><td>{a.endsOn?.split('-').reverse().join('/') || 'Pendiente'}</td><td>{a.collective ? saved.collectiveLabel : a.responsibleIds.map(id => saved.members.find(m => m.id === id)?.name ?? 'Integrante no disponible').join(', ')}</td><td>{a.resources.map(c => c.label || c.other || saved.catalogs.find(v => v.id === c.catalogId)?.label).filter(Boolean).join(', ')}</td><td>{a.means.map(c => c.label || c.other || saved.catalogs.find(v => v.id === c.catalogId)?.label).filter(Boolean).join(', ')}</td></tr>)}</tbody></table></div></section>}
    {error && <p className="error" role="alert">{error}</p>}{notice && <p className="notice" role="status">{notice}</p>}
    {conflict && <section className="conflict"><p>Los cambios locales se conservan. Revise la matriz actual antes de descartar sus cambios.</p><button className="secondary" onClick={compare}>Consultar matriz actual</button>{latest && <><h2>Matriz actual del servidor</h2><p>Fuente: {latest.source || 'Sin fuente'}</p>{latest.activities.map(a => <section key={a.id}><h3>{a.title}</h3><dl><dt>Fechas</dt><dd>{a.startsOn?.split('-').reverse().join('/') || 'Sin inicio'} — {a.endsOn?.split('-').reverse().join('/') || 'Sin fin'}</dd><dt>Responsables</dt><dd>{a.collective ? latest.collectiveLabel : a.responsibleIds.map(id => latest.members.find(m => m.id === id)?.name ?? 'Integrante no disponible').join(', ')}</dd><dt>Recursos</dt><dd>{a.resources.map(c => c.label).join(', ')}</dd><dt>Medios</dt><dd>{a.means.map(c => c.label).join(', ')}</dd></dl></section>)}<button className="secondary" onClick={reload}>Descartar cambios y cargar matriz actual</button></>}</section>}
  </section>
}
