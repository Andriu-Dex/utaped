import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'
import { Icon } from '../shared/Icon'

type Item = { documentId: string; activityId: string; documentTitle: string; activityTitle: string; groupName: string; periodName: string; deadline: string | null; missingEvidence: number }
type Page = { items: Item[]; total: number; page: number; size: number }
export function UpcomingActivities({ groupId, periodId, onOpenDocument }: { groupId: string; periodId: string; onOpenDocument?: (id: string) => void }) {
  const [kind, setKind] = useState('UPCOMING')
  const [open, setOpen] = useState(false), [page, setPage] = useState(0), [data, setData] = useState<Page | null>(null), [error, setError] = useState(''), [reload, setReload] = useState(0)
  const [previousScope, setPreviousScope] = useState({ groupId, periodId })
  if (previousScope.groupId !== groupId || previousScope.periodId !== periodId) { setPreviousScope({ groupId, periodId }); setPage(0) }
  const query = new URLSearchParams({ page: String(page), size: '20', kind, ...(groupId ? { groupId } : {}), ...(periodId ? { periodId } : {}) }).toString()
  const [loadedQuery, setLoadedQuery] = useState('')
  const loading = loadedQuery !== query
  useEffect(() => {
    if (!open) return
    const controller = new AbortController()
    api<Page>('/reporting/upcoming?' + query, 'GET', undefined, controller.signal).then(value => { if (!controller.signal.aborted) { setData(value); setLoadedQuery(query); setError('') } }).catch(e => { if (!controller.signal.aborted) { setData(null); setError(e.message); setLoadedQuery(query) } })
    return () => controller.abort()
  }, [open, query, reload])
  return <section className="card"><div className="section-heading"><h3>Detalle de actividades</h3><button type="button" className="secondary" aria-expanded={open} onClick={() => setOpen(value => !value)}>{open ? 'Ocultar detalle' : 'Ver todas las actividades'}</button></div>{open && <><label>Actividades a consultar<select value={kind} onChange={e => { setKind(e.currentTarget.value); setPage(0) }}><option value="UPCOMING">Próximas a vencer</option><option value="OVERDUE">Con plazos vencidos</option><option value="PENDING_EVIDENCE">Con evidencias pendientes</option></select></label>{error && <Toast message={error} />}{loading ? <p role="status">Consultando actividades…</p> : data ? <><p>{data.total} actividades de documentos que puede consultar</p><div className="table-scroll"><table><thead><tr><th>Actividad</th><th>Documento</th><th>Vencimiento</th><th>Evidencias pendientes</th>{onOpenDocument && <th>Acción</th>}</tr></thead><tbody>{data.items.map(item => <tr key={item.documentId + item.activityId}><td>{item.activityTitle}</td><td>{item.documentTitle}<small>{item.groupName} · {item.periodName}</small></td><td>{item.deadline ? item.deadline.split('-').reverse().join('/') : 'Sin plazo'}</td><td>{item.missingEvidence}</td>{onOpenDocument && <td><button type="button" className="secondary table-action-icon-btn" title="Abrir ejecución" aria-label={'Abrir ejecución: ' + item.activityTitle} onClick={() => onOpenDocument(item.documentId)}><Icon name="arrow" size={18} /></button></td>}</tr>)}</tbody></table></div>{!data.items.length && <p>No hay actividades con estos filtros.</p>}<div className="review-actions"><button type="button" className="secondary" disabled={!page} onClick={() => setPage(value => value - 1)}>Anterior</button><span>Página {page + 1}</span><button type="button" className="secondary" disabled={(page + 1) * data.size >= data.total} onClick={() => setPage(value => value + 1)}>Siguiente</button></div></> : <button type="button" className="secondary" onClick={() => { setLoadedQuery(''); setReload(value => value + 1) }}>Reintentar actividades</button>}</>}</section>
}
