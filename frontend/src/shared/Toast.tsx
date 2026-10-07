import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import './Toast.css'

type Tone = 'error' | 'warning' | 'success'

export function Toast({ message, tone = 'error', onDismiss }: { message: string; tone?: Tone; onDismiss?: () => void }) {
  return <ToastItem key={message} message={message} tone={tone} onDismiss={onDismiss} />
}

const icons = {
  success: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="m5 12 4 4L19 6"/></svg>,
  error: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>,
  warning: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg>
}
const CloseIcon = <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>

const titles = {
  success: 'Completado',
  error: 'Se encontró un problema',
  warning: 'Atención'
}

function ToastItem({ message, tone, onDismiss }: { message: string; tone: Tone; onDismiss?: () => void }) {
  const [dismissed, setDismissed] = useState(false)
  const [paused, setPaused] = useState(false)
  useEffect(() => {
    if (tone !== 'success' || paused || dismissed) return
    const timer = window.setTimeout(() => setDismissed(true), 8000)
    return () => window.clearTimeout(timer)
  }, [tone, paused, dismissed])
  if (dismissed) return null
  const host = document.getElementById('toast-root')
  if (!host) return null
  return createPortal(<div className={'toast toast-' + tone}
    onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}
    onFocusCapture={() => setPaused(true)} onBlurCapture={event => { if (!event.currentTarget.contains(event.relatedTarget)) setPaused(false) }}>
    <div className="toast-icon-container">
      <span className="toast-symbol" aria-hidden="true">{icons[tone]}</span>
    </div>
    <div className="toast-content">
      <span className="toast-title">{titles[tone]}</span>
      <p role={tone === 'success' ? 'status' : 'alert'} aria-atomic="true">{message}</p>
    </div>
    <button type="button" className="toast-close" aria-label="Cerrar aviso" title="Cerrar aviso" onClick={() => { setDismissed(true); onDismiss?.() }}>{CloseIcon}</button>
  </div>, host)
}
