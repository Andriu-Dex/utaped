import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import './Toast.css'

type Tone = 'error' | 'warning' | 'success'

export function Toast({ message, tone = 'error' }: { message: string; tone?: Tone }) {
  return <ToastItem key={message} message={message} tone={tone} />
}

function ToastItem({ message, tone }: { message: string; tone: Tone }) {
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
    <span className="toast-symbol" aria-hidden="true">{tone === 'success' ? '✓' : '!'}</span>
    <p role={tone === 'success' ? 'status' : 'alert'} aria-atomic="true">{message}</p>
    <button type="button" className="toast-close" aria-label="Cerrar aviso" title="Cerrar aviso" onClick={() => setDismissed(true)}>×</button>
  </div>, host)
}
