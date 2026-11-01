import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Icon, type IconName } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import { auditActionLabels } from '../audit/auditLabels'
import { formatPercent, type ReportingSummary } from '../planning/reportingTypes'
import type { AdminPage } from './adminNavigation'
import './admin-overview.css'

interface Window { label: string; startsOn: string | null; endsOn: string | null; status: 'OPEN' | 'UPCOMING' | 'CLOSED' | 'UNCONFIGURED'; remainingDays: number | null }
interface Period { id: string; name: string; startsOn: string; endsOn: string; current: boolean; windows: Window[] }
interface Summary {
  today: string; updatedAt: string; periods: Period[]; selectedPeriodId: string | null
  users: number; activeUsers: number; activeGroups: number; reporting: ReportingSummary
  documentTypes: { type: string; count: number }[]
  alerts: { scopesWithoutCoordinator: number; reviewsOutsideWindow: number; pendingEvidence: number; overdueActivities: number }
  events: { id: string; action: string; actorName: string | null; occurredAt: string }[]
}

const date = (value: string) => new Date(value + 'T12:00:00').toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: 'numeric' })

function windowLabel(window: Window) {
  if (window.status === 'UNCONFIGURED') return 'Sin configurar'
  if (window.status === 'CLOSED') return 'Ventana cerrada'
  if (window.status === 'UPCOMING') return 'Abre el ' + date(window.startsOn!)
  return window.remainingDays === 0 ? 'Cierra hoy' : window.remainingDays + (window.remainingDays === 1 ? ' día calendario restante' : ' días calendario restantes')
}

function category(action: string) {
  if (/^(LOGIN|LOGOUT|PASSWORD|ADMIN_PASSWORD|ACCOUNT|CAPTCHA)/.test(action)) return 'Seguridad'
  if (/^(USER|MEMBER|GROUP|SCOPE|POSITION|COORDINATOR)/.test(action)) return 'Organización'
  if (/^(WORK_PLAN|WORK_REPORT|REPORT|EXECUTION|REVIEW|AUTHOR|DOCUMENT|EVIDENCE)/.test(action)) return 'Documentos'
  return 'Configuración'
}

const typeLabels: Record<string, string> = { T1: 'Plan de Trabajo', T2: 'Informe final', T3: 'Informe de ejecución' }

