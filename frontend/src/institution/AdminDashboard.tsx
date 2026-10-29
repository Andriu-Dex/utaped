import { ManagementOverview } from '../planning/ManagementOverview'
import type { ReportingSummary } from '../planning/reportingTypes'
import { useEffect, useState } from 'react'
import { api, type Group, type Period } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import { auditActionLabels } from '../audit/auditLabels'
import type { AdminPage } from './adminNavigation'
import './administration.css'

interface Summary {
  reporting: ReportingSummary
  users: number
  activeUsers: number
  groups: Group[]
  periods: Period[]
  events: { id: string; action: string; actorName: string | null; occurredAt: string }[]
}

export function AdminDashboard({ onOpen }: { onOpen: (page: AdminPage) => void }) {
  const [data, setData] = useState<Summary | null>(null)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let active = true
    Promise.all([
      api<{ total: number }>('/admin/users/directory?size=1'),
      api<{ total: number }>('/admin/users/directory?size=1&active=true'),
      api<Group[]>('/admin/groups'),
      api<Period[]>('/periods'),
      api<{ items: Summary['events'] }>('/admin/audit-events?page=0'),
      api<ReportingSummary>('/reporting'),
    ])
      .then(([users, activeUsers, groups, periods, events, reporting]) => {
        if (active) {
          setData({
            reporting,
            users: users.total,
            activeUsers: activeUsers.total,
            groups,
            periods,
            events: events.items.slice(0, 5),
          })
          setError('')
        }
      })
      .catch(e => {
        if (active) setError(e instanceof Error ? e.message : 'No se pudo cargar el panel administrativo.')
      })
    return () => {
      active = false
    }
  }, [refresh])

  const adminMetrics = data
    ? [
        { label: 'Usuarios registrados', value: data.users, page: 'users' as const, icon: 'users' as const, variant: 'blue' },
        { label: 'Usuarios activos', value: data.activeUsers, page: 'users' as const, icon: 'check' as const, variant: 'teal' },
        { label: 'Grupos activos', value: data.groups.filter(group => group.active).length, page: 'groups' as const, icon: 'columns' as const, variant: 'indigo' },
        { label: 'Períodos registrados', value: data.periods.length, page: 'periods' as const, icon: 'calendar' as const, variant: 'amber' },
      ]
    : []

  const shortcuts = [
    { label: 'Usuarios y acceso', page: 'users' as const, icon: 'users' as const, desc: 'Cuentas y roles del personal' },
    { label: 'Grupos y pertenencias', page: 'groups' as const, icon: 'columns' as const, desc: 'Comisiones y adscripciones' },
    { label: 'Catálogo de actividades', page: 'activities' as const, icon: 'document' as const, desc: 'Plantillas y actividades' },
    { label: 'Recursos y medios', page: 'resources' as const, icon: 'archive' as const, desc: 'Evidencias y repositorios' },
    { label: 'Flujos de aprobación', page: 'workflows' as const, icon: 'check' as const, desc: 'Rutas de firma y validación' },
    { label: 'Feriados y restricciones', page: 'holidays' as const, icon: 'calendar' as const, desc: 'Días inhábiles del ciclo' },
  ] as const

  return (
    <section aria-label="Panel general administrativo" className="admin-dashboard-view">
      <div className="admin-overview-heading">
        <div>
          <span className="section-kicker">CONFIGURACIÓN Y CONTROL</span>
          <h1>Panel general</h1>
          <p className="muted">Administra cuentas, organización y configuración institucional.</p>
        </div>
        <button
          type="button"
          className="secondary"
          onClick={() => {
            setData(null)
            setError('')
            setRefresh(value => value + 1)
          }}
        >
          <Icon name="refresh" />
          Actualizar panel
        </button>
      </div>

      {error && <Toast message={error} />}
      {!data && !error && <p role="status">Cargando panel administrativo…</p>}

      {data && (
        <>
          <div className="admin-metrics">
            {adminMetrics.map(metric => (
              <button
                type="button"
                className={`secondary admin-metric admin-metric-${metric.variant}`}
                key={metric.label}
                onClick={() => onOpen(metric.page)}
              >
                <div className="admin-metric-top">
                  <span className={`admin-metric-icon icon-${metric.variant}`}>
                    <Icon name={metric.icon} size={20} />
                  </span>
                  <span className="admin-metric-action-icon">
                    <Icon name="arrow" size={14} />
                  </span>
                </div>
                <div className="admin-metric-content">
                  <strong>{metric.value}</strong>
                  <span className="admin-metric-label">{metric.label}</span>
                </div>
                <small>
                  Consultar
                  <Icon name="arrow" size={13} />
                </small>
              </button>
            ))}
          </div>

          <div className="management-page-header">
            <div>
              <span className="section-kicker">PANORAMA INSTITUCIONAL</span>
              <h2>Gestión documental</h2>
            </div>
            <button type="button" className="secondary" onClick={() => onOpen('reporting')}>
              <Icon name="document" />
              Ver reportes de gestión
            </button>
          </div>

          <ManagementOverview data={data.reporting} />

          <div className="admin-dashboard-grid">
            <section className="card admin-shortcuts-card">
              <div className="section-heading">
                <span className="section-icon"><Icon name="settings" /></span>
                <div>
                  <span className="section-kicker">MÓDULOS DEL SISTEMA</span>
                  <h2>Configuración institucional</h2>
                </div>
              </div>
              <p className="muted">Accede al módulo que necesitas gestionar.</p>
              <div className="admin-shortcuts">
                {shortcuts.map(item => (
                  <button
                    type="button"
                    className="secondary admin-shortcut-btn"
                    key={item.page}
                    onClick={() => onOpen(item.page)}
                  >
                    <span className="shortcut-icon">
                      <Icon name={item.icon} size={18} />
                    </span>
                    <span className="shortcut-content">
                      <strong className="shortcut-label">{item.label}</strong>
                      <small className="shortcut-desc">{item.desc}</small>
                    </span>
                    <Icon name="arrow" size={16} className="shortcut-arrow" />
                  </button>
                ))}
              </div>
            </section>

            <section className="card admin-activity-card">
              <div className="section-heading">
                <span className="section-icon amber"><Icon name="clock" /></span>
                <div>
                  <span className="section-kicker">REGISTRO DE ACCIONES</span>
                  <h2>Actividad reciente</h2>
                </div>
              </div>
              <p className="muted">Últimos eventos registrados en la plataforma.</p>
              {data.events.length ? (
                <ul className="admin-events">
                  {data.events.map(event => (
                    <li key={event.id} className="admin-event-item">
                      <div className="event-dot" />
                      <div className="event-details">
                        <strong>{auditActionLabels[event.action] ?? event.action}</strong>
                        <div className="event-meta">
                          <span className="event-actor">
                            <Icon name="users" size={13} />
                            {event.actorName ?? 'Sistema'}
                          </span>
                          <time dateTime={event.occurredAt}>
                            {new Date(event.occurredAt).toLocaleString('es-EC', { dateStyle: 'short', timeStyle: 'short' })}
                          </time>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="empty-state">No hay actividad registrada.</p>
              )}
              <button
                type="button"
                className="secondary admin-audit-btn"
                onClick={() => onOpen('audit')}
              >
                <Icon name="eye" size={16} />
                Abrir auditoría
                <Icon name="arrow" size={14} />
              </button>
            </section>
          </div>
        </>
      )}
    </section>
  )
}
