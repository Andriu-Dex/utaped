import { useEffect, useState } from 'react'
import { api, type Account } from '../shared/api'
import { Toast } from '../shared/Toast'
import { ReviewInspector } from './ReviewWorkspace'

type Round = { id: string; round_number: number; status: string; created_at: string }
export function ReviewHistory({ documentId, title, onBack, onDirtyChange }: { documentId: string; title: string; onBack: () => void; onDirtyChange: (dirty: boolean) => void }) {
  const [rounds, setRounds] = useState<Round[]>([]); const [account, setAccount] = useState<Account | null>(null); const [selected, setSelected] = useState(''); const [error, setError] = useState(''); const [loading, setLoading] = useState(true)
  useEffect(() => { let active = true; Promise.all([api<Round[]>('/work-plans/' + documentId + '/reviews'), api<Account>('/auth/me')]).then(([r, a]) => { if (active) { setRounds(r); setAccount(a); setLoading(false) } }).catch(e => { if (active) { setError(e.message); setLoading(false) } }); return () => { active = false } }, [documentId])
  if (selected && account) return <ReviewInspector key={selected} id={selected} accountId={account.id} title={title} onBack={() => setSelected('')} onDirtyChange={onDirtyChange} />
  return <section><button type="button" className="secondary" onClick={onBack}>Volver al documento</button><span className="section-kicker review-kicker">TRAZABILIDAD DOCUMENTAL</span><h1>Rondas y observaciones</h1>{error && <Toast message={error} />}<section className="card">{loading ? <p role="status">Cargando rondas…</p> : rounds.length === 0 ? <div className="empty-state"><p>Aún no hay rondas de revisión.</p><small>La firma del documento inicia el flujo configurado.</small></div> : <div className="table-scroll"><table><thead><tr><th>Ronda</th><th>Estado</th><th>Inicio</th><th>Acción</th></tr></thead><tbody>{rounds.map(r => <tr key={r.id}><td>Ronda {r.round_number}</td><td><span className={'status-badge ' + (r.status === 'APPROVED' ? 'active' : '')}>{r.status === 'APPROVED' ? 'Aprobada' : r.status === 'RETURNED' ? 'Devuelta' : 'En revisión'}</span></td><td>{new Date(r.created_at).toLocaleString('es-EC')}</td><td><button type="button" className="secondary" onClick={() => setSelected(r.id)}>Ver ronda {r.round_number}</button></td></tr>)}</tbody></table></div>}</section></section>
}
