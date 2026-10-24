import { Toast } from '../shared/Toast'
import { useEffect, useState, useRef, useId } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
import './notifications.css'

interface Notification {
  id: string
  kind: string
  targetType: 'GROUP' | 'WORK_PLAN' | 'ACCOUNT'
  targetId: string
  title: string
  message: string
  createdAt: string
  readAt: string | null
  targetAvailable: boolean
}

interface NotificationPage {
  items: Notification[]
  total: number
  unread: number
  page: number
  size: number
}

export interface NotificationTarget {
  type: 'GROUP' | 'WORK_PLAN' | 'ACCOUNT'
  id: string
}

function changed() {
  window.dispatchEvent(new Event('notifications-changed'))
}

function parseNotificationMessage(raw: string) {
  const parts = raw.split(' Objeto: ')
  return {
    body: parts[0] || raw,
    targetObject: parts[1] || null
  }
}

function formatNotificationTime(isoString: string): string {
  try {
    const date = new Date(isoString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMinutes = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMinutes / 60)
    const diffDays = Math.floor(diffHours / 24)

    if (diffMinutes < 1) return 'Ahora mismo'
    if (diffMinutes < 60) return `Hace ${diffMinutes} min`
    if (diffHours < 24) return `Hace ${diffHours} h`
    if (diffDays === 1) return 'Ayer'
    if (diffDays < 7) return `Hace ${diffDays} d`

    return date.toLocaleDateString('es-EC', { day: '2-digit', month: 'short' })
  } catch {
    return isoString
  }
}

export function NotificationIndicator({ onOpenTarget }: { onOpenTarget: (target: NotificationTarget) => Promise<void> }) {
  const [count, setCount] = useState<number | null>(null)
  const [isOpen, setIsOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null), trigger = useRef<HTMLButtonElement>(null), panel = useRef<HTMLDivElement>(null)
  const panelId = useId()
  useEffect(() => {
    if (!isOpen) return
    panel.current?.querySelector<HTMLElement>('button:not(:disabled)')?.focus()
    const outside = (event: PointerEvent) => { if (!root.current?.contains(event.target as Node)) setIsOpen(false) }
    document.addEventListener('pointerdown', outside)
    return () => { document.removeEventListener('pointerdown', outside) }
  }, [isOpen])
  function close() { setIsOpen(false); requestAnimationFrame(() => trigger.current?.focus()) }


  useEffect(() => {
    let active = true
    const refresh = () => {
      if (document.visibilityState === 'visible') {
        api<{ unread: number }>('/notifications/unread-count')
          .then(data => { if (active) setCount(data.unread) })
          .catch(() => { if (active) setCount(null) })
      }
    }
    refresh()
    const timer = window.setInterval(refresh, 60000)
    window.addEventListener('notifications-changed', refresh)
    window.addEventListener('focus', refresh)
    document.addEventListener('visibilitychange', refresh)
    return () => {
      active = false
      window.clearInterval(timer)
      window.removeEventListener('notifications-changed', refresh)
      window.removeEventListener('focus', refresh)
      document.removeEventListener('visibilitychange', refresh)
    }
  }, [])

  return (
    <div ref={root} style={{ position: 'relative' }}>
      <button
        ref={trigger}
        aria-expanded={isOpen}
        aria-controls={isOpen ? panelId : undefined}
        aria-haspopup="dialog"
        className="icon-button"
        style={{ position: 'relative', width: 42, height: 42, padding: 0 }}
        title="Notificaciones"
        aria-label={count === null ? 'Notificaciones' : 'Notificaciones: ' + count + ' sin leer'}
        onClick={() => setIsOpen(!isOpen)}
      >
        <Icon name="bell" size={22} />
        {count !== null && count > 0 && (
          <span style={{
            position: 'absolute',
            top: 2,
            right: 2,
            background: 'var(--danger)',
            color: 'white',
            fontSize: '.62rem',
            fontWeight: 800,
            padding: '2px 5px',
            borderRadius: 10,
            lineHeight: 1,
            border: '2px solid var(--surface)'
          }}>
            {count > 99 ? '99+' : count}
          </span>
        )}
      </button>

      {isOpen && (
        <div ref={panel} id={panelId} role="dialog" aria-label="Notificaciones" className="notification-popover" onBlurCapture={event => { if (event.relatedTarget && !root.current?.contains(event.relatedTarget as Node)) setIsOpen(false) }} onKeyDown={event => {
          if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); close() }
        }}>
          <Notifications
            onOpen={async (t) => {
              await onOpenTarget(t)
              close()
            }}
            onClose={close}
          />
        </div>
      )}
    </div>
  )
}

