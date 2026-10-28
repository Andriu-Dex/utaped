import { useEffect, useState } from 'react'
import { api, download, type Group, type Period } from '../shared/api'
import { Toast } from '../shared/Toast'
import { Icon } from '../shared/Icon'
import { ManagementOverview } from './ManagementOverview'
import { formatPercent, type ReportingSummary } from './reportingTypes'

export function ReportingDashboard({ administrative = false, onOpenDocument }: { administrative?: boolean; onOpenDocument?: (id: string) => void }) {
  const [data, setData] = useState<ReportingSummary | null>(null)
  const [groups, setGroups] = useState<Group[]>([]), [periods, setPeriods] = useState<Period[]>([])
  const [error, setError] = useState(''), [loading, setLoading] = useState(true), [reload, setReload] = useState(0)
  const [filters, setFilters] = useState({ groupId: '', periodId: '', archived: false })
  const [exporting, setExporting] = useState(false)
  const query = new URLSearchParams({ archived: String(filters.archived), ...(filters.groupId ? { groupId: filters.groupId } : {}), ...(filters.periodId ? { periodId: filters.periodId } : {}) }).toString()
  useEffect(() => {
    let active = true
    Promise.all([api<Group[]>(administrative ? '/admin/groups' : '/groups'), api<Period[]>('/periods'), api<ReportingSummary>('/reporting?' + query)])
      .then(([g, p, summary]) => { if (active) { setGroups(g); setPeriods(p); setData(summary); setError('') } })
      .catch(e => { if (active) { setError(e.message); setData(null) } })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [query, administrative, reload])
  const change = (next: typeof filters) => { setData(null); setLoading(true); setError(''); setFilters(next) }
  async function exportReport() {
    setExporting(true); setError('')
    try { await download('/reporting/export?' + query, 'seguimiento-utaped.csv') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo exportar el reporte.') }
    finally { setExporting(false) }
  }
  return <section>
    <header className="plan-wizard-header management-page-header"><div><span className="section-kicker">SEGUIMIENTO INSTITUCIONAL</span><div className="plan-header-title-row"><h1>Reportes de gestión</h1></div><p>{administrative ? 'Panorama institucional de la planificación y sus resultados.' : 'Planificación y resultados de los documentos que puedes consultar.'}</p></div><button type="button" className="secondary" disabled={!data || loading || exporting} onClick={() => void exportReport()}><Icon name="download" size={17} />{exporting ? 'Exportando…' : 'Exportar CSV'}</button></header>
    {error && <Toast message={error} onDismiss={() => setError('')} />}
    <section className="card"><div className="management-filters"><label>Grupo del reporte<select value={filters.groupId} onChange={e => change({ ...filters, groupId: e.target.value })}><option value="">Todos disponibles</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label><label>Período del reporte<select value={filters.periodId} onChange={e => change({ ...filters, periodId: e.target.value })}><option value="">Todos los períodos</option>{periods.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label><button className="secondary" type="button" disabled={loading} onClick={() => { setLoading(true); setError(''); setReload(value => value + 1) }}><Icon name="refresh" size={16} />Actualizar</button></div><label className="check"><input type="checkbox" checked={filters.archived} onChange={e => change({ ...filters, archived: e.target.checked })} />Incluir versiones archivadas</label></section>
    {loading ? <p role="status">Consultando reportes…</p> : !data ? <section className="empty-state" aria-label="Reportes no disponibles"><p>No se pudieron cargar los reportes.</p><button type="button" className="secondary" onClick={() => { setLoading(true); setError(''); setReload(n => n + 1) }}>Reintentar reportes</button></section> : <>
      <ManagementOverview data={data} onOpenDocument={onOpenDocument} />
      <section className="card"><div className="section-heading"><span className="section-icon"><Icon name="users" /></span><div><span className="section-kicker">DETALLE PARA ANÁLISIS</span><h2>Resultados documentales</h2></div></div>
        {!data.rows.length ? <div className="empty-state"><Icon name="document" size={32} /><p>No hay documentos disponibles para estos filtros.</p></div> : <div className="table-scroll" role="region" aria-label="Detalle de resultados documentales" tabIndex={0}><table className="reporting-detail-table"><thead><tr><th>Grupo / carrera / período</th><th>Planes / Informes</th><th>Aprobados</th><th>En revisión</th><th>Borradores</th><th>Actividades con respaldo</th><th>Cobertura documental</th><th>Avance declarado</th><th>Evidencias pendientes</th><th>Próximas / vencidas</th></tr></thead><tbody>{data.rows.map((r, i) => <tr key={i}><td><strong>{r.groupName}</strong><small className="review-caption">{r.careerName} · {r.periodName}</small></td><td>{r.plans} / {r.reports}</td><td>{r.approved}</td><td>{r.inReview}</td><td>{r.drafts}</td><td>{r.completedActivities} / {r.activities}</td><td>{formatPercent(r.evidenceCoverage)}</td><td><strong>{r.declaredExecution === null ? 'Sin resultado aprobado' : formatPercent(r.declaredExecution)}</strong><span className="reporting-result-coverage">{r.declaredActivities} de {r.activities} actividades informadas</span></td><td>{r.pendingEvidence}</td><td>{r.dueSoonActivities} / {r.overdueActivities}</td></tr>)}</tbody></table></div>}
      </section>
    </>}
  </section>
}
