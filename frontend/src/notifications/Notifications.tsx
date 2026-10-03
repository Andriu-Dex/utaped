import { useEffect, useState } from 'react'
import { api } from '../shared/api'
interface Notification { id: string; kind: string; targetType: 'GROUP' | 'WORK_PLAN'; targetId: string; title: string; message: string; createdAt: string; readAt: string | null; targetAvailable: boolean }
interface NotificationPage { items: Notification[]; total: number; unread: number; page: number; size: number }
export interface NotificationTarget { type: 'GROUP' | 'WORK_PLAN'; id: string }
function changed() { window.dispatchEvent(new Event('notifications-changed')) }
export function NotificationIndicator({ onOpen }: { onOpen: () => void }) {
  const [count, setCount] = useState<number | null>(null)
  useEffect(() => { let active = true; const refresh = () => { if (document.visibilityState === 'visible') api<{ unread: number }>('/notifications/unread-count').then(data => { if (active) setCount(data.unread) }).catch(() => { if (active) setCount(null) }) }; refresh(); const timer = window.setInterval(refresh, 60000); window.addEventListener('notifications-changed', refresh); window.addEventListener('focus', refresh); document.addEventListener('visibilitychange', refresh); return () => { active = false; window.clearInterval(timer); window.removeEventListener('notifications-changed', refresh); window.removeEventListener('focus', refresh); document.removeEventListener('visibilitychange', refresh) } }, [])
  return <button title="Abrir notificaciones" aria-label={count === null ? 'Notificaciones' : 'Notificaciones: ' + count + ' sin leer'} onClick={onOpen}>Notificaciones{count !== null && ' (' + count + ')'}</button>
}
export function Notifications({ onOpen }: { onOpen: (target: NotificationTarget) => Promise<void> }) {
  const [filter, setFilter] = useState({ read: '', page: 0 }); const [result, setResult] = useState<NotificationPage | null>(null); const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false); const [reload, setReload] = useState(0)
  useEffect(() => { let active = true; const params = new URLSearchParams({ page: String(filter.page) }); if (filter.read) params.set('read', filter.read)
    api<NotificationPage>('/notifications?' + params).then(data => { if (active) { setResult(data) } }).catch(e => { if (active) { setError(e.message); setResult(null) } }); return () => { active = false }
  }, [filter, reload])
  async function mutate(path: string, open = false) {
    setBusy(true); setError(''); setNotice('')
    try { const target = await api<NotificationTarget>(path, 'POST'); changed(); setFilter(f => ({ ...f, page: 0 })); setReload(n => n + 1); if (open) await onOpen(target); else setNotice('Estado de lectura actualizado.') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo actualizar la notificación.'); setReload(n => n + 1) } finally { setBusy(false) }
  }
  return <section aria-label="Bandeja de notificaciones"><h1>Notificaciones</h1><p>Avisos personales sobre pertenencias y previsualizaciones guardadas. La disponibilidad del objeto depende de sus permisos actuales.</p>
    <label>Estado de lectura<select aria-label="Estado de lectura" value={filter.read} onChange={e => { setError(''); setResult(null); setFilter({ read: e.target.value, page: 0 }) }}><option value="">Todas</option><option value="false">Sin leer</option><option value="true">Leídas</option></select></label>
    <button className="secondary" disabled={busy} onClick={() => { setError(''); setResult(null); setReload(n => n + 1); changed() }}>Actualizar notificaciones</button>{' '}<button disabled={busy || !result?.unread} onClick={() => void mutate('/notifications/read-all')}>Marcar todas como leídas</button>
    {error && <p role="alert" className="error">{error}</p>}{notice && <p role="status" className="notice">{notice}</p>}
    {!result && !error && <p role="status">Cargando notificaciones…</p>}
    {result && <><p>{result.total} notificaciones · {result.unread} sin leer · Página {result.page + 1}</p>{result.items.length === 0 && <p>No hay notificaciones para este filtro.</p>}
      <ul className="notification-list">{result.items.map(notification => <li className={'card ' + (!notification.readAt ? 'notification-unread' : '')} key={notification.id} aria-label={notification.title}><h2>{notification.title}</h2><p>{notification.message}</p><p><time dateTime={notification.createdAt}>{new Date(notification.createdAt).toLocaleString('es-EC')}</time> · {notification.readAt ? 'Leída' : 'Sin leer'}</p>
        <button className="secondary" title="Abrir el objeto de la notificación" disabled={busy || !notification.targetAvailable} onClick={() => void mutate('/notifications/' + notification.id + '/open', true)}>Abrir objeto</button>{' '}{!notification.readAt && <button className="secondary" disabled={busy} onClick={() => void mutate('/notifications/' + notification.id + '/read')}>Marcar como leída</button>}
        {!notification.targetAvailable && <p>Objeto no disponible con sus permisos actuales. El aviso permanece como historial.</p>}
      </li>)}</ul><div className="pagination"><button className="secondary" disabled={busy || result.page === 0} onClick={() => { setResult(null); setFilter(f => ({ ...f, page: f.page - 1 })) }}>Notificaciones anteriores</button><button className="secondary" disabled={busy || (result.page + 1) * result.size >= result.total} onClick={() => { setResult(null); setFilter(f => ({ ...f, page: f.page + 1 })) }}>Notificaciones siguientes</button></div>
    </>}
  </section>
}
