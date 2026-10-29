import { Toast } from '../shared/Toast'
import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { ManagementOverview } from './ManagementOverview'
import { PendingInbox } from './PendingInbox'
import type { ReportingSummary } from './reportingTypes'
import { Icon } from '../shared/Icon'
import type { PlanOptions, PlanPage } from './types'
interface Summary { documents: number; editableDrafts: number; recent: PlanPage }
export function TrackingDashboard({ onOpen, onOpenReports }: { onOpen: (id: string, step?: string) => void; onOpenReports: () => void }) {
  const [metrics, setMetrics] = useState<ReportingSummary | null>(null)
  const [filters, setFilters] = useState({ groupId: '', periodId: '' })
  const [data, setData] = useState<Summary | null>(null), [options, setOptions] = useState<PlanOptions | null>(null)
  const [error, setError] = useState(''), [reload, setReload] = useState(0)
  useEffect(() => {
    let active = true
    const params = new URLSearchParams()
    if (filters.groupId) params.set('groupId', filters.groupId)
    if (filters.periodId) params.set('periodId', filters.periodId)
    Promise.all([api<Summary>('/tracking?' + params), api<PlanOptions>('/work-plans/options'), api<ReportingSummary>('/reporting?' + params)])
      .then(([summary, choices, report]) => { if (active) { setData(summary); setOptions(choices); setMetrics(report); setError('') } })
      .catch(e => { if (active) { setError(e.message); setData(null); setMetrics(null) } })
    return () => { active = false }
  }, [filters, reload])
  function filter(key: 'groupId' | 'periodId', value: string) { setData(null); setFilters(f => ({ ...f, [key]: value })) }
  return <section className="management-home" aria-label="Seguimiento de mis documentos">
    <PendingInbox onOpen={onOpen} />
    <header className="management-page-header"><div><span className="section-kicker">PANORAMA DOCUMENTAL</span><h2>Seguimiento de mis documentos</h2><p>El estado de tus documentos y los próximos compromisos.</p></div><button type="button" className="secondary" onClick={onOpenReports}><Icon name="document" size={17} />Ver reportes de gestión</button></header>
    <div className="management-filters"><label>Grupo del seguimiento<select value={filters.groupId} onChange={e => filter('groupId', e.target.value)}><option value="">Todos mis grupos</option>{options?.groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label><label>Período del seguimiento<select value={filters.periodId} onChange={e => filter('periodId', e.target.value)}><option value="">Todos los períodos</option>{options?.periods.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label><button className="secondary" type="button" onClick={() => { setData(null); setReload(n => n + 1) }}><Icon name="refresh" size={16} />Actualizar seguimiento</button></div>
    {error && <><Toast message={error} /><p className="field-help">No se pudo cargar el seguimiento. Vuelve a intentarlo con Actualizar seguimiento.</p></>}
    {!data && !error && <p role="status">Cargando seguimiento…</p>}
    {data && metrics && <><ManagementOverview data={metrics} onOpenDocument={id => onOpen(id, 'execution')} />
      <section className="card"><div className="management-page-header"><div><span className="section-kicker">TU ESPACIO DE TRABAJO</span><h3>Documentos recientes</h3></div><span className="status-badge">{metrics.drafts} borradores</span></div>
        {data.recent.items.length === 0 ? <div className="empty-state"><Icon name="document" size={30} /><p>No hay documentos disponibles para estos filtros.</p><small>Al crear un Plan de Trabajo aparecerá en este espacio.</small></div> : <ul className="tracking-list">{data.recent.items.map(plan => <li key={plan.id}><span><div className="doc-title-cell"><strong>{plan.title}</strong><span className={'status-badge ' + (plan.editable ? 'status-draft' : 'status-archived')}>{plan.editable ? 'Borrador editable' : 'Solo lectura'}</span></div><small>{plan.groupName} · {plan.periodName}</small></span><button className="secondary" title={'Abrir ' + plan.title} aria-label={'Abrir desde seguimiento: ' + plan.title} onClick={() => onOpen(plan.id)}>Abrir<Icon name="arrow" size={16} /></button></li>)}</ul>}
      </section>
    </>}
  </section>
}