export function Notifications({ onOpen, onClose }: { onOpen: (target: NotificationTarget) => Promise<void>; onClose?: () => void }) {
  const [filter, setFilter] = useState({ read: '', page: 0 })
  const [result, setResult] = useState<NotificationPage | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true
    const params = new URLSearchParams({ page: String(filter.page) })
    if (filter.read) params.set('read', filter.read)
    api<NotificationPage>('/notifications?' + params)
      .then(data => { if (active) { setResult(data); setError('') } })
      .catch(e => { if (active) { setError(e.message); setResult(null) } })
    return () => { active = false }
  }, [filter, reload])

  async function mutate(path: string, open = false) {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const target = await api<NotificationTarget>(path, 'POST')
      changed()
      setFilter(f => ({ ...f, page: 0 }))
      setReload(n => n + 1)
      if (open) await onOpen(target)
      else setNotice('Estado de lectura actualizado.')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo actualizar la notificación.')
      setReload(n => n + 1)
    } finally {
      setBusy(false)
    }
  }

  function getNotificationIcon(kind: string, targetType: Notification['targetType']) {
    if (kind === 'DOCUMENT_APPROVED') {
      return { icon: <Icon name="check" size={13} />, className: 'kind-success' }
    }
    if (kind === 'DOCUMENT_RETURNED' || kind.includes('WARNING') || kind.includes('NEAR')) {
      return { icon: <Icon name="refresh" size={13} />, className: 'kind-warning' }
    }
    if (kind === 'AUTHOR_SIGNATURE_REQUESTED') {
      return { icon: <Icon name="edit" size={13} />, className: 'kind-warning' }
    }
    if (targetType === 'ACCOUNT') {
      return { icon: <Icon name="lock" size={13} />, className: 'kind-account' }
    }
    if (targetType === 'GROUP') {
      return { icon: <Icon name="users" size={13} />, className: 'kind-group' }
    }
    return { icon: <Icon name="document" size={13} />, className: 'kind-work_plan' }
  }

  return (
    <section aria-label="Bandeja de notificaciones" className="notifications-container">
      <header className="notifications-header">
        <div className="notifications-header-title">
          <h2>Notificaciones</h2>
          {result && result.unread > 0 ? (
            <span className="notifications-count-badge">{result.unread} sin leer</span>
          ) : result ? (
            <span className="notifications-count-badge is-clean">Al día</span>
          ) : null}
        </div>
        <div className="notifications-header-actions">
          <button
            type="button"
            className="notification-header-btn"
            disabled={busy || !result?.unread}
            onClick={() => void mutate('/notifications/read-all')}
            title="Marcar todas como leídas"
            aria-label="Marcar todas como leídas"
          >
            <Icon name="check" size={16} />
          </button>
          <button
            type="button"
            className="notification-header-btn"
            disabled={busy}
            onClick={() => { setError(''); setResult(null); setReload(n => n + 1); changed() }}
            title="Actualizar"
            aria-label="Actualizar"
          >
            <Icon name="refresh" size={16} />
          </button>
          {onClose && (
            <>
              <span className="notification-header-divider" aria-hidden="true" />
              <button
                type="button"
                className="notification-header-btn"
                onClick={onClose}
                title="Cerrar"
                aria-label="Cerrar"
              >
                <Icon name="x" size={16} />
              </button>
            </>
          )}
        </div>
      </header>

      <div className="notification-filters" role="group" aria-label="Filtrar notificaciones">
        {[['', 'Todas'], ['false', 'Sin leer'], ['true', 'Leídas']].map(([value, label]) => <button type="button" key={value} aria-pressed={filter.read === value} disabled={busy} onClick={() => { setError(''); setResult(null); setFilter({ read: value, page: 0 }) }}>{label}</button>)}
      </div>
      {error && <Toast message={error} onDismiss={() => setError('')} />}
      {notice && <Toast tone="success" message={notice} />}

      {!result && !error && (
        <div className="notification-empty">
          <p>Cargando notificaciones…</p>
        </div>
      )}

      {result && (
        <>
          {result.items.length === 0 ? (
            <div className="notification-empty">
              <span className="notification-empty-icon"><Icon name="bell" size={24} /></span>
              <h3>Sin notificaciones</h3>
              <p>No hay notificaciones para el filtro seleccionado.</p>
            </div>
          ) : (
            <div className="notifications-list-scroll">
              {result.items.map(notification => {
                const parsed = parseNotificationMessage(notification.message)
                const avatar = getNotificationIcon(notification.kind, notification.targetType)

                return (
                  <div
                    className={'notification-item ' + (!notification.readAt ? 'unread' : '')}
                    key={notification.id}
                  >
                    <div className="notification-header-actions">
                      <time
                        className="notification-time"
                        dateTime={notification.createdAt}
                        title={new Date(notification.createdAt).toLocaleString('es-EC')}
                      >
                        {formatNotificationTime(notification.createdAt)}
                      </time>

                      {!notification.readAt && (
                        <button
                          type="button"
                          className="notification-action-btn"
                          disabled={busy}
                          onClick={(e) => {
                            e.stopPropagation()
                            void mutate('/notifications/' + notification.id + '/read')
                          }}
                          title="Marcar como leída"
                          aria-label="Marcar como leída"
                        >
                          <Icon name="check" size={12} />
                        </button>
                      )}
                    </div>

                    <button
                      type="button"
                      disabled={busy || !notification.targetAvailable}
                      aria-label={'Abrir ' + notification.title}
                      className="notification-content"
                      onClick={() => {
                        if (notification.targetAvailable) {
                          void mutate('/notifications/' + notification.id + '/open', true)
                        }
                      }}
                    >
                      <span className={'notification-avatar ' + avatar.className}>
                        {avatar.icon}
                        {!notification.readAt && <span className="unread-dot" title="No leída" />}
                      </span>

                      <div className="notification-main">
                        <div className="notification-title-row">
                          <p className="notification-title">{notification.title}</p>
                        </div>

                        <p className="notification-message">{parsed.body}</p>

                        {parsed.targetObject && (
                          <div className="notification-target-badge" title={parsed.targetObject}>
                            <span>{parsed.targetObject}</span>
                          </div>
                        )}

                        {!notification.targetAvailable && (
                          <p className="notification-unavailable-msg">
                            Objeto no disponible.
                          </p>
                        )}
                      </div>
                    </button>
                  </div>
                )
              })}
            </div>
          )}

          {(result.page > 0 || (result.page + 1) * result.size < result.total) && (
            <footer className="notifications-pagination">
              <button
                type="button"
                disabled={busy || result.page === 0}
                onClick={() => { setResult(null); setFilter(f => ({ ...f, page: f.page - 1 })) }}
              >
                ← Anteriores
              </button>
              <button
                type="button"
                disabled={busy || (result.page + 1) * result.size >= result.total}
                onClick={() => { setResult(null); setFilter(f => ({ ...f, page: f.page + 1 })) }}
              >
                Siguientes →
              </button>
            </footer>
          )}
        </>
      )}
    </section>
  )
}
