import { confirmAction } from './confirm-action'
import { useEffect, useId, useRef, useState, type ReactNode } from 'react'
import { Icon } from './Icon'
import { containDialogFocus } from './dialog-focus'
import './action-form.css'

export function ActionForm({ label, children, dirty, disabled = false, onCancel, openSignal, modal = false }: {
  label: string; children: ReactNode; dirty?: boolean; disabled?: boolean; onCancel?: () => void; openSignal?: string; modal?: boolean
}) {
  const [open, setOpen] = useState(!!openSignal), [touched, setTouched] = useState(false)
  const [previousSignal, setPreviousSignal] = useState(openSignal)
  const button = useRef<HTMLButtonElement>(null), body = useRef<HTMLDivElement>(null), id = useId()
  const dialog = useRef<HTMLDialogElement>(null)
  if (previousSignal !== openSignal) { setPreviousSignal(openSignal); if (openSignal) setOpen(true); setTouched(false) }
  useEffect(() => {
    if (modal && open && dialog.current && !dialog.current.open) dialog.current.showModal()
    if (modal && !open && dialog.current?.open) dialog.current.close()
    if (open) body.current?.querySelector<HTMLElement>('input:not(:disabled),select:not(:disabled),textarea:not(:disabled),button:not(:disabled)')?.focus()
    if (!modal || !open) return
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const removeTrap = dialog.current ? containDialogFocus(dialog.current) : () => {}
    return () => { removeTrap(); document.body.style.overflow = previous }
  }, [open, modal])
  async function cancel() {
    if (disabled || ((dirty ?? touched) && !(await confirmAction('Hay cambios sin guardar. ¿Cancelar y descartarlos?')))) return
    onCancel?.(); setOpen(false); setTouched(false)
    requestAnimationFrame(() => button.current?.focus())
  }
  const content = <>
      <div className="action-form-heading"><strong id={id + '-heading'}>{label}</strong><button type="button" className="secondary" disabled={disabled} aria-label={'Cancelar: ' + label} onClick={cancel}>Cancelar</button></div>
      <div ref={body} id={id} role="region" aria-label={'Formulario: ' + label} className="action-form-body" onChangeCapture={() => setTouched(true)} onResetCapture={() => setTouched(false)}>{children}</div>
    </>
  return <div className="action-form" onKeyDown={e => { if (!modal && open && e.key === 'Escape') { e.preventDefault(); e.stopPropagation(); cancel() } }}>
    {(!open || modal) && <button ref={button} type="button" disabled={disabled} aria-expanded={open} aria-haspopup={modal ? 'dialog' : undefined} onClick={() => setOpen(true)}><Icon name="plus" size={17} />{label}</button>}
    {modal ? <dialog ref={dialog} className="action-form-dialog" aria-labelledby={id + '-heading'} onCancel={e => { e.preventDefault(); cancel() }}>{open && content}</dialog> : open && content}
  </div>
}
