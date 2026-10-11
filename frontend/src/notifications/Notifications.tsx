import { Toast } from '../shared/Toast'
import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
interface Notification { id: string; kind: string; targetType: 'GROUP' | 'WORK_PLAN'; targetId: string; title: string; message: string; createdAt: string; readAt: string | null; targetAvailable: boolean }
interface NotificationPage { items: Notification[]; total: number; unread: number; page: number; size: number }
export interface NotificationTarget { type: 'GROUP' | 'WORK_PLAN'; id: string }
function changed() { window.dispatchEvent(new Event('notifications-changed')) }
export function NotificationIndicator({ onOpenTarget }: { onOpenTarget: (target: NotificationTarget) => Promise<void> }) {
  const [count, setCount] = useState<number | null>(null)
  const [isOpen, setIsOpen] = useState(false)
  useEffect(() => { let active = true; const refresh = () => { if (document.visibilityState === 'visible') api<{ unread: number }>('/notifications/unread-count').then(data => { if (active) setCount(data.unread) }).catch(() => { if (active) setCount(null) }) }; refresh(); const timer = window.setInterval(refresh, 60000); window.addEventListener('notifications-changed', refresh); window.addEventListener('focus', refresh); document.addEventListener('visibilitychange', refresh); return () => { active = false; window.clearInterval(timer); window.removeEventListener('notifications-changed', refresh); window.removeEventListener('focus', refresh); document.removeEventListener('visibilitychange', refresh) } }, [])
  return <div style={{ position: 'relative' }}>
    <button className="icon-button" style={{ position: 'relative', width: 42, height: 42, padding: 0 }} title="Notificaciones" aria-label={count === null ? 'Notificaciones' : 'Notificaciones: ' + count + ' sin leer'} onClick={() => setIsOpen(!isOpen)}><Icon name="bell" size={22} />
      {count !== null && count > 0 && <span style={{ position: 'absolute', top: 0, right: 0, background: 'var(--danger)', color: 'white', fontSize: '.6rem', fontWeight: 800, padding: '3px 6px', borderRadius: 12, lineHeight: 1, border: '2px solid var(--surface)' }}>{count > 99 ? '99+' : count}</span>}
    </button>
    {isOpen && <div className="notification-popover" style={{ position: 'absolute', top: 'calc(100% + 16px)', right: 0, width: 'clamp(320px, 90vw, 440px)', maxHeight: '75vh', overflowY: 'auto', background: 'var(--surface)', border: '1px solid var(--border)', boxShadow: '0 12px 32px rgba(0,0,0,0.12)', borderRadius: 16, zIndex: 100, padding: 16 }}>
      <Notifications onOpen={async (t) => { setIsOpen(false); await onOpenTarget(t); }} onClose={() => setIsOpen(false)} />
    </div>}
  </div>
}
export function Notifications({ onOpen, onClose }: { onOpen: (target: NotificationTarget) => Promise<void>; onClose?: () => void }) {
  const [filter, setFilter] = useState({ read: '', page: 0 }); const [result, setResult] = useState<NotificationPage | null>(null); const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false); const [reload, setReload] = useState(0)
  useEffect(() => { let active = true; const params = new URLSearchParams({ page: String(filter.page) }); if (filter.read) params.set('read', filter.read)
    api<NotificationPage>('/notifications?' + params).then(data => { if (active) { setResult(data) } }).catch(e => { if (active) { setError(e.message); setResult(null) } }); return () => { active = false }
  }, [filter, reload])
  async function mutate(path: string, open = false) {
    setBusy(true); setError(''); setNotice('')
    try { const target = await api<NotificationTarget>(path, 'POST'); changed(); setFilter(f => ({ ...f, page: 0 })); setReload(n => n + 1); if (open) await onOpen(target); else setNotice('Estado de lectura actualizado.') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo actualizar la notificación.'); setReload(n => n + 1) } finally { setBusy(false) }
  }
  return <section aria-label="Bandeja de notificaciones" className="notifications-container">
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
      <h2 style={{ margin: 0, fontSize: '1.2rem' }}>Notificaciones</h2>
      <div style={{ display: 'flex', gap: 8 }}>
        <button className="icon-button" disabled={busy || !result?.unread} onClick={() => void mutate('/notifications/read-all')} title="Marcar todas como leídas" aria-label="Marcar todas como leídas"><Icon name="check" size={16} /></button>
        <button className="icon-button" disabled={busy} onClick={() => { setError(''); setResult(null); setReload(n => n + 1); changed() }} title="Actualizar" aria-label="Actualizar"><Icon name="refresh" size={16} /></button>
        {onClose && <button className="icon-button" onClick={onClose} title="Cerrar" aria-label="Cerrar"><Icon name="x" size={16} /></button>}
      </div>
    </div>
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    {!result && !error && <p style={{ textAlign: 'center', padding: '2rem 0', color: 'var(--text-muted)' }}>Cargando notificaciones…</p>}
    {result && <>
      {result.items.length === 0 && <p style={{ textAlign: 'center', padding: '2rem 0', color: 'var(--text-muted)' }}>No tienes nuevas notificaciones.</p>}
      <ul className="notification-list" style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {result.items.map(notification => (
          <li className={'notification-item ' + (!notification.readAt ? 'unread' : '')} key={notification.id} style={{ padding: '12px', background: !notification.readAt ? 'var(--surface-hover)' : 'transparent', borderBottom: '1px solid var(--border)', borderRadius: 8 }}>
             <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, alignItems: 'flex-start' }}>
               <div style={{ flex: 1, cursor: notification.targetAvailable ? 'pointer' : 'default' }} onClick={() => { if(notification.targetAvailable) void mutate('/notifications/' + notification.id + '/open', true) }}>
                 <p style={{ margin: '0 0 4px', fontWeight: !notification.readAt ? 600 : 400, fontSize: '0.95rem' }}>{notification.title}</p>
                 <p style={{ margin: '0 0 8px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>{notification.message}</p>
                 <p style={{ margin: 0, fontSize: '0.75rem', color: 'var(--text-muted)' }}><time dateTime={notification.createdAt}>{new Date(notification.createdAt).toLocaleString('es-EC')}</time></p>
               </div>
               {!notification.readAt && <button className="icon-button" disabled={busy} onClick={() => void mutate('/notifications/' + notification.id + '/read')} title="Marcar como leída" style={{ padding: 4 }}><Icon name="check" size={16} /></button>}
             </div>
             {!notification.targetAvailable && <p style={{ margin: '8px 0 0', fontSize: '0.75rem', color: 'var(--danger)' }}>Objeto no disponible.</p>}
          </li>
        ))}
      </ul>
      {(result.page > 0 || (result.page + 1) * result.size < result.total) && <div style={{ display: 'flex', justifyContent: 'center', marginTop: 16, paddingTop: 16, borderTop: '1px solid var(--border)', gap: 16 }}>
        <button className="text-button" style={{ background: 'transparent', border: 'none', color: 'var(--primary)', cursor: 'pointer', fontWeight: 600 }} disabled={busy || result.page === 0} onClick={() => { setResult(null); setFilter(f => ({ ...f, page: f.page - 1 })) }}>Anteriores</button>
        <button className="text-button" style={{ background: 'transparent', border: 'none', color: 'var(--primary)', cursor: 'pointer', fontWeight: 600 }} disabled={busy || (result.page + 1) * result.size >= result.total} onClick={() => { setResult(null); setFilter(f => ({ ...f, page: f.page + 1 })) }}>Siguientes</button>
      </div>}
    </>}
  </section>
}
