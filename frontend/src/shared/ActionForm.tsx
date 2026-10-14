import { useEffect, useId, useRef, useState, type ReactNode } from 'react'
import { Icon } from './Icon'
import './action-form.css'

export function ActionForm({ label, children, dirty, disabled = false, onCancel, openSignal }: {
  label: string; children: ReactNode; dirty?: boolean; disabled?: boolean; onCancel?: () => void; openSignal?: string
}) {
  const [open, setOpen] = useState(!!openSignal), [touched, setTouched] = useState(false)
  const [previousSignal, setPreviousSignal] = useState(openSignal)
  const button = useRef<HTMLButtonElement>(null), body = useRef<HTMLDivElement>(null), id = useId()
  if (previousSignal !== openSignal) { setPreviousSignal(openSignal); if (openSignal) setOpen(true); setTouched(false) }
  useEffect(() => {
    if (open) body.current?.querySelector<HTMLElement>('input:not(:disabled),select:not(:disabled),textarea:not(:disabled),button:not(:disabled)')?.focus()
  }, [open])
  function cancel() {
    if (disabled || ((dirty ?? touched) && !window.confirm('Hay cambios sin guardar. ¿Cancelar y descartarlos?'))) return
    onCancel?.(); setOpen(false); setTouched(false)
    requestAnimationFrame(() => button.current?.focus())
  }
  return <div className="action-form" onKeyDown={e => { if (open && e.key === 'Escape') { e.preventDefault(); e.stopPropagation(); cancel() } }}>
    {!open ? <button ref={button} type="button" disabled={disabled} aria-expanded={false} aria-controls={id} onClick={() => setOpen(true)}><Icon name="plus" size={17} />{label}</button> : <>
      <div className="action-form-heading"><strong>{label}</strong><button type="button" className="secondary" disabled={disabled} aria-label={'Cancelar: ' + label} onClick={cancel}>Cancelar</button></div>
      <div ref={body} id={id} role="region" aria-label={'Formulario: ' + label} className="action-form-body" onChangeCapture={() => setTouched(true)} onResetCapture={() => setTouched(false)}>{children}</div>
    </>}
  </div>
}
