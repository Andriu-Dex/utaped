import { Icon } from '../shared/Icon'
import { formatPercent, type ReportingSummary } from './reportingTypes'
import './management-dashboard.css'

export function ManagementOverview({ data, onOpenDocument }: { data: ReportingSummary; onOpenDocument?: (id: string) => void }) {
  const states = [
    { key: 'draft', label: 'Borradores', value: data.drafts },
    { key: 'review', label: 'En revisión', value: data.inReview },
    { key: 'approved', label: 'Aprobados', value: data.approved },
  ]
  const total = data.plans + data.reports
  const grouped = new Map<string, { name: string; activities: number; complete: number }>()
  data.rows.forEach(row => {
    const item = grouped.get(row.groupId) ?? { name: row.groupName, activities: 0, complete: 0 }
    item.activities += row.activities
    item.complete += row.completedActivities
    grouped.set(row.groupId, item)
  })
  const groups = [...grouped.values()].filter(item => item.activities > 0).sort((a, b) => a.name.localeCompare(b.name, 'es'))
  const maximum = Math.max(1, ...groups.map(item => item.activities))

  return (
    <section className="management-overview" aria-label="Indicadores de gestión">
      <div className="management-metrics">
        <article className="management-metric">
          <span className="metric-icon">
            <Icon name="document" size={20} />
          </span>
          <span className="metric-label">Documentos</span>
          <strong>{total}</strong>
          <small>{data.plans} Planes · {data.reports} Informes</small>
        </article>

        <article className="management-metric">
          <span className="metric-icon metric-amber">
            <Icon name="archive" size={20} />
          </span>
          <span className="metric-label">Evidencias pendientes</span>
          <strong>{data.pendingEvidence}</strong>
          <small>Medios sin respaldo en Planes aprobados</small>
        </article>

        <article className="management-metric">
          <span className="metric-icon">
            <Icon name="calendar" size={20} />
          </span>
          <span className="metric-label">Actividades por vencer</span>
          <strong>{data.dueSoonActivities}</strong>
          <small>Hoy y próximos {data.dueSoonDays} días</small>
          {data.overdueActivities > 0 && (
            <span className="management-overdue">{data.overdueActivities} con plazos vencidos</span>
          )}
        </article>

        <article className="management-metric metric-execution">
          <span className="metric-icon metric-teal">
            <Icon name="check" size={20} />
          </span>
          <span className="metric-label">Avance declarado</span>
          <strong className={data.declaredExecution === null ? 'metric-empty-val' : ''}>
            {data.declaredExecution === null ? 'Sin resultados' : formatPercent(data.declaredExecution)}
          </strong>
          <small>{data.declaredActivities} de {data.activities} actividades informadas</small>
          <div className="management-meter" aria-hidden="true">
            <span style={{ width: data.declarationCoverage + '%' }} />
          </div>
          <span className="management-coverage">Cobertura de resultados: {formatPercent(data.declarationCoverage)}</span>
        </article>
      </div>

      <div className="management-charts">
        <section className="card management-chart" aria-label="Estados documentales">
          <div className="management-section-title">
            <span className="section-icon"><Icon name="columns" /></span>
            <div>
              <span className="section-kicker">ESTADO DOCUMENTAL</span>
              <h3>De borrador a aprobación</h3>
            </div>
          </div>
          <div className="document-state-bar" aria-hidden="true">
            {states.map(state => (
              <span
                key={state.key}
                className={'state-' + state.key}
                style={{ width: (total ? (state.value / total) * 100 : 0) + '%' }}
              />
            ))}
          </div>
          <ul className="state-legend">
            {states.map(state => (
              <li key={state.key}>
                <span className={'legend-dot state-' + state.key} />
                <span className="legend-label">{state.label}</span>
                <span className="legend-metrics">
                  <strong>{state.value}</strong>
                  {total > 0 && <small className="legend-pct">({Math.round((state.value / total) * 100)}%)</small>}
                </span>
              </li>
            ))}
          </ul>
          <div className="management-evidence-summary">
            <span className="evidence-summary-badge"><Icon name="check" size={16} /></span>
            <div>
              <span><strong>{data.completedActivities} / {data.activities}</strong> actividades con respaldo completo</span>
              <small>La evidencia documental y el avance declarado se muestran por separado.</small>
            </div>
          </div>
        </section>

        <section className="card management-chart" aria-label="Actividades por grupo">
          <div className="management-section-title">
            <span className="section-icon"><Icon name="users" /></span>
            <div>
              <span className="section-kicker">DISTRIBUCIÓN DEL TRABAJO</span>
              <h3>Actividades por grupo</h3>
            </div>
          </div>
          {!groups.length ? (
            <div className="management-empty-state">
              <span className="management-empty-icon"><Icon name="users" size={22} /></span>
              <p className="management-empty">Todavía no hay actividades en los filtros seleccionados.</p>
            </div>
          ) : (
            <ul className="group-chart-list">
              {groups.slice(0, 6).map(group => (
                <li key={group.name}>
                  <div className="group-header-row">
                    <span className="group-name">{group.name}</span>
                    <span className="group-count-badge">{group.activities} {group.activities === 1 ? 'actividad' : 'actividades'}</span>
                  </div>
                  <div className="group-chart-track" aria-hidden="true">
                    <span style={{ width: (group.activities / maximum) * 100 + '%' }} />
                  </div>
                  <div className="group-submeta">
                    <span className={group.complete > 0 ? 'group-complete-active' : 'group-complete-zero'}>
                      <Icon name="check" size={13} />
                      {group.complete} con evidencias completas
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
          {groups.length > 6 && (
            <p className="field-help">Se muestran 6 de {groups.length} grupos. El reporte y la exportación incluyen todos.</p>
          )}
        </section>
      </div>

      <section className="card management-deadlines" aria-label="Próximos vencimientos">
        <div className="management-section-title">
          <span className="section-icon"><Icon name="clock" /></span>
          <div>
            <span className="section-kicker">ATENCIÓN A LOS PLAZOS</span>
            <h3>Próximos vencimientos</h3>
          </div>
          <span className="status-badge">Hasta {data.dueSoonDays} días</span>
        </div>

        {!data.upcoming.length ? (
          <div className="management-empty-state">
            <span className="management-empty-icon"><Icon name="check" size={24} /></span>
            <div>
              <strong>Todo al día</strong>
              <p className="management-empty">
                {data.dueSoonActivities
                  ? 'Los indicadores resumen los vencimientos. No hay detalles de actividades disponibles para su consulta.'
                  : 'No hay actividades con evidencias pendientes próximas a vencer.'}
              </p>
            </div>
          </div>
        ) : (
          <ul className="upcoming-activities">
            {data.upcoming.map(activity => (
              <li key={activity.documentId + activity.activityId}>
                <div className="upcoming-date">
                  <Icon name="calendar" size={17} />
                  <time dateTime={activity.deadline}>
                    {new Date(activity.deadline + 'T12:00:00').toLocaleDateString('es-EC', { day: '2-digit', month: 'short' })}
                  </time>
                </div>
                <div className="upcoming-description">
                  <strong>{activity.activityTitle}</strong>
                  <small>{activity.groupName} · {activity.periodName}</small>
                  <span>{activity.missingEvidence} {activity.missingEvidence === 1 ? 'medio sin evidencia' : 'medios sin evidencia'}</span>
                </div>
                {onOpenDocument && (
                  <button
                    className="secondary management-open"
                    type="button"
                    title={'Abrir ejecución: ' + activity.activityTitle}
                    aria-label={'Abrir ejecución: ' + activity.activityTitle}
                    onClick={() => onOpenDocument(activity.documentId)}
                  >
                    <Icon name="arrow" size={18} />
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}

        <details className="management-method">
          <summary>
            <Icon name="columns" size={15} />
            Cómo se calculan estos indicadores
          </summary>
          <p>
            El avance es la media de los últimos resultados aprobados por actividad; el Informe final prevalece sobre los cortes de ejecución. Las actividades sin resultado no se consideran ejecutadas ni se incluyen en esa media. La cobertura indica cuántas actividades planificadas tienen resultado aprobado.
          </p>
          <p>
            Los pendientes y vencimientos corresponden a Planes aprobados, activos y sin archivar. Se consideran las prórrogas individuales; una actividad puede tener plazos vencidos y próximos para responsables distintos. El listado muestra hasta seis actividades autorizadas y su vencimiento próximo más cercano.
          </p>
        </details>
      </section>
    </section>
  )
}
