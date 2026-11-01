import { UpcomingActivities } from './UpcomingActivities'
import { useEffect, useState } from 'react'
import { api, download, type Group, type Period } from '../shared/api'
import { Toast } from '../shared/Toast'
import { Icon } from '../shared/Icon'
import { ManagementOverview } from './ManagementOverview'
import { formatPercent, type ReportingSummary } from './reportingTypes'
import { ReportingSkeleton } from './ReportingSkeleton'

export function ReportingDashboard({ administrative = false, onOpenDocument, initialPeriodId = '', onPeriodChange }: { administrative?: boolean; initialPeriodId?: string; onOpenDocument?: (id: string) => void; onPeriodChange?: (id: string) => void }) {
  const [snapshot, setSnapshot] = useState<{ key: string; value: ReportingSummary } | null>(null)
  const [groups, setGroups] = useState<Group[]>([]), [periods, setPeriods] = useState<Period[]>([])
  const [error, setError] = useState(''), [loading, setLoading] = useState(true), [reload, setReload] = useState(0)
  const [filters, setFilters] = useState({ groupId: '', periodId: initialPeriodId, archived: false })
  const [exporting, setExporting] = useState(false), [exportError, setExportError] = useState('')
  const [catalogError, setCatalogError] = useState(''), [catalogReload, setCatalogReload] = useState(0)
  const query = new URLSearchParams({ archived: String(filters.archived), ...(filters.groupId ? { groupId: filters.groupId } : {}), ...(filters.periodId ? { periodId: filters.periodId } : {}) }).toString()

  const requestKey = String(administrative) + ':' + query
  const data = snapshot?.key === requestKey ? snapshot.value : null

  const [previousInitialPeriod, setPreviousInitialPeriod] = useState(initialPeriodId)
  if (previousInitialPeriod !== initialPeriodId) {
    setPreviousInitialPeriod(initialPeriodId)
    if (filters.periodId !== initialPeriodId) {
      setFilters({ ...filters, periodId: initialPeriodId })
      setLoading(true)
      setError('')
    }
  }

  // Catalogs do not depend on report filters or manual report refreshes.
  useEffect(() => {
    let active = true
    Promise.all([api<Group[]>(administrative ? '/admin/groups' : '/groups'), api<Period[]>('/periods')])
      .then(([g, p]) => { if (active) { setGroups(g); setPeriods(p); setCatalogError('') } })
      .catch(e => { if (active) { setGroups([]); setPeriods([]); setCatalogError(e.message) } })
    return () => { active = false }
  }, [administrative, catalogReload])

  useEffect(() => {
    let active = true
    api<ReportingSummary>('/reporting?' + query)
      .then(summary => { if (active) { setSnapshot({ key: requestKey, value: summary }); setError('') } })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [query, requestKey, reload])

  const refresh = () => { setLoading(true); setError(''); setReload(value => value + 1) }
  const change = (next: typeof filters) => {
    if (next.groupId === filters.groupId && next.periodId === filters.periodId && next.archived === filters.archived) return
    setLoading(true); setError(''); setFilters(next)
    onPeriodChange?.(next.periodId)
  }

  async function exportReport() {
    setExporting(true); setExportError('')
    try { await download('/reporting/export?' + query, 'seguimiento-utaped.csv') }
    catch (e) { setExportError(e instanceof Error ? e.message : 'No se pudo exportar el reporte.') }
    finally { setExporting(false) }
  }

  return <section>
    <header className="plan-wizard-header management-page-header">
      <div>
        <span className="section-kicker">SEGUIMIENTO INSTITUCIONAL</span>
        <div className="plan-header-title-row">
          <h1>Reportes de gestión</h1>
        </div>
        <p>{administrative ? 'Panorama institucional de la planificación y sus resultados.' : 'Planificación y resultados de los documentos que puedes consultar.'}</p>
      </div>
      <button type="button" className="secondary" disabled={!data || loading || exporting} onClick={() => void exportReport()}>
        <Icon name="download" size={17} />
        {exporting ? 'Exportando…' : 'Exportar CSV'}
      </button>
    </header>

    {error && <Toast message={error} onDismiss={() => setError('')} />}
    {exportError && <Toast message={exportError} onDismiss={() => setExportError('')} />}

    <section className="card" style={{ marginBottom: '22px' }}>
      <div className="management-filters">
        <label>Grupo del reporte
          <select aria-label="Grupo del reporte" value={filters.groupId} onChange={e => change({ ...filters, groupId: e.target.value })}>
            <option value="">Todos disponibles</option>
            {groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}
          </select>
        </label>
        <label>Período del reporte
          <select aria-label="Período del reporte" value={filters.periodId} onChange={e => change({ ...filters, periodId: e.target.value })}>
            <option value="">Todos los períodos</option>
            {periods.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </label>
        <button className="secondary" type="button" disabled={loading} onClick={refresh}>
          <Icon name="refresh" size={16} />Actualizar
        </button>
      </div>
      <label className="check" style={{ marginTop: '14px', paddingTop: '10px', borderTop: '1px solid var(--border)' }}>
        <input type="checkbox" checked={filters.archived} onChange={e => change({ ...filters, archived: e.target.checked })} />
        Incluir versiones archivadas
      </label>
    </section>

    {catalogError && <div className="report-loading-notice" role="status"><span>No se pudieron cargar los filtros. El reporte se consulta de forma independiente.</span><button type="button" className="secondary" onClick={() => { setCatalogError(''); setCatalogReload(n => n + 1) }}>Reintentar filtros</button></div>}
    {data && loading && <p role="status" className="report-loading-notice">Actualizando reporte…</p>}
    {data && error && !loading && <div className="report-loading-notice" role="status"><span>No se pudo actualizar. Se conserva la última consulta de estos filtros.</span><button type="button" className="secondary" onClick={refresh}>Reintentar reportes</button></div>}
    {!data && loading ? <ReportingSkeleton /> : !data ? (
      <section className="empty-state" aria-label="Reportes no disponibles">
        <p>No se pudieron cargar los reportes.</p>
        <button type="button" className="secondary" onClick={refresh}>Reintentar reportes</button>
      </section>
    ) : (
      <>
        <ManagementOverview data={data} onOpenDocument={onOpenDocument} />
        {<UpcomingActivities key={query} groupId={filters.groupId} periodId={filters.periodId} onOpenDocument={onOpenDocument} />}
        <section className="card">
          <div className="section-heading">
            <span className="section-icon"><Icon name="users" /></span>
            <div>
              <span className="section-kicker">DETALLE PARA ANÁLISIS</span>
              <h2>Resultados documentales</h2>
            </div>
            {data.rows.length > 0 && (
              <span className="status-badge" style={{ marginLeft: 'auto' }}>
                {data.rows.length} {data.rows.length === 1 ? 'ámbito consultado' : 'ámbitos consultados'}
              </span>
            )}
          </div>
          {!data.rows.length ? (
            <div className="empty-state">
              <Icon name="document" size={32} />
              <p>No hay documentos disponibles para estos filtros.</p>
            </div>
          ) : (
            <div className="table-scroll" role="region" aria-label="Detalle de resultados documentales" tabIndex={0}>
              <table className="reporting-detail-table">
                <thead>
                  <tr>
                    <th>Grupo / carrera / período</th>
                    <th>Planes / Informes</th>
                    <th>Aprobados</th>
                    <th>En revisión</th>
                    <th>Borradores</th>
                    <th>Actividades con respaldo</th>
                    <th>Cobertura documental</th>
                    <th>Avance declarado</th>
                    <th>Evidencias pendientes</th>
                    <th>Próximas / vencidas</th>
                  </tr>
                </thead>
                <tbody>
                  {data.rows.map((r, i) => (
                    <tr key={i}>
                      <td>
                        <strong>{r.groupName}</strong>
                        <small className="review-caption">{r.careerName} · {r.periodName}</small>
                      </td>
                      <td>
                        <span className="reporting-ratio-badge">{r.plans} / {r.reports}</span>
                      </td>
                      <td>
                        <span className="status-badge active" style={{ fontSize: '0.76rem', padding: '2px 8px' }}>{r.approved}</span>
                      </td>
                      <td>
                        <span className="status-badge pending" style={{ fontSize: '0.76rem', padding: '2px 8px' }}>{r.inReview}</span>
                      </td>
                      <td>
                        <span className="status-badge" style={{ fontSize: '0.76rem', padding: '2px 8px', background: 'var(--blue-50)', color: 'var(--blue-700)', borderColor: 'rgba(23, 95, 148, 0.2)' }}>{r.drafts}</span>
                      </td>
                      <td>
                        <strong>{r.completedActivities} / {r.activities}</strong>
                      </td>
                      <td>
                        <div className="reporting-cell-metric">
                          <span className="reporting-coverage-val">{formatPercent(r.evidenceCoverage)}</span>
                          <div className="reporting-mini-track" aria-hidden="true">
                            <span style={{ width: Math.min(100, Math.max(0, r.evidenceCoverage)) + '%' }} />
                          </div>
                        </div>
                      </td>
                      <td>
                        <strong>{r.declaredExecution === null ? 'Sin resultado aprobado' : formatPercent(r.declaredExecution)}</strong>
                        <span className="reporting-result-coverage">{r.declaredActivities} de {r.activities} actividades informadas</span>
                      </td>
                      <td>
                        {r.pendingEvidence > 0 ? (
                          <span className="status-badge" style={{ background: 'var(--amber-100)', color: 'var(--amber-700)', borderColor: 'rgba(217, 119, 6, 0.25)' }}>
                            {r.pendingEvidence}
                          </span>
                        ) : (
                          <span className="muted">0</span>
                        )}
                      </td>
                      <td>
                        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                          <span className="reporting-due-soon" title="Próximas a vencer">{r.dueSoonActivities}</span>
                          <span className="muted">/</span>
                          <span className={'reporting-overdue-tag ' + (r.overdueActivities > 0 ? 'has-overdue' : '')} title="Vencidas">
                            {r.overdueActivities}
                          </span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </>
    )}
  </section>
}
