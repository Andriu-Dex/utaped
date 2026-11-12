import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import './work-assistance.css'

type Kind = 'CORRECTION' | 'REVIEW' | 'SIGNATURE' | 'EVIDENCE'
interface Item { id: string; kind: Kind; documentId: string; title: string; detail: string; groupName: string; periodName: string; deadline: string | null; overdue: boolean; step: string; action: string }
interface PendingPage { items: Item[]; total: number; page: number; size: number; counts: Record<Kind, number> }
const categories: { key: Kind; label: string }[] = [{ key: 'CORRECTION', label: 'Correcciones' }, { key: 'REVIEW', label: 'Revisiones' }, { key: 'SIGNATURE', label: 'Firmas' }, { key: 'EVIDENCE', label: 'Evidencias' }]
function date(value: string) { return new Date(value + 'T12:00:00').toLocaleDateString('es-EC') }

export function PendingInbox({ onOpen }: { onOpen: (id: string, step?: string) => void }) {
  const [kind, setKind] = useState(''), [page, setPage] = useState(0), [reload, setReload] = useState(0)
  const [data, setData] = useState<PendingPage | null>(null), [error, setError] = useState('')
  const [expanded, setExpanded] = useState(false)
  useEffect(() => {
    let active = true
    api<PendingPage>('/pending-work?' + new URLSearchParams({ kind, page: String(page), size: '5' })).then(value => {
      if (!active) return
      if (value.total > 0 && page * value.size >= value.total) { setPage(0); return }
      setData(value); setError('')
    }).catch(e => { if (active) { setError(e.message); setData(null) } })
    return () => { active = false }
  }, [kind, page, reload])
  function filter(value: string) { if (value === kind && page === 0) return; setData(null); setError(''); setPage(0); setKind(value) }
  const total = data ? Object.values(data.counts).reduce((sum, value) => sum + value, 0) : null

  // Alternativa B: Banner dinámico inteligente cuando no hay pendientes y no se ha expandido manualmente
  if (data && total === 0 && !expanded) {
    return (
      <section className="card pending-banner-cleared" aria-label="Mis pendientes">
        <div className="pending-banner-main">
          <span className="pending-banner-icon" aria-hidden="true">
            <Icon name="check" size={16} />
          </span>
          <p className="pending-banner-text">
            <strong>✓ Al día:</strong> No tienes tareas pendientes. Puedes continuar con tus documentos recientes.
          </p>
        </div>
        <div className="pending-banner-actions">
          <button
            type="button"
            className="link pending-banner-expand-btn"
            onClick={() => setExpanded(true)}
            title="Ver filtros y categorías"
          >
            Ver filtros
          </button>
          <button
            type="button"
            className="secondary pending-banner-refresh-btn"
            title="Actualizar mis pendientes"
            aria-label="Actualizar mis pendientes"
            onClick={() => { setData(null); setError(''); setReload(value => value + 1) }}
          >
            <Icon name="refresh" size={16} />
          </button>
        </div>
      </section>
    )
  }

  return <section className="card pending-inbox" aria-label="Mis pendientes">
    <header className="assistance-heading">
      <div className="section-heading">
        <span className="section-icon"><Icon name="calendar" /></span>
        <div>
          <span className="section-kicker">TU PRÓXIMA ACCIÓN</span>
          <h2>Mis pendientes</h2>
          <p className="muted">Lo que requiere tu intervención, con los plazos más urgentes primero.</p>
        </div>
      </div>
      <div className="assistance-heading-actions">
        {total === 0 && (
          <button
            type="button"
            className="link pending-collapse-btn"
            onClick={() => setExpanded(false)}
            title="Contraer a banner"
          >
            Contraer aviso
          </button>
        )}
        <button
          type="button"
          className="secondary"
          title="Actualizar mis pendientes"
          aria-label="Actualizar mis pendientes"
          disabled={!data && !error}
          onClick={() => { setData(null); setError(''); setReload(value => value + 1) }}
        >
          <Icon name="refresh" size={18} />
        </button>
      </div>
    </header>
    <div className="pending-filters" role="group" aria-label="Tipo de pendiente">
      <button type="button" className="secondary" aria-pressed={kind === ''} onClick={() => filter('')}>Todos <span>{total ?? '—'}</span></button>
      {categories.map(category => <button key={category.key} type="button" className="secondary" aria-pressed={kind === category.key} onClick={() => filter(category.key)}>{category.label} <span>{data?.counts[category.key] ?? '—'}</span></button>)}
    </div>
    {error && <><Toast message={error} /><p>No se pudieron consultar tus pendientes.</p><button type="button" className="secondary" onClick={() => { setError(''); setReload(value => value + 1) }}>Reintentar pendientes</button></>}
    {!data && !error && <p role="status">Consultando tus pendientes…</p>}
    {data && <>
      {data.items.length === 0 ? <div className="assistance-empty"><span className="section-icon"><Icon name="check" /></span><h3>{kind ? 'Sin pendientes de este tipo' : 'Sin pendientes asignados'}</h3><p>No tienes acciones pendientes para las asignaciones consultadas.</p></div> : <ul className="pending-list">{data.items.map(item => <li key={item.id} className={item.overdue ? 'pending-overdue' : ''}>
        <div><div className="pending-meta"><span className={'status-badge ' + (item.overdue ? 'pending-deadline' : '')}>{categories.find(category => category.key === item.kind)?.label}</span>{item.deadline && <span className={item.overdue ? 'pending-deadline' : 'muted'}>{item.overdue ? 'Plazo vencido · ' : 'Hasta '}{date(item.deadline)}</span>}</div><h3>{item.title}</h3><p>{item.detail}</p><small className="muted">{item.groupName} · {item.periodName}</small></div>
        <button type="button" className="secondary" onClick={() => onOpen(item.documentId, item.step)} aria-label={item.action + ': ' + item.title}>{item.action}<Icon name="arrow" size={16} /></button>
      </li>)}</ul>}
      <footer className="pending-pagination"><span role="status">{data.total} {data.total === 1 ? 'pendiente' : 'pendientes'}{data.total > 0 && ' · Página ' + (page + 1)}</span><div><button type="button" className="secondary" disabled={page === 0} onClick={() => { setData(null); setPage(value => value - 1) }}>Anterior</button><button type="button" className="secondary" disabled={(page + 1) * data.size >= data.total} onClick={() => { setData(null); setPage(value => value + 1) }}>Siguiente</button></div></footer>
    </>}
  </section>
}
