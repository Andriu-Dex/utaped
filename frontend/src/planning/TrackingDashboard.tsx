import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import { PendingInbox } from './PendingInbox'
import type { PlanPage } from './types'
import './management-dashboard.css'

function typeLabel(type: 'T1' | 'T2' | 'T3') {
  if (type === 'T1') return 'Plan T1'
  if (type === 'T3') return 'Informe de ejecución'
  return 'Informe final T2'
}

function statusClass(archivedAt: string | null, state: 'DRAFT' | 'IN_REVIEW' | 'APPROVED') {
  if (archivedAt) return 'status-archived'
  if (state === 'APPROVED') return 'status-approved'
  if (state === 'IN_REVIEW') return 'status-in_review'
  return 'status-draft'
}

function statusLabel(archivedAt: string | null, state: 'DRAFT' | 'IN_REVIEW' | 'APPROVED') {
  if (archivedAt) return 'Archivado'
  if (state === 'APPROVED') return 'Aprobado'
  if (state === 'IN_REVIEW') return 'En revisión'
  return 'Borrador'
}

function formatUpdatedAt(value: string) {
  try {
    const d = new Date(value)
    if (isNaN(d.getTime())) return value
    return d.toLocaleDateString('es-EC', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return value
  }
}

export function TrackingDashboard({
  displayName,
  onOpen,
  onOpenReports,
  onOpenDocuments,
}: {
  displayName?: string
  onOpen: (id: string, step?: string) => void
  onOpenReports: () => void
  onOpenDocuments?: () => void
}) {
  const [data, setData] = useState<PlanPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true
    api<PlanPage>('/tracking/recent')
      .then(page => {
        if (active) {
          setData(page)
          setError('')
        }
      })
      .catch(e => {
        if (active) {
          setError(e instanceof Error ? e.message : 'No se pudieron consultar los documentos recientes.')
          setData(null)
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [reload])

  return (
    <section className="management-home" aria-label="Espacio de trabajo personal">
      <header className="tracking-home-header">
        <div>
          <span className="section-kicker">ESPACIO PERSONAL</span>
          <h1>Bienvenido, {displayName || 'Inicio'}</h1>
          <p className="muted">Organiza tu planificación, atiende tus pendientes y retoma tus documentos recientes.</p>
        </div>
        <button type="button" className="secondary" onClick={onOpenReports}>
          <Icon name="document" size={17} />Ver reportes de gestión
        </button>
      </header>

      <PendingInbox onOpen={onOpen} />

      <section className="card recent-documents-card" aria-label="Documentos recientes">
        <div className="management-page-header">
          <div className="section-heading">
            <span className="section-icon"><Icon name="document" /></span>
            <div>
              <span className="section-kicker">TU ESPACIO DE TRABAJO</span>
              <h2>Documentos recientes</h2>
              <p className="muted">Retoma tus borradores en curso y consulta tus documentos vigentes.</p>
            </div>
          </div>
          <button
            type="button"
            className="secondary"
            title="Actualizar documentos recientes"
            aria-label="Actualizar documentos recientes"
            disabled={loading}
            onClick={() => { setData(null); setError(''); setLoading(true); setReload(n => n + 1) }}
          >
            <Icon name="refresh" size={18} />
          </button>
        </div>

        {loading && !data && !error && <p role="status">Cargando documentos recientes…</p>}

        {error && (
          <div className="recent-error-state">
            <Toast message={error} onDismiss={() => setError('')} />
            <p>No se pudieron consultar tus documentos recientes.</p>
            <button
              type="button"
              className="secondary"
              onClick={() => { setData(null); setError(''); setLoading(true); setReload(n => n + 1) }}
            >
              Reintentar recientes
            </button>
          </div>
        )}

        {data && (data.items.length === 0 ? (
          <div className="empty-state">
            <Icon name="document" size={32} />
            <p>Aún no tienes documentos disponibles.</p>
            {onOpenDocuments && (
              <button type="button" className="secondary" onClick={onOpenDocuments}>
                Ver todos los documentos<Icon name="arrow" size={16} />
              </button>
            )}
          </div>
        ) : (
          <>
            <ul className="tracking-list">
              {data.items.map(plan => (
                <li key={plan.id} className="tracking-item">
                  <div className="tracking-item-main">
                    <span className="tracking-item-icon" aria-hidden="true"><Icon name="document" size={18} /></span>
                    <div className="tracking-item-info">
                      <div className="doc-title-cell">
                        <span className={'doc-type-pill doc-type-' + plan.documentType.toLowerCase()}>
                          {typeLabel(plan.documentType)}
                        </span>
                        <strong>{plan.title}</strong>
                        <span className={'status-badge ' + statusClass(plan.archivedAt, plan.documentState)}>
                          {statusLabel(plan.archivedAt, plan.documentState)}
                        </span>
                        {!plan.editable && (
                          <span className="status-badge status-readonly">Solo lectura</span>
                        )}
                      </div>
                      <div className="tracking-item-meta">
                        <span>Versión {plan.formalVersion} · {plan.groupName} · {plan.periodName}</span>
                        {plan.updatedAt && (
                          <span className="tracking-item-updated">
                            Actualizado {formatUpdatedAt(plan.updatedAt)}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  <button
                    type="button"
                    className="secondary"
                    title={'Abrir ' + plan.title}
                    aria-label={'Abrir ' + plan.title}
                    onClick={() => onOpen(plan.id)}
                  >
                    Abrir<Icon name="arrow" size={16} />
                  </button>
                </li>
              ))}
            </ul>
            {onOpenDocuments && (
              <footer className="recent-card-footer">
                <button type="button" className="link recent-view-all-btn" onClick={onOpenDocuments}>
                  {data.total > data.items.length
                    ? `Ver todos los documentos (${data.total})`
                    : 'Ver todos los documentos'}
                  <Icon name="arrow" size={16} />
                </button>
              </footer>
            )}
          </>
        ))}
      </section>
    </section>
  )
}
