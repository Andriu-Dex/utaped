import { useEffect, useState } from 'react'
import { api, type Account } from '../shared/api'
import { Icon, type IconName } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import { ReviewInspector } from '../workflow/ReviewWorkspace'
import { AuditViewer } from '../audit/AuditViewer'
import './document-timeline.css'

export interface TimelineItem {
  id: string
  kind: string
  title: string
  detail: string
  actorName: string | null
  status: string | null
  roundNumber: number | null
  formalVersion: string | null
  occurredAt: string
  reviewId: string | null
  linkedDocumentId: string | null
  linkedDocumentType: string | null
}

export interface DocumentTimelineProps {
  documentId: string
  title: string
  documentType?: string
  formalVersion?: string
  documentState?: string
  onBack: () => void
  onDirtyChange?: (dirty: boolean) => void
  onOpenDocument?: (docId: string) => void
}

type TabMode = 'timeline' | 'audit'
type FilterCategory = 'ALL' | 'CREATION' | 'SIGNATURE' | 'REVIEW' | 'VERSION' | 'REPORT'

export function DocumentTimeline({
  documentId,
  title,
  documentType = 'T1',
  formalVersion,
  documentState,
  onBack,
  onDirtyChange,
  onOpenDocument,
}: DocumentTimelineProps) {
  const [tab, setTab] = useState<TabMode>('timeline')
  const [order, setOrder] = useState<'asc' | 'desc'>('asc')
  const [filter, setFilter] = useState<FilterCategory>('ALL')
  const [items, setItems] = useState<TimelineItem[]>([])
  const [account, setAccount] = useState<Account | null>(null)
  const [selectedRound, setSelectedRound] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true
    Promise.resolve().then(() => {
      if (active) {
        setLoading(true)
        setError('')
      }
    })
    Promise.all([
      api<TimelineItem[]>(`/work-plans/${documentId}/timeline?order=${order}`),
      api<Account>('/auth/me'),
    ])
      .then(([timelineItems, me]) => {
        if (active) {
          setItems(timelineItems)
          setAccount(me)
          setLoading(false)
        }
      })
      .catch(e => {
        if (active) {
          setError(e instanceof Error ? e.message : 'No se pudo cargar la línea de tiempo.')
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [documentId, order, reload])

  if (selectedRound && account) {
    return (
      <ReviewInspector
        key={selectedRound}
        id={selectedRound}
        accountId={account.id}
        title={title}
        onBack={() => setSelectedRound('')}
        onDirtyChange={onDirtyChange ?? (() => {})}
      />
    )
  }

  function getNodeStyle(kind: string): { className: string; icon: IconName } {
    switch (kind) {
      case 'CREATION':
        return { className: 'node-creation', icon: 'document' }
      case 'SIGNATURE':
        return { className: 'node-signature', icon: 'edit' }
      case 'REVIEW_ROUND_SUBMITTED':
        return { className: 'node-review-round', icon: 'eye' }
      case 'REVIEW_RETURNED':
        return { className: 'node-review-returned', icon: 'refresh' }
      case 'REVIEW_TASK_COMPLETED':
        return { className: 'node-review-completed', icon: 'check' }
      case 'APPROVAL':
        return { className: 'node-approval', icon: 'check' }
      case 'FORMAL_VERSION':
        return { className: 'node-formal-version', icon: 'copy' }
      case 'LINKED_REPORT':
        return { className: 'node-linked-report', icon: 'document' }
      case 'ARCHIVED':
        return { className: 'node-archived', icon: 'archive' }
      default:
        return { className: '', icon: 'clock' }
    }
  }

  const filteredItems = items.filter(item => {
    if (filter === 'ALL') return true
    if (filter === 'CREATION') return item.kind === 'CREATION'
    if (filter === 'SIGNATURE') return item.kind === 'SIGNATURE'
    if (filter === 'REVIEW') {
      return (
        item.kind === 'REVIEW_ROUND_SUBMITTED' ||
        item.kind === 'REVIEW_TASK_COMPLETED' ||
        item.kind === 'REVIEW_RETURNED' ||
        item.kind === 'APPROVAL'
      )
    }
    if (filter === 'VERSION') return item.kind === 'FORMAL_VERSION'
    if (filter === 'REPORT') return item.kind === 'LINKED_REPORT'
    return true
  })

  function formatTime(isoString: string): string {
    try {
      const date = new Date(isoString)
      return date.toLocaleString('es-EC', {
        dateStyle: 'medium',
        timeStyle: 'short',
      })
    } catch {
      return isoString
    }
  }

  return (
    <section className="document-timeline-container" aria-label="Línea de tiempo documental">
      <header className="plan-wizard-header">
        <span className="section-kicker review-kicker">TRAZABILIDAD DOCUMENTAL</span>
        <div className="plan-header-title-row">
          <button
            type="button"
            className="plan-back-btn"
            onClick={onBack}
            title="Volver al documento"
            aria-label="Volver al documento"
          >
            <Icon name="arrowLeft" size={20} />
            <span className="sr-only">Volver al documento</span>
          </button>
          <h1>Línea de tiempo documental</h1>
        </div>
        <div className="document-timeline-meta">
          <span className="document-timeline-title-badge">
            <Icon name="document" size={14} />
            {documentType}
          </span>
          {formalVersion && (
            <span className="document-timeline-title-badge">
              Versión formal {formalVersion}
            </span>
          )}
          {documentState && (
            <span
              className={
                'status-badge ' +
                (documentState === 'APPROVED' ? 'active' : documentState === 'IN_REVIEW' ? 'pending' : '')
              }
            >
              {documentState === 'APPROVED'
                ? 'Aprobado'
                : documentState === 'IN_REVIEW'
                  ? 'En revisión'
                  : 'Borrador'}
            </span>
          )}
          <span className="muted" style={{ fontSize: '0.8125rem' }}>
            {title}
          </span>
        </div>
      </header>

      {error && <Toast message={error} />}

      <div className="document-timeline-controls">
        <div className="document-timeline-tabs" role="tablist" aria-label="Vistas de trazabilidad">
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'timeline'}
            className={'timeline-tab-btn' + (tab === 'timeline' ? ' is-active' : '')}
            onClick={() => setTab('timeline')}
          >
            <Icon name="clock" size={16} />
            <span>Línea de tiempo</span>
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'audit'}
            className={'timeline-tab-btn' + (tab === 'audit' ? ' is-active' : '')}
            onClick={() => setTab('audit')}
          >
            <Icon name="eye" size={16} />
            <span>Auditoría técnica</span>
          </button>
        </div>

        {tab === 'timeline' && (
          <>
            <div className="document-timeline-filters" role="group" aria-label="Filtrar acontecimientos">
              <button
                type="button"
                className={'timeline-filter-pill' + (filter === 'ALL' ? ' is-active' : '')}
                onClick={() => setFilter('ALL')}
              >
                Todos ({items.length})
              </button>
              <button
                type="button"
                className={'timeline-filter-pill' + (filter === 'SIGNATURE' ? ' is-active' : '')}
                onClick={() => setFilter('SIGNATURE')}
              >
                Firmas ({items.filter(i => i.kind === 'SIGNATURE').length})
              </button>
              <button
                type="button"
                className={'timeline-filter-pill' + (filter === 'REVIEW' ? ' is-active' : '')}
                onClick={() => setFilter('REVIEW')}
              >
                Revisiones (
                {
                  items.filter(
                    i =>
                      i.kind === 'REVIEW_ROUND_SUBMITTED' ||
                      i.kind === 'REVIEW_TASK_COMPLETED' ||
                      i.kind === 'REVIEW_RETURNED' ||
                      i.kind === 'APPROVAL'
                  ).length
                }
                )
              </button>
              {items.some(i => i.kind === 'FORMAL_VERSION') && (
                <button
                  type="button"
                  className={'timeline-filter-pill' + (filter === 'VERSION' ? ' is-active' : '')}
                  onClick={() => setFilter('VERSION')}
                >
                  Versiones ({items.filter(i => i.kind === 'FORMAL_VERSION').length})
                </button>
              )}
              {items.some(i => i.kind === 'LINKED_REPORT') && (
                <button
                  type="button"
                  className={'timeline-filter-pill' + (filter === 'REPORT' ? ' is-active' : '')}
                  onClick={() => setFilter('REPORT')}
                >
                  Informes ({items.filter(i => i.kind === 'LINKED_REPORT').length})
                </button>
              )}
            </div>

            <button
              type="button"
              className="timeline-order-btn"
              onClick={() => setOrder(current => (current === 'asc' ? 'desc' : 'asc'))}
              title={
                order === 'asc'
                  ? 'Orden cronológico (1º más antiguos). Clic para invertir.'
                  : 'Orden cronológico inverso (1º más recientes). Clic para invertir.'
              }
              aria-label="Invertir orden cronológico"
            >
              <Icon name={order === 'asc' ? 'arrowDown' : 'arrowUp'} size={14} />
              <span>{order === 'asc' ? 'Más antiguos primero' : 'Más recientes primero'}</span>
            </button>
          </>
        )}
      </div>

      {tab === 'audit' ? (
        <AuditViewer targetDocId={documentId} />
      ) : (
        <section className="card" style={{ padding: 'var(--space-2, 0.5rem) var(--space-4, 1rem)' }}>
          {loading ? (
            <p role="status" style={{ padding: 'var(--space-4, 1rem)' }}>
              Cargando línea de tiempo…
            </p>
          ) : error ? (
            <div style={{ padding: 'var(--space-4, 1rem)' }}>
              <button
                type="button"
                className="secondary"
                onClick={() => setReload(v => v + 1)}
              >
                Reintentar carga de línea de tiempo
              </button>
            </div>
          ) : filteredItems.length === 0 ? (
            <div className="empty-state" style={{ padding: 'var(--space-6, 2rem) var(--space-4, 1rem)' }}>
              <Icon name="clock" size={32} />
              <p>No se encontraron acontecimientos para este filtro.</p>
              <small>El documento registrará hitos conforme avance en sus firmas y revisiones.</small>
            </div>
          ) : (
            <div className="document-timeline-track">
              {filteredItems.map(item => {
                const node = getNodeStyle(item.kind)
                return (
                  <div key={item.id} className="document-timeline-entry">
                    <div
                      className={'document-timeline-node ' + node.className}
                      aria-hidden="true"
                    >
                      <Icon name={node.icon} size={15} />
                    </div>

                    <article className="document-timeline-card">
                      <header className="timeline-card-header">
                        <div className="timeline-card-title-group">
                          <h3 className="timeline-card-title">{item.title}</h3>
                          {item.roundNumber != null && (
                            <span className="badge" style={{ fontSize: '0.7rem' }}>
                              Ronda {item.roundNumber}
                            </span>
                          )}
                          {item.formalVersion && (
                            <span className="badge" style={{ fontSize: '0.7rem' }}>
                              v{item.formalVersion}
                            </span>
                          )}
                          {item.status && (
                            <span
                              className={
                                'status-badge ' +
                                (item.status === 'APPROVED' || item.status === 'COMPLETED'
                                  ? 'active'
                                  : item.status === 'RETURNED'
                                    ? 'rejected'
                                    : 'pending')
                              }
                            >
                              {item.status === 'APPROVED'
                                ? 'Aprobado'
                                : item.status === 'RETURNED'
                                  ? 'Devuelto'
                                  : item.status === 'COMPLETED'
                                    ? 'Completado'
                                    : item.status === 'ACTIVE'
                                      ? 'En curso'
                                      : item.status}
                            </span>
                          )}
                        </div>
                        <time className="timeline-card-time" dateTime={item.occurredAt}>
                          <Icon name="clock" size={13} />
                          {formatTime(item.occurredAt)}
                        </time>
                      </header>

                      <p className="timeline-card-body">{item.detail}</p>

                      <footer className="timeline-card-meta">
                        {item.actorName && (
                          <span className="timeline-actor-chip">
                            <Icon name="users" size={13} />
                            {item.actorName}
                          </span>
                        )}

                        <div className="timeline-card-actions">
                          {item.reviewId && (
                            <button
                              type="button"
                              className="secondary timeline-action-btn"
                              onClick={() => setSelectedRound(item.reviewId!)}
                              title="Examinar observaciones y estado de la ronda"
                            >
                              <Icon name="eye" size={14} />
                              <span>Examinar ronda {item.roundNumber ?? ''}</span>
                            </button>
                          )}

                          {item.linkedDocumentId && onOpenDocument && (
                            <button
                              type="button"
                              className="secondary timeline-action-btn"
                              onClick={() => onOpenDocument(item.linkedDocumentId!)}
                              title="Abrir documento vinculado"
                            >
                              <Icon name="document" size={14} />
                              <span>Abrir documento</span>
                            </button>
                          )}
                        </div>
                      </footer>
                    </article>
                  </div>
                )
              })}
            </div>
          )}
        </section>
      )}
    </section>
  )
}
