import { Toast } from '../shared/Toast'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api } from '../shared/api'

interface Attachment { id: string; label: string; title: string; description: string; originalName: string; sizeBytes: number; pageCount: number }
interface State { rowVersion: number; editable: boolean; enabled: boolean; privacyNoticeEnabled: boolean; maxFileBytes: number; maxTotalBytes: number; maxAttachments: number; items: Attachment[] }
export function AttachmentEditor({ targetDocId, onBack, onDirtyChange }: { targetDocId: string; onBack: () => void; onDirtyChange: (dirty: boolean) => void }) {
  const path = '/work-plans/' + targetDocId + '/attachments'
  const [state, setState] = useState<State | null>(null)
  const [title, setTitle] = useState(''); const [file, setFile] = useState<File | null>(null)
  const [editing, setEditing] = useState<Attachment | null>(null)
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const requestKey = useRef(crypto.randomUUID()); const fileInput = useRef<HTMLInputElement>(null)
  const dirty = !!title || !!file || editing !== null
  useEffect(() => { let active = true; api<State>(path).then(value => { if (active) setState(value) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [path])
  useEffect(() => { onDirtyChange(dirty); const prevent = (e: BeforeUnloadEvent) => { if (dirty) { e.preventDefault(); e.returnValue = '' } }; window.addEventListener('beforeunload', prevent); return () => { window.removeEventListener('beforeunload', prevent); onDirtyChange(false) } }, [dirty, onDirtyChange])
  async function mutate(action: () => Promise<State>, message: string) {
    setBusy(true); setError(''); setNotice('')
    try { setState(await action()); setNotice(message); return true } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo completar la operación.'); return false } finally { setBusy(false) }
  }
  async function upload(e: FormEvent) {
    e.preventDefault(); if (!state || !file) return
    if (file.size > state.maxFileBytes) { setError('El archivo supera el límite permitido.'); return }
    const data = new FormData(); data.append('file', file)
    const query = new URLSearchParams({ rowVersion: String(state.rowVersion), requestKey: requestKey.current, title })
    if (await mutate(() => api<State>(path + '?' + query, 'POST', data), 'Anexo agregado.')) { setTitle(''); setFile(null); requestKey.current = crypto.randomUUID(); if (fileInput.current) fileInput.current.value = '' }
  }
  async function saveEdit(e: FormEvent) { e.preventDefault(); if (!state || !editing) return; if (await mutate(() => api<State>(path + '/' + editing.id, 'PUT', { rowVersion: state.rowVersion, title: editing.title, description: editing.description }), 'Anexo actualizado.')) setEditing(null) }
  function move(index: number, delta: number) { if (!state) return; const ids = state.items.map(a => a.id); [ids[index], ids[index + delta]] = [ids[index + delta], ids[index]]; void mutate(() => api<State>(path + '/order', 'PUT', { rowVersion: state.rowVersion, attachmentIds: ids }), 'Orden actualizado.') }
  return <section><button className="secondary" disabled={busy} onClick={() => { if (!dirty || window.confirm('¿Descartar los cambios sin guardar?')) onBack() }}>Volver al Plan</button><h1>Anexos del Plan T1</h1>
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    {!state ? <p>Cargando anexos…</p> : <div className="card">
      <p>Archivos PDF privados. Máximo {state.maxAttachments} anexos, {Math.floor(state.maxFileBytes / 1048576)} MB por archivo y {Math.floor(state.maxTotalBytes / 1048576)} MB en total. Límites técnicos configurables.</p>
      {!state.editable && <Toast tone="warning" message="Documento de solo lectura." />}
      <fieldset disabled={busy || !state.editable || dirty}><legend>Opciones del documento</legend>
        <label>¿El Plan tiene anexos?<select value={String(state.enabled)} onChange={e => void mutate(() => api<State>(path + '/settings', 'PUT', { rowVersion: state.rowVersion, enabled: e.target.value === 'true', privacyNoticeEnabled: state.privacyNoticeEnabled }), 'Opciones guardadas.')}><option value="false">No</option><option value="true">Sí</option></select></label>
        <label className="check"><input type="checkbox" checked={state.privacyNoticeEnabled} onChange={e => void mutate(() => api<State>(path + '/settings', 'PUT', { rowVersion: state.rowVersion, enabled: state.enabled, privacyNoticeEnabled: e.target.checked }), 'Opciones guardadas.')} />Incluir nota de protección de datos personales de la plantilla</label>
        <p>Seleccione la nota cuando corresponda al contenido del documento; no se activa automáticamente.</p>
      </fieldset>
      {state.enabled && <form onSubmit={upload}><fieldset disabled={busy || !state.editable || editing !== null}><legend>Agregar anexo</legend><label>Título del anexo<input required maxLength={200} value={title} onChange={e => setTitle(e.target.value)} /></label><label>Archivo PDF del anexo<input ref={fileInput} required type="file" accept="application/pdf,.pdf" onChange={e => { setFile(e.target.files?.[0] ?? null); requestKey.current = crypto.randomUUID() }} /></label><button disabled={state.items.length >= state.maxAttachments}>Agregar PDF</button></fieldset></form>}
      {state.items.map((a, index) => <article className="card" key={a.id} aria-label={a.label}><h2>{a.label}. {a.title}</h2><p>{a.originalName} · {a.pageCount} páginas · {(a.sizeBytes / 1024).toFixed(1)} KB</p><p className="preserve-text">{a.description}</p>
        <a href={'/api' + path + '/' + a.id + '/content'}>Descargar original</a>{' '}
        <button className="secondary" title={'Editar ' + a.label} aria-label={'Editar ' + a.label} disabled={busy || !state.editable || dirty} onClick={() => setEditing({ ...a })}>✎</button>{' '}
        <button className="secondary" title={'Subir ' + a.label} aria-label={'Subir ' + a.label} disabled={busy || !state.editable || dirty || index === 0} onClick={() => move(index, -1)}>↑</button>{' '}
        <button className="secondary" title={'Bajar ' + a.label} aria-label={'Bajar ' + a.label} disabled={busy || !state.editable || dirty || index === state.items.length - 1} onClick={() => move(index, 1)}>↓</button>{' '}
        <button className="danger" title={'Quitar ' + a.label} aria-label={'Quitar ' + a.label} disabled={busy || !state.editable || dirty} onClick={() => { if (window.confirm('¿Quitar este anexo del borrador? Las previsualizaciones anteriores se conservarán.')) void mutate(() => api<State>(path + '/' + a.id + '?rowVersion=' + state.rowVersion, 'DELETE'), 'Anexo quitado y etiquetas actualizadas.') }}>×</button>
      </article>)}
      {editing && <form onSubmit={saveEdit}><fieldset disabled={busy}><legend>Editar anexo</legend><label>Nuevo título del anexo<input required maxLength={200} value={editing.title} onChange={e => setEditing({ ...editing, title: e.target.value })} /></label><label>Descripción del anexo<textarea maxLength={2000} value={editing.description} onChange={e => setEditing({ ...editing, description: e.target.value })} /></label><button>Guardar anexo</button><button type="button" className="secondary" onClick={() => setEditing(null)}>Cancelar edición</button></fieldset></form>}
      <button className="secondary" disabled={busy} onClick={() => { if (!dirty || window.confirm('¿Descartar los cambios locales y recargar?')) { setTitle(''); setFile(null); setEditing(null); requestKey.current = crypto.randomUUID(); if (fileInput.current) fileInput.current.value = ''; void mutate(() => api<State>(path), 'Versión actual cargada.') } }}>Recargar anexos</button>
    </div>}
  </section>
}
