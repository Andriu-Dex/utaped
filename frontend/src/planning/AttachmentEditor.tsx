import { FileInput } from '../shared/FileInput'
import { DocumentReadOnlyNotice } from './DocumentReadOnlyNotice'
import { StepPagination } from './StepPagination'
import { usePlanStepGuard } from './PlanStepNavigation'
import { Icon } from '../shared/Icon'
import { confirmAction } from '../shared/confirm-action'
import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api } from '../shared/api'

interface Attachment { id: string; label: string; title: string; description: string; originalName: string; sourceName?: string; sourceSizeBytes?: number; sizeBytes: number; pageCount: number }
interface State { rowVersion: number; editable: boolean; readOnlyReason?: string | null; enabled: boolean; privacyNoticeEnabled: boolean; maxFileBytes: number; maxTotalBytes: number; maxAttachments: number; items: Attachment[] }
export function AttachmentEditor({ targetDocId, onBack, onDirtyChange, onPrevious, onNext, documentType = 'T1', previousLabel = 'Actividades y matriz', nextLabel = 'Preparación y previsualización', onSaved }: { onSaved?: () => Promise<void>; documentType?: 'T1' | 'T2' | 'T3'; previousLabel?: string; nextLabel?: string; onPrevious?: () => Promise<void>; onNext?: () => Promise<void>; targetDocId: string; onBack: () => void; onDirtyChange: (dirty: boolean) => void }) {
  const path = '/work-plans/' + targetDocId + '/attachments'
  const documentLabel = documentType === 'T1' ? 'Plan' : 'Informe'
  const heading = documentType === 'T1' ? 'Anexos del Plan T1' : documentType === 'T2' ? 'Anexos del Informe final T2' : 'Anexos del Informe de ejecución'
  const [state, setState] = useState<State | null>(null)
  const [title, setTitle] = useState(''); const [file, setFile] = useState<File | null>(null)
  const [kind, setKind] = useState<'FILE' | 'URL' | 'TABLE'>('FILE'), [url, setUrl] = useState(''), [cells, setCells] = useState([['', ''], ['', '']])
  const [editing, setEditing] = useState<Attachment | null>(null)
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const [loadFailed, setLoadFailed] = useState(false), [reload, setReload] = useState(0)
  const requestKey = useRef(crypto.randomUUID()); const fileInput = useRef<HTMLInputElement>(null)
  const editButton = useRef<HTMLButtonElement | null>(null)
  const dirty = !!title || !!file || kind !== 'FILE' || editing !== null
  useEffect(() => { let active = true; api<State>(path).then(value => { if (active) { setState(value); setLoadFailed(false) } }).catch(e => { if (active) { setError(e.message); setLoadFailed(true) } }); return () => { active = false } }, [path, reload])
  useEffect(() => { onDirtyChange(dirty); const prevent = (e: BeforeUnloadEvent) => { if (dirty) { e.preventDefault(); e.returnValue = '' } }; window.addEventListener('beforeunload', prevent); return () => { window.removeEventListener('beforeunload', prevent); onDirtyChange(false) } }, [dirty, onDirtyChange])
  const embedded = usePlanStepGuard(async () => {
    if (busy) return false
    return !dirty || await confirmAction('¿Descartar los cambios sin guardar antes de cambiar de paso?')
  }, busy || (!state && !loadFailed))
  async function navigate(action: () => Promise<void>) {
    if (dirty && !(await confirmAction('¿Descartar los cambios sin guardar antes de cambiar de paso?'))) return
    setBusy(true); setError('')
    try { await action() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo cambiar de paso.') } finally { setBusy(false) }
  }
  async function mutate(action: () => Promise<State>, message: string) {
    setBusy(true); setError(''); setNotice('')
    try { setState(await action()); await onSaved?.(); setNotice(message); return true } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo completar la operación.'); return false } finally { setBusy(false) }
  }
  async function upload(e: FormEvent) {
    e.preventDefault(); if (!state) return
    if (kind !== 'FILE') {
      if (await mutate(() => api<State>(path + '/generated', 'POST', { rowVersion: state.rowVersion, requestKey: requestKey.current, title, kind, url: kind === 'URL' ? url : null, rows: kind === 'TABLE' ? cells : null }), 'Anexo agregado.')) { setTitle(''); setUrl(''); setCells([['', ''], ['', '']]); setKind('FILE'); requestKey.current = crypto.randomUUID() }
      return
    }
    if (!file) return
    if (file.size > state.maxFileBytes) { setError('El archivo supera el límite permitido.'); return }
    const data = new FormData(); data.append('file', file)
    const query = new URLSearchParams({ rowVersion: String(state.rowVersion), requestKey: requestKey.current, title })
    if (await mutate(() => api<State>(path + '?' + query, 'POST', data), 'Anexo agregado.')) { setTitle(''); setFile(null); requestKey.current = crypto.randomUUID(); if (fileInput.current) { fileInput.current.value = ''; fileInput.current.dispatchEvent(new Event('change', { bubbles: true })) } }
  }
  async function saveEdit(e: FormEvent) { e.preventDefault(); if (!state || !editing) return; if (await mutate(() => api<State>(path + '/' + editing.id, 'PUT', { rowVersion: state.rowVersion, title: editing.title, description: editing.description }), 'Anexo actualizado.')) setEditing(null) }
  async function cancelEdit() {
    const original = state?.items.find(item => item.id === editing?.id)
    if (editing && (editing.title !== original?.title || editing.description !== original?.description) && !(await confirmAction('¿Descartar los cambios de este anexo?'))) return
    setEditing(null); requestAnimationFrame(() => editButton.current?.focus())
  }
  function move(index: number, delta: number) { if (!state) return; const ids = state.items.map(a => a.id); [ids[index], ids[index + delta]] = [ids[index + delta], ids[index]]; void mutate(() => api<State>(path + '/order', 'PUT', { rowVersion: state.rowVersion, attachmentIds: ids }), 'Orden actualizado.') }
  return <section className={embedded ? 'plan-step-panel' : undefined}>
    {!embedded ? (
      <header className="plan-wizard-header">
        <span className="section-kicker review-kicker">{documentType === 'T1' ? 'PASO 4 DE 5 · ANEXOS Y OPCIONES' : 'ANEXOS Y OPCIONES · ' + documentType}</span>
        <div className="plan-header-title-row">
          <button
            type="button"
            className="plan-back-btn"
            disabled={busy}
            onClick={async () => { if (!dirty || (await confirmAction('¿Descartar los cambios sin guardar?'))) onBack() }}
            title={'Volver al ' + documentLabel}
            aria-label={'Volver al ' + documentLabel}
          >
            <Icon name="arrowLeft" size={20} />
            <span className="sr-only">{'Volver al ' + documentLabel}</span>
          </button>
          <h1>{heading}</h1>
        </div>
      </header>
    ) : (
      <>
        <span className="section-kicker review-kicker">{documentType === 'T1' ? 'PASO 4 DE 5 · ANEXOS Y OPCIONES' : 'ANEXOS Y OPCIONES · ' + documentType}</span>
        <h2 className="plan-step-title">{heading}</h2>
      </>
    )}
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    {!state ? loadFailed ? <div className="notice"><p>No se pudieron cargar los anexos.</p><button type="button" className="secondary" onClick={() => { setError(''); setLoadFailed(false); setReload(value => value + 1) }}>Reintentar carga de anexos</button></div> : <p role="status">Cargando anexos…</p> : <div className="card">
      <p>Anexos PDF o imágenes PNG/JPEG. Máximo {state.maxAttachments} anexos, {Math.floor(state.maxFileBytes / 1048576)} MB por archivo y {Math.floor(state.maxTotalBytes / 1048576)} MB en total. Límites técnicos configurables.</p>
      <DocumentReadOnlyNotice document={state} />
      <fieldset disabled={busy || !state.editable || dirty}><legend>Opciones del documento</legend>
        <label>¿El {documentLabel} tiene anexos?<select aria-label={'¿El ' + documentLabel + ' tiene anexos?'} value={String(state.enabled)} onChange={e => void mutate(() => api<State>(path + '/settings', 'PUT', { rowVersion: state.rowVersion, enabled: e.target.value === 'true', privacyNoticeEnabled: state.privacyNoticeEnabled }), 'Opciones guardadas.')}><option value="false">No</option><option value="true">Sí</option></select></label>
      </fieldset>
      {state.enabled && <ActionForm label="Agregar anexo" dirty={!!title || !!file || kind !== 'FILE'} disabled={busy || !state.editable || editing !== null} onCancel={() => { setTitle(''); setFile(null); setKind('FILE'); setUrl(''); setCells([['', ''], ['', '']]); requestKey.current = crypto.randomUUID() }}><form onSubmit={upload}><fieldset disabled={busy || !state.editable || editing !== null}><legend>Agregar anexo</legend><label>Título del anexo<input required maxLength={200} value={title} onChange={e => setTitle(e.target.value)} /></label><label>Tipo de anexo<select value={kind} onChange={e => { setKind(e.currentTarget.value as typeof kind); setFile(null); requestKey.current = crypto.randomUUID() }}><option value="FILE">Archivo PDF o imagen</option><option value="URL">Enlace externo</option><option value="TABLE">Tabla</option></select></label>{kind === 'FILE' && <label>Archivo del anexo (PDF o imagen)<FileInput ref={fileInput} required accept="application/pdf,.pdf,image/png,.png,image/jpeg,.jpg,.jpeg" onChange={e => { setFile(e.target.files?.[0] ?? null); requestKey.current = crypto.randomUUID() }} /></label>}{kind === 'URL' && <label>Enlace HTTPS<input type="url" required maxLength={2048} value={url} onChange={e => setUrl(e.currentTarget.value)} placeholder="https://" /></label>}{kind === 'TABLE' && <><div className="table-scroll"><table><thead><tr>{cells[0].map((_, column) => <th key={column}>Columna {column + 1}</th>)}</tr></thead><tbody>{cells.map((row, rowIndex) => <tr key={rowIndex}>{row.map((cell, column) => <td key={column}><input aria-label={(rowIndex ? 'Fila ' + rowIndex : 'Encabezado') + ', columna ' + (column + 1)} required={!rowIndex} maxLength={rowIndex ? 200 : 80} value={cell} onChange={e => { const value = e.currentTarget.value; setCells(rows => rows.map((r, i) => i === rowIndex ? r.map((c, j) => j === column ? value : c) : r)) }} /></td>)}<td>{rowIndex > 0 && <button type="button" className="secondary" title="Quitar fila" aria-label={'Quitar fila ' + rowIndex} disabled={cells.length <= 2} onClick={() => setCells(rows => rows.filter((_, i) => i !== rowIndex))}><Icon name="trash" size={16} /></button>}</td></tr>)}</tbody></table></div><div className="review-actions"><button type="button" className="secondary" disabled={cells.length >= 100} onClick={() => setCells(rows => [...rows, rows[0].map(() => '')])}>Agregar fila</button><button type="button" className="secondary" disabled={cells[0].length >= 8} onClick={() => setCells(rows => rows.map(row => [...row, '']))}>Agregar columna</button><button type="button" className="secondary" disabled={cells[0].length <= 1} onClick={async () => { if (await confirmAction('¿Quitar la última columna de la tabla?')) setCells(rows => rows.map(row => row.slice(0, -1))) }}>Quitar última columna</button></div></>}<button disabled={state.items.length >= state.maxAttachments}>Agregar anexo</button></fieldset></form></ActionForm>}
      {state.items.map((a, index) => <article className="card" key={a.id} aria-label={a.label}><h2>{a.label}. {a.title}</h2><p>{a.sourceName ?? a.originalName} · {a.pageCount} páginas · {((a.sourceSizeBytes ?? a.sizeBytes) / 1024).toFixed(1)} KB</p><p className="preserve-text">{a.description}</p>
        <div className="attachment-actions" role="group" aria-label={'Acciones de ' + a.label}><a className="secondary attachment-action" href={'/api' + path + '/' + a.id + '/original'} title={'Descargar original de ' + a.label} aria-label={'Descargar original de ' + a.label}><Icon name="download" size={18} /></a>
        <button className="secondary attachment-action" title={'Editar ' + a.label} aria-label={'Editar ' + a.label} disabled={busy || !state.editable || dirty} onClick={event => { editButton.current = event.currentTarget; setEditing({ ...a }) }}><Icon name="edit" size={18} /></button>{' '}
        <button className="secondary attachment-action" title={'Subir ' + a.label} aria-label={'Subir ' + a.label} disabled={busy || !state.editable || dirty || index === 0} onClick={() => move(index, -1)}><Icon name="arrowUp" size={18} /></button>{' '}
        <button className="secondary attachment-action" title={'Bajar ' + a.label} aria-label={'Bajar ' + a.label} disabled={busy || !state.editable || dirty || index === state.items.length - 1} onClick={() => move(index, 1)}><Icon name="arrowDown" size={18} /></button>{' '}
        <button className="secondary attachment-action attachment-remove" title={'Quitar ' + a.label} aria-label={'Quitar ' + a.label} disabled={busy || !state.editable || dirty} onClick={async () => { if ((await confirmAction('¿Quitar este anexo del borrador? Las previsualizaciones anteriores se conservarán.'))) void mutate(() => api<State>(path + '/' + a.id + '?rowVersion=' + state.rowVersion, 'DELETE'), 'Anexo quitado y etiquetas actualizadas.') }}><Icon name="trash" size={18} /></button></div>
      </article>)}
      {editing && <form onSubmit={saveEdit} onKeyDown={event => { if (event.key === 'Escape' && !busy) { event.preventDefault(); event.stopPropagation(); void cancelEdit() } }}><fieldset disabled={busy}><legend>Editar anexo</legend><label>Nuevo título del anexo<input required maxLength={200} value={editing.title} onChange={e => setEditing({ ...editing, title: e.target.value })} /></label><label>Descripción del anexo<textarea maxLength={2000} value={editing.description} onChange={e => setEditing({ ...editing, description: e.target.value })} /></label><button>Guardar anexo</button><button type="button" className="secondary" onClick={() => void cancelEdit()}>Cancelar edición</button></fieldset></form>}
      <button className="secondary" disabled={busy} onClick={async () => { if (!dirty || (await confirmAction('¿Descartar los cambios locales y recargar?'))) { setTitle(''); setFile(null); setEditing(null); requestKey.current = crypto.randomUUID(); if (fileInput.current) { fileInput.current.value = ''; fileInput.current.dispatchEvent(new Event('change', { bubbles: true })) }; void mutate(() => api<State>(path), 'Versión actual cargada.') } }}>Recargar anexos</button>
    </div>}
    {(onPrevious || onNext) && <StepPagination previous={onPrevious ? { label: previousLabel, action: () => void navigate(onPrevious) } : undefined} next={onNext ? { label: nextLabel, action: () => void navigate(onNext) } : undefined} disabled={busy || !state} />}
  </section>
}
