import './reporting-loading.css'

export function ReportingSkeleton() {
  return <section className="report-skeleton" aria-label="Cargando reporte" aria-busy="true">
    <p role="status">Consultando reportes…</p>
    <div className="report-skeleton-metrics" aria-hidden="true">{[0, 1, 2, 3].map(key => <div className="card report-skeleton-card" key={key}><span /><span /><span /></div>)}</div>
    <div className="report-skeleton-charts" aria-hidden="true">{[0, 1].map(key => <div className="card report-skeleton-card" key={key}><span /><div className="report-skeleton-plot" /></div>)}</div>
  </section>
}
