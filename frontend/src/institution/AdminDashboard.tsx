import { useEffect, useState } from 'react'
import { api, type Group, type Period } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import { auditActionLabels } from '../audit/auditLabels'
import type { AdminPage } from './adminNavigation'
import './administration.css'

interface Summary {
  users: number; activeUsers: number; groups: Group[]; periods: Period[];
  events: { id: string; action: string; actorName: string | null; occurredAt: string }[]
}
export function AdminDashboard({ onOpen }: { onOpen: (page: AdminPage) => void }) {
  const [data, setData] = useState<Summary | null>(null)
  const [error, setError] = useState(''); const [refresh, setRefresh] = useState(0)
  useEffect(() => {
    let active = true
    Promise.all([api<{ total: number }>('/admin/users/directory?size=1'), api<{ total: number }>('/admin/users/directory?size=1&active=true'), api<Group[]>('/admin/groups'), api<Period[]>('/periods'), api<{ items: Summary['events'] }>('/admin/audit-events?page=0')])
      .then(([users, activeUsers, groups, periods, events]) => { if (active) { setData({ users: users.total, activeUsers: activeUsers.total, groups, periods, events: events.items.slice(0,5) }); setError('') } })
      .catch(e => { if (active) setError(e instanceof Error ? e.message : 'No se pudo cargar el panel administrativo.') })
    return () => { active = false }
  }, [refresh])
  return <section aria-label="Panel general administrativo">
    <div className="admin-overview-heading"><div><span className="section-kicker">CONFIGURACIÓN Y CONTROL</span><h1>Panel general</h1><p className="muted">Administra cuentas, organización y configuración institucional.</p></div><button className="secondary" onClick={() => { setData(null); setError(''); setRefresh(value => value + 1) }}><Icon name="refresh" />Actualizar panel</button></div>
    {error && <Toast message={error} />}
    {!data && !error && <p role="status">Cargando panel administrativo…</p>}
    {data && <>
      <div className="admin-metrics">
        {([{ label: 'Usuarios registrados', value: data.users, page: 'users' }, { label: 'Usuarios activos', value: data.activeUsers, page: 'users' }, { label: 'Grupos activos', value: data.groups.filter(group => group.active).length, page: 'groups' }, { label: 'Períodos registrados', value: data.periods.length, page: 'periods' }] as const).map(metric => <button className="secondary admin-metric" key={metric.label} onClick={() => onOpen(metric.page)}><span>{metric.label}</span><strong>{metric.value}</strong><small>Consultar<Icon name="arrow" size={14} /></small></button>)}
      </div>
      <div className="grid"><section className="card"><h2>Configuración institucional</h2><p className="muted">Accede al módulo que necesitas gestionar.</p><div className="admin-shortcuts">{([{ label: 'Usuarios y acceso', page: 'users' }, { label: 'Grupos y pertenencias', page: 'groups' }, { label: 'Catálogo de actividades', page: 'activities' }, { label: 'Recursos y medios', page: 'resources' }, { label: 'Flujos de aprobación', page: 'workflows' }, { label: 'Feriados y restricciones', page: 'holidays' }] as const).map(item => <button className="secondary" key={item.page} onClick={() => onOpen(item.page)}>{item.label}<Icon name="arrow" size={16} /></button>)}</div></section>
      <section className="card"><h2>Actividad reciente</h2>{data.events.length ? <ul className="admin-events">{data.events.map(event => <li key={event.id}><strong>{auditActionLabels[event.action] ?? event.action}</strong><span>{event.actorName ?? 'Sistema'}</span><time dateTime={event.occurredAt}>{new Date(event.occurredAt).toLocaleString('es-EC')}</time></li>)}</ul> : <p>No hay actividad registrada.</p>}<button className="secondary" onClick={() => onOpen('audit')}>Abrir auditoría</button></section></div>
    </>}
  </section>
}
