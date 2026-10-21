import { useEffect, useRef, useSyncExternalStore } from 'react'
import { createPortal } from 'react-dom'
import { currentConfirmation, finishConfirmation, subscribeConfirmation } from './confirm-action'
import { containDialogFocus } from './dialog-focus'
import { Icon } from './Icon'
import './confirmation-dialog.css'

export function ConfirmationDialog() {
  const request = useSyncExternalStore(subscribeConfirmation, currentConfirmation)
  const dialog = useRef<HTMLDialogElement>(null)
  const cancel = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    if (!request || !dialog.current) return
    const element = dialog.current, previous = document.body.style.overflow
    element.showModal(); cancel.current?.focus(); document.body.style.overflow = 'hidden'
    const removeTrap = containDialogFocus(element)
    return () => { removeTrap(); element.close(); document.body.style.overflow = previous }
  }, [request])
  if (!request) return null
  function finish(accepted: boolean) { dialog.current?.close(); finishConfirmation(accepted) }
  return createPortal(<dialog ref={dialog} className="confirmation-dialog" aria-labelledby="confirmation-title" aria-describedby="confirmation-message" onCancel={event => { event.preventDefault(); finish(false) }}>
    <div className="confirmation-symbol"><Icon name="lock" size={24} /></div>
    <h2 id="confirmation-title">Confirmar acción</h2>
    <p id="confirmation-message">{request.message}</p>
    <div className="confirmation-buttons"><button ref={cancel} type="button" className="secondary" onClick={() => finish(false)}>Cancelar</button><button type="button" onClick={() => finish(true)}>Confirmar</button></div>
  </dialog>, document.body)
}
