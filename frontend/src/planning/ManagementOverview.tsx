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
    item.activities += row.activities; item.complete += row.completedActivities; grouped.set(row.groupId, item)
  })
  const groups = [...grouped.values()].filter(item => item.activities > 0).sort((a, b) => a.name.localeCompare(b.name, 'es'))
  const maximum = Math.max(1, ...groups.map(item => item.activities))
  return <section className="management-overview" aria-label="Indicadores de gestión">
    <div className="management-metrics">
      <article className="management-metric"><span className="metric-icon"><Icon name="document" /></span><span className="metric-label">Documentos</span><strong>{total}</strong><small>{data.plans} Planes · {data.reports} Informes</small></article>
      <article className="management-metric"><span className="metric-icon metric-amber"><Icon name="archive" /></span><span className="metric-label">Evidencias pendientes</span><strong>{data.pendingEvidence}</strong><small>Medios sin respaldo en Planes aprobados</small></article>
      <article className="management-metric"><span className="metric-icon"><Icon name="calendar" /></span><span className="metric-label">Actividades por vencer</span><strong>{data.dueSoonActivities}</strong><small>Hoy y próximos {data.dueSoonDays} días</small>{data.overdueActivities > 0 && <span className="management-overdue">{data.overdueActivities} con plazos vencidos</span>}</article>
      <article className="management-metric metric-execution"><span className="metric-icon metric-teal"><Icon name="check" /></span><span className="metric-label">Avance declarado</span><strong>{data.declaredExecution === null ? 'Sin resultados' : formatPercent(data.declaredExecution)}</strong><small>{data.declaredActivities} de {data.activities} actividades informadas</small><div className="management-meter" aria-hidden="true"><span style={{ width: data.declarationCoverage + '%' }} /></div><span className="management-coverage">Cobertura de resultados: {formatPercent(data.declarationCoverage)}</span></article>
    </div>
    <div className="management-charts">
      <section className="card management-chart" aria-label="Estados documentales"><div className="management-section-title"><span className="section-icon"><Icon name="columns" /></span><div><span className="section-kicker">ESTADO DOCUMENTAL</span><h3>De borrador a aprobación</h3></div></div>
        <div className="document-state-bar" aria-hidden="true">{states.map(state => <span key={state.key} className={'state-' + state.key} style={{ width: (total ? state.value / total * 100 : 0) + '%' }} />)}</div>
        <ul className="state-legend">{states.map(state => <li key={state.key}><span className={'legend-dot state-' + state.key} /><span>{state.label}</span><strong>{state.value}</strong></li>)}</ul>
        <div className="management-evidence-summary"><span><strong>{data.completedActivities} / {data.activities}</strong> actividades con respaldo completo</span><small>La evidencia documental y el avance declarado se muestran por separado.</small></div>
      </section>
      <section className="card management-chart" aria-label="Actividades por grupo"><div className="management-section-title"><span className="section-icon"><Icon name="users" /></span><div><span className="section-kicker">DISTRIBUCIÓN DEL TRABAJO</span><h3>Actividades por grupo</h3></div></div>
        {!groups.length ? <p className="management-empty">Todavía no hay actividades en los filtros seleccionados.</p> : <ul className="group-chart-list">{groups.slice(0, 6).map(group => <li key={group.name}><div><span>{group.name}</span><strong>{group.activities}</strong></div><div className="group-chart-track" aria-hidden="true"><span style={{ width: group.activities / maximum * 100 + '%' }} /></div><small>{group.complete} con evidencias completas</small></li>)}</ul>}
        {groups.length > 6 && <p className="field-help">Se muestran 6 de {groups.length} grupos. El reporte y la exportación incluyen todos.</p>}
      </section>
    </div>
    <section className="card management-deadlines" aria-label="Próximos vencimientos"><div className="management-section-title"><span className="section-icon"><Icon name="clock" /></span><div><span className="section-kicker">ATENCIÓN A LOS PLAZOS</span><h3>Próximos vencimientos</h3></div><span className="status-badge">Hasta {data.dueSoonDays} días</span></div>
      {!data.upcoming.length ? <p className="management-empty">{data.dueSoonActivities ? 'Los indicadores resumen los vencimientos. No hay detalles de actividades disponibles para su consulta.' : 'No hay actividades con evidencias pendientes próximas a vencer.'}</p> : <ul className="upcoming-activities">{data.upcoming.map(activity => <li key={activity.documentId + activity.activityId}><div className="upcoming-date"><Icon name="calendar" size={17} /><time dateTime={activity.deadline}>{new Date(activity.deadline + 'T12:00:00').toLocaleDateString('es-EC', { day: '2-digit', month: 'short' })}</time></div><div className="upcoming-description"><strong>{activity.activityTitle}</strong><small>{activity.groupName} · {activity.periodName}</small><span>{activity.missingEvidence} {activity.missingEvidence === 1 ? 'medio sin evidencia' : 'medios sin evidencia'}</span></div>{onOpenDocument && <button className="secondary management-open" type="button" title={'Abrir ejecución: ' + activity.activityTitle} aria-label={'Abrir ejecución: ' + activity.activityTitle} onClick={() => onOpenDocument(activity.documentId)}><Icon name="arrow" size={18} /></button>}</li>)}</ul>}
      <details className="management-method"><summary>Cómo se calculan estos indicadores</summary><p>El avance es la media de los últimos resultados aprobados por actividad; el Informe final prevalece sobre los cortes de ejecución. Las actividades sin resultado no se consideran ejecutadas ni se incluyen en esa media. La cobertura indica cuántas actividades planificadas tienen resultado aprobado.</p><p>Los pendientes y vencimientos corresponden a Planes aprobados, activos y sin archivar. Se consideran las prórrogas individuales; una actividad puede tener plazos vencidos y próximos para responsables distintos. El listado muestra hasta seis actividades autorizadas y su vencimiento próximo más cercano.</p></details>
    </section>
  </section>
}
