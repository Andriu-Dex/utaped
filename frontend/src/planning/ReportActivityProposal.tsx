import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api } from '../shared/api'
import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'

type Proposal = { id: string; title: string; status: 'PENDING' | 'ACCEPTED' | 'REJECTED'; resolution_note: string | null }
const labels = { PENDING: 'Pendiente de Administración', ACCEPTED: 'Incorporada al catálogo', REJECTED: 'No incorporada' }
export function ReportActivityProposal({ documentId, onDirtyChange }: { documentId: string; onDirtyChange: (dirty: boolean) => void }) {
  const [title, setTitle] = useState(''), [items, setItems] = useState<Proposal[] | null>(null), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState(''), [reload, setReload] = useState(0)
  const [loaded, setLoaded] = useState(false)
  const requestKey = useRef(crypto.randomUUID())
  useEffect(() => { onDirtyChange(!!title); return () => onDirtyChange(false) }, [title, onDirtyChange])
  useEffect(() => { let active = true; api<Proposal[]>('/reports/' + documentId + '/activity-proposals').then(values => { if (active) { setItems(values); setError(''); setLoaded(true) } }).catch(e => { if (active) { setError(e.message); setItems(null); setLoaded(true) } }); return () => { active = false } }, [documentId, reload])
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('')
    try { await api('/reports/' + documentId + '/activity-proposals', 'POST', { title, requestKey: requestKey.current }); setTitle(''); requestKey.current = crypto.randomUUID(); setReload(value => value + 1); setNotice('Propuesta enviada a Administración para un período posterior.') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo registrar la propuesta.') }
    finally { setBusy(false) }
  }
  return <section className="card"><h3>Actividades para próximos períodos</h3><p className="muted">Proponga una actividad a partir de las mejoras del Informe. Administración revisará su incorporación al catálogo.</p>{error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}<ActionForm label="Proponer actividad futura" modal disabled={busy} dirty={!!title} onCancel={() => setTitle('')}><form onSubmit={submit}><fieldset disabled={busy}><label>Actividad propuesta<textarea required maxLength={500} value={title} onChange={e => setTitle(e.currentTarget.value)} /></label><button>Enviar propuesta</button></fieldset></form></ActionForm>{!loaded ? <p role="status">Consultando propuestas…</p> : !items ? <button type="button" className="secondary" onClick={() => { setLoaded(false); setReload(value => value + 1) }}>Reintentar propuestas</button> : items.length > 0 && <ul className="scope-member-list">{items.map(item => <li key={item.id}><strong>{item.title}</strong><span className="status-badge">{labels[item.status]}</span>{item.resolution_note && <small>{item.resolution_note}</small>}</li>)}</ul>}</section>
}