export function AdminOverview({ onOpen }: { onOpen: (page: AdminPage, periodId?: string) => void }) {
  const [selection, setSelection] = useState('auto'), [refresh, setRefresh] = useState(0)
  const [data, setData] = useState<Summary | null>(null), [choices, setChoices] = useState<Period[]>([]), [error, setError] = useState('')

  useEffect(() => {
    let active = true
    const params = new URLSearchParams()
    if (selection === 'all') params.set('allPeriods', 'true')
    else if (selection !== 'auto') params.set('periodId', selection)
    api<Summary>('/admin/overview?' + params)
      .then(value => {
        if (active) {
          setData(value)
          setChoices(value.periods)
          setError('')
        }
      })
      .catch(e => {
        if (active) {
          setError(e.message)
          setData(null)
        }
      })
    return () => { active = false }
  }, [selection, refresh])

  function reload() { setData(null); setError(''); setRefresh(value => value + 1) }

  function selectPeriod(value: string) {
    const current = selection === 'auto' ? data?.selectedPeriodId ?? 'all' : selection
    if (value === current) return
    setData(null); setError(''); setSelection(value)
  }

  const selected = data?.periods.find(period => period.id === data.selectedPeriodId)
  const metrics = data?.reporting
  const total = metrics ? metrics.plans + metrics.reports : 0
  const reports = () => onOpen('reporting', data?.selectedPeriodId ?? undefined)

  const states = metrics ? [
    { label: 'Borradores', value: metrics.drafts, color: 'blue' },
    { label: 'En revisión', value: metrics.inReview, color: 'amber' },
    { label: 'Aprobados', value: metrics.approved, color: 'teal' }
  ] : []

  const activeSegmentsCount = states.filter(s => s.value > 0).length
  const gap = activeSegmentsCount > 1 ? 1.5 : 0
  let offset = 0
  const segments = states.map(state => {
    const start = offset
    const percentage = total ? (state.value / total) * 100 : 0
    offset += percentage
    const dash = Math.max(0, percentage - gap)
    return { ...state, start, percentage, dash }
  })

  const rows = [...(metrics?.rows ?? [])]
    .filter(row => row.activities > 0)
    .sort((a, b) => b.overdueActivities - a.overdueActivities || b.pendingEvidence - a.pendingEvidence || a.evidenceCoverage - b.evidenceCoverage || a.groupName.localeCompare(b.groupName, 'es') || a.careerName.localeCompare(b.careerName, 'es'))
    .slice(0, 5)

  const alerts: { value: number; label: string; detail: string; page: AdminPage }[] = data ? [
    { value: data.alerts.scopesWithoutCoordinator, label: 'Ámbitos sin coordinador activo', detail: 'Revisa las pertenencias y asignaciones institucionales.', page: 'organization' as const },
    { value: data.alerts.reviewsOutsideWindow, label: 'Documentos en revisión fuera de ventana', detail: 'La fecha configurada de revisión terminó y aún hay una etapa activa.', page: 'reporting' as const },
    { value: data.alerts.overdueActivities, label: 'Actividades con evidencias y plazos vencidos', detail: 'Se consideran los plazos personales y sus prórrogas.', page: 'reporting' as const },
    { value: data.alerts.pendingEvidence, label: 'Medios de verificación sin respaldo', detail: 'Planes aprobados en períodos activos.', page: 'reporting' as const },
  ].filter(alert => alert.value > 0) : []

  const kpis: { label: string; value: string | number; detail: string; icon: IconName; color: string; action: () => void }[] = data && metrics ? [
    { label: 'Personal institucional', value: data.users, detail: data.activeUsers + ' con acceso activo · ' + data.activeGroups + ' grupos activos', icon: 'users', color: 'blue', action: () => onOpen('users') },
    { label: 'Documentos', value: total, detail: metrics.plans + ' Planes · ' + metrics.reports + ' Informes', icon: 'document', color: 'blue', action: reports },
    { label: 'Evidencias pendientes', value: metrics.pendingEvidence, detail: 'Medios sin respaldo en Planes aprobados', icon: 'archive', color: 'amber', action: reports },
    { label: 'Avance declarado', value: metrics.declaredExecution === null ? 'Sin resultados' : formatPercent(metrics.declaredExecution), detail: metrics.declaredActivities + ' de ' + metrics.activities + ' actividades informadas · Cobertura ' + formatPercent(metrics.declarationCoverage), icon: 'check', color: 'teal', action: reports },
  ] : []

  return <section className="executive-overview" aria-label="Panel general administrativo">
    <header className="executive-heading">
      <div>
        <span className="section-kicker">SUPERVISIÓN INSTITUCIONAL</span>
        <h1>Panel general</h1>
        <p className="muted">Prioridades, ventanas de trabajo y estado de la gestión documental.</p>
      </div>
      <button type="button" className="secondary" disabled={!data && !error} onClick={reload}>
        <Icon name="refresh" size={17} />
        Actualizar panel
      </button>
    </header>

    <div className="executive-filter">
      <label>Período del panel
        <select aria-label="Período del panel" value={selection === 'auto' ? data?.selectedPeriodId ?? 'all' : selection} onChange={e => selectPeriod(e.target.value)}>
          <option value="all">Resumen institucional · Todos los períodos</option>
          {choices.map(period => <option key={period.id} value={period.id}>{period.name}{period.current ? ' · Vigente' : ''}</option>)}
        </select>
      </label>
      {data && <small className="muted">Actualizado {new Date(data.updatedAt).toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' })}</small>}
    </div>

    {error && (
      <>
        <Toast message={error} />
        <section className="card">
          <p>No se pudo consultar el panel. No se muestran indicadores incompletos.</p>
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center', marginTop: '12px' }}>
            <button type="button" className="secondary" onClick={reload}>Reintentar panel</button>
            {selection !== 'all' && (
              <button type="button" className="link" onClick={() => { setError(''); setSelection('all') }}>
                Consultar resumen institucional
              </button>
            )}
          </div>
        </section>
      </>
    )}

    {!data && !error && <p role="status">Cargando panel administrativo…</p>}

    {data && metrics && (
      <>
        <section className="executive-period card" aria-label="Período y ventanas de trabajo">
          <div className="executive-period-name">
            <span className="section-icon"><Icon name="calendar" /></span>
            <div>
              <span className="section-kicker">{selected ? selected.current ? 'PERÍODO VIGENTE' : 'PERÍODO ACTIVO SELECCIONADO' : 'RESUMEN INSTITUCIONAL'}</span>
              <h2>{selected?.name ?? 'Todos los períodos'}</h2>
              <p>{selected ? date(selected.startsOn) + ' — ' + date(selected.endsOn) : data.periods.length ? 'Selecciona un período para consultar sus ventanas. Puede haber varios vigentes.' : 'No hay períodos activos configurados.'}</p>
            </div>
          </div>
          {selected ? (
            <div className="executive-windows">
              {selected.windows.map(window => (
                <div key={window.label} className={'executive-window window-' + window.status.toLowerCase()}>
                  <strong>{window.label}</strong>
                  <span>{windowLabel(window)}</span>
                  {window.endsOn && <small>Hasta {date(window.endsOn)}</small>}
                </div>
              ))}
            </div>
          ) : (
            <button type="button" className="secondary" onClick={() => onOpen('periods')}>
              Gestionar períodos
              <Icon name="arrow" size={16} />
            </button>
          )}
          <p className="executive-window-note">Las ventanas pueden solaparse. Las prórrogas individuales se consultan en cada ámbito o actividad.</p>
        </section>

        <section className="executive-kpis" aria-label="Indicadores de gestión">
          {kpis.map(kpi => (
            <button type="button" key={kpi.label} className="executive-kpi" onClick={kpi.action}>
              <span className={'executive-icon tone-' + kpi.color}>
                <Icon name={kpi.icon} size={20} />
              </span>
              <span className="executive-kpi-label">{kpi.label}</span>
              <strong className={typeof kpi.value === 'string' && kpi.value === 'Sin resultados' ? 'no-result' : ''}>
                {kpi.value}
              </strong>
              <small>{kpi.detail}</small>
              <Icon name="arrow" size={15} className="executive-kpi-arrow" />
            </button>
          ))}
        </section>

        <section className="card executive-alerts" aria-label="Alertas operativas">
          <div className="executive-section-heading">
            <div>
              <span className="section-kicker">PRIORIDADES DE SEGUIMIENTO</span>
              <h2>Requiere atención</h2>
            </div>
            <span className="status-badge">Períodos activos{selected ? ' · ' + selected.name : ''}</span>
          </div>
          {alerts.length ? (
            <ul>
              {alerts.map(alert => (
                <li key={alert.label}>
                  <span className="executive-alert-count">{alert.value}</span>
                  <div>
                    <strong>{alert.label}</strong>
                    <p>{alert.detail}</p>
                  </div>
                  <button
                    type="button"
                    className="secondary table-action-icon-btn"
                    title={'Consultar ' + alert.label.toLowerCase()}
                    aria-label={'Consultar ' + alert.label.toLowerCase()}
                    onClick={() => alert.page === 'reporting' ? reports() : onOpen(alert.page)}
                  >
                    <Icon name="arrow" size={17} />
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <div className="executive-clear">
              <Icon name="check" size={20} />
              <p>Sin alertas en los controles consultados.</p>
            </div>
          )}
        </section>

        <div className="executive-chart-grid">
          <section className="card executive-lifecycle" aria-label="Estados documentales">
            <div className="executive-section-heading">
              <div>
                <span className="section-kicker">CICLO DOCUMENTAL</span>
                <h2>Estado de los documentos</h2>
              </div>
            </div>
            <div className="executive-ring-content">
              <svg viewBox="0 0 160 160" role="img" aria-label={total ? 'Distribución de ' + total + ' documentos por estado' : 'Sin documentos para este período'}>
                <circle className={'ring-track' + (total === 0 ? ' empty' : '')} cx="80" cy="80" r="58" fill="none" strokeWidth="16" />
                {segments.filter(segment => segment.value > 0).map(segment => (
                  <circle
                    key={segment.label}
                    className={'ring-segment stroke-' + segment.color}
                    cx="80"
                    cy="80"
                    r="58"
                    fill="none"
                    strokeWidth="16"
                    pathLength="100"
                    strokeDasharray={segment.dash + ' ' + (100 - segment.dash)}
                    strokeDashoffset={-(segment.start + gap / 2)}
                    transform="rotate(-90 80 80)"
                  />
                ))}
                <text className={'ring-total' + (total === 0 ? ' empty' : '')} x="80" y="78" textAnchor="middle">{total}</text>
                <text className="ring-caption" x="80" y="98" textAnchor="middle">{total === 1 ? 'documento' : 'documentos'}</text>
              </svg>
              <ul className="executive-legend">
                {states.map(state => (
                  <li key={state.label}>
                    <span className={'executive-dot tone-' + state.color} />
                    <span>{state.label}</span>
                    <strong>{state.value}</strong>
                    {total > 0 && <small className="legend-pct">({Math.round((state.value / total) * 100)}%)</small>}
                  </li>
                ))}
              </ul>
            </div>
          </section>

          <section className="card executive-types" aria-label="Documentos por tipo">
            <span className="section-kicker">COMPOSICIÓN DOCUMENTAL</span>
            <h2>Planes e informes</h2>
            <div className="executive-type-track" aria-hidden="true">
              {data.documentTypes.map(type => (
                <span key={type.type} className={'type-' + type.type} style={{ width: (total ? (type.count / total) * 100 : 0) + '%' }} />
              ))}
            </div>
            <ul className="executive-type-legend">
              {data.documentTypes.map(type => (
                <li key={type.type}>
                  <span className={'executive-type-dot type-' + type.type} />
                  <span>
                    {typeLabels[type.type] ?? type.type}
                    <small>{type.type}</small>
                  </span>
                  <strong>
                    {type.count}
                    {total > 0 && type.count > 0 && (
                      <span className="legend-pct"> ({Math.round((type.count / total) * 100)}%)</span>
                    )}
                  </strong>
                </li>
              ))}
            </ul>
            {total === 0 && <p className="muted" style={{ margin: '14px 0 0', fontSize: '0.84rem' }}>Aún no hay documentos en este resumen.</p>}
          </section>
        </div>

        <div className="executive-bottom-grid">
          <section className="card executive-evidence" aria-label="Respaldo documental por ámbito">
            <div className="executive-section-heading">
              <div>
                <span className="section-kicker">RESPALDO DOCUMENTAL</span>
                <h2>Evidencias por ámbito</h2>
              </div>
            </div>
            <p className="executive-help">Hasta cinco ámbitos, priorizando plazos vencidos y medios pendientes. El respaldo no equivale a ejecución física.</p>
            {rows.length ? (
              <ul>
                {rows.map(row => {
                  const coverage = row.activities ? (row.completedActivities / row.activities) * 100 : 0
                  return (
                    <li key={JSON.stringify([row.groupId, row.careerName, row.periodName])}>
                      <div className="executive-evidence-label">
                        <strong>{row.groupName}</strong>
                        <span>{formatPercent(coverage)}</span>
                      </div>
                      <small>{row.careerName} · {row.periodName}</small>
                      <div className="executive-coverage-track" aria-hidden="true">
                        <span style={{ width: coverage + '%' }} />
                      </div>
                      <div className="executive-evidence-note">
                        <span>{row.completedActivities} de {row.activities} actividades con evidencias completas</span>
                        {row.overdueActivities > 0 && <span className="executive-warning">{row.overdueActivities} con plazos vencidos</span>}
                      </div>
                    </li>
                  )
                })}
              </ul>
            ) : (
              <p className="muted">No hay actividades para comparar en este resumen.</p>
            )}
            <button type="button" className="secondary" onClick={reports}>
              Ver reportes de gestión
              <Icon name="arrow" size={16} />
            </button>
          </section>

          <section className="card executive-events" aria-label="Actividad administrativa reciente">
            <span className="section-kicker">AUDITORÍA GLOBAL</span>
            <h2>Actividad reciente</h2>
            <ul>
              {data.events.map(event => (
                <li key={event.id}>
                  <span className="status-badge">{category(event.action)}</span>
                  <strong>{auditActionLabels[event.action] ?? event.action}</strong>
                  <small>
                    {event.actorName ?? 'Sistema'} ·{' '}
                    <time dateTime={event.occurredAt}>
                      {new Date(event.occurredAt).toLocaleString('es-EC', { dateStyle: 'short', timeStyle: 'short' })}
                    </time>
                  </small>
                </li>
              ))}
            </ul>
            {data.events.length === 0 && <p className="muted">No hay actividad registrada.</p>}
            <button type="button" className="secondary" onClick={() => onOpen('audit')}>
              <Icon name="eye" size={16} />
              Abrir auditoría
            </button>
          </section>
        </div>
      </>
    )}
  </section>
}
