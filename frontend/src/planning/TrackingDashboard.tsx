import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
import type { PlanOptions, PlanPage } from './types'
interface Scope { id: string; name: string; documents: number }
interface Summary { documents: number; editableDrafts: number; readOnlyDocuments: number; withPreview: number; groups: Scope[]; periods: Scope[]; recent: PlanPage }
export function TrackingDashboard({ onOpen }: { onOpen: (id: string) => void }) {
  const [filters, setFilters] = useState({ groupId: '', periodId: '' }); const [data, setData] = useState<Summary | null>(null); const [options, setOptions] = useState<PlanOptions | null>(null); const [error, setError] = useState(''); const [reload, setReload] = useState(0)
  useEffect(() => { let active = true; const params = new URLSearchParams(); if (filters.groupId) params.set('groupId', filters.groupId); if (filters.periodId) params.set('periodId', filters.periodId)
    Promise.all([api<Summary>('/tracking?' + params), api<PlanOptions>('/work-plans/options')]).then(([summary, choices]) => { if (active) { setData(summary); setOptions(choices); setError('') } }).catch(e => { if (active) { setError(e.message); setData(null) } }); return () => { active = false }
  }, [filters, reload])
  function filter(key: 'groupId' | 'periodId', value: string) { setData(null); setFilters(f => ({ ...f, [key]: value })) }
  return <section className="card" aria-label="Seguimiento de mis documentos"><div className="section-heading"><span className="section-icon"><Icon name="document" /></span><div><span className="section-kicker">PANORAMA DOCUMENTAL</span><h2>Seguimiento de mis documentos</h2></div></div><p className="muted">Consulta tus Planes T1 disponibles y retoma la planificación.</p>
    <div className="grid"><label>Grupo del seguimiento<select aria-label="Grupo del seguimiento" value={filters.groupId} onChange={e => filter('groupId', e.target.value)}><option value="">Todos mis grupos</option>{options?.groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label><label>Período del seguimiento<select aria-label="Período del seguimiento" value={filters.periodId} onChange={e => filter('periodId', e.target.value)}><option value="">Todos los períodos</option>{options?.periods.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label></div>
    <button className="secondary" onClick={() => { setData(null); setReload(n => n + 1) }}>Actualizar seguimiento</button>{error && <p className="error" role="alert">{error}</p>}
    {!data && !error && <p role="status">Cargando seguimiento…</p>}
    {data && <><dl className="tracking-totals"><div><dt>Documentos disponibles</dt><dd>{data.documents}</dd></div><div><dt>Borradores editables</dt><dd>{data.editableDrafts}</dd></div><div><dt>Documentos de solo lectura</dt><dd>{data.readOnlyDocuments}</dd></div><div><dt>Con previsualización guardada</dt><dd>{data.withPreview}</dd></div></dl>
      <p className="field-help">Solo lectura considera el estado y la ventana de elaboración. Una previsualización puede corresponder a contenido anterior y no acredita firma ni aprobación.</p>
      <h3>Documentos recientes</h3>{data.recent.items.length === 0 ? <div className="empty-state"><Icon name="document" size={30} /><p>No hay documentos disponibles para estos filtros.</p><small>Al crear un Plan de Trabajo aparecerá en este espacio.</small></div> : <ul className="tracking-list">{data.recent.items.map(plan => <li key={plan.id}><span><strong>{plan.title}</strong><small>{plan.groupName} · {plan.periodName} · {plan.editable ? 'Borrador editable' : 'Solo lectura'}</small></span><button className="secondary" title={'Abrir ' + plan.title} aria-label={'Abrir desde seguimiento: ' + plan.title} onClick={() => onOpen(plan.id)}>Abrir<Icon name="arrow" size={16} /></button></li>)}</ul>}
      {data.documents > 0 && <div className="grid"><div><h3>Por grupo</h3><ul>{data.groups.map(scope => <li key={scope.id}>{scope.name}: {scope.documents}</li>)}</ul></div><div><h3>Por período</h3><ul>{data.periods.map(scope => <li key={scope.id}>{scope.name}: {scope.documents}</li>)}</ul></div></div>}
    </>}
  </section>
}
