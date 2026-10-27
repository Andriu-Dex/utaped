import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { api } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { containDialogFocus } from '../shared/dialog-focus'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import type { PlanPage } from './types'
import '../shared/action-form.css'

type DocumentSummary = PlanPage['items'][number]

export function DocumentArchiveDialog({ plan, onCancel, onComplete, onDirtyChange }: {
  plan: DocumentSummary; onCancel: () => void; onComplete: () => void; onDirtyChange: (dirty: boolean) => void
}) {
  const dialog = useRef<HTMLDialogElement>(null), cancelButton = useRef<HTMLButtonElement>(null), id = useId()
  const submitting = useRef(false)
  const [reason, setReason] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState('')
  const draft = plan.documentState === 'DRAFT', label = draft ? 'Descartar borrador' : 'Archivar documento'
  useEffect(() => { onDirtyChange(!!reason); return () => onDirtyChange(false) }, [reason, onDirtyChange])
  useEffect(() => {
    const element = dialog.current
    if (!element) return
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const previousOverflow = document.body.style.overflow
    element.showModal(); cancelButton.current?.focus(); document.body.style.overflow = 'hidden'
    const removeTrap = containDialogFocus(element)
    return () => { removeTrap(); element.close(); document.body.style.overflow = previousOverflow; if (previousFocus?.isConnected) previousFocus.focus() }
  }, [])
  async function cancel() {
    if (submitting.current || (reason && !(await confirmAction('Hay un motivo sin guardar. ¿Cerrar y descartar lo escrito?')))) return
    onCancel()
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting.current || !reason.trim()) return
    submitting.current = true; setBusy(true); setError('')
    try {
      await api('/work-plans/' + plan.id + '/archive', 'POST', { rowVersion: plan.rowVersion, reason: reason.trim() })
      onComplete()
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo completar la acción.'); submitting.current = false; setBusy(false) }
  }
  return <dialog ref={dialog} className="action-form-dialog document-archive-dialog" aria-labelledby={id + '-title'} aria-describedby={id + '-description'} onCancel={event => { event.preventDefault(); void cancel() }}>
    <div className="action-form-heading"><h2 id={id + '-title'}>{label}</h2><button ref={cancelButton} type="button" className="secondary" disabled={busy} onClick={() => void cancel()}>Cancelar</button></div>
    <div className="action-form-body">
    <p className="document-archive-title"><Icon name={draft ? 'trash' : 'archive'} size={20} /><strong>{plan.title}</strong></p>
    <p id={id + '-description'}>{draft ? 'El borrador dejará de aparecer en tu bandeja. Se conservará el registro para trazabilidad; no se elimina definitivamente.' : 'Se conservarán el PDF firmado, las firmas y el historial. El documento quedará archivado y en solo lectura.'}</p>
    {error && <Toast message={error} onDismiss={() => setError('')} />}
    <form onSubmit={submit}>
      <label htmlFor={id + '-reason'}>Motivo {draft ? 'del descarte' : 'del archivo'}<textarea id={id + '-reason'} required maxLength={1000} rows={3} value={reason} disabled={busy} onChange={event => setReason(event.target.value)} /></label>
      <div className="action-form-actions"><button type="submit" className={draft ? 'document-discard-confirm' : ''} disabled={busy || !reason.trim()}>{busy ? 'Procesando…' : label}</button></div>
    </form>
    </div>
  </dialog>
}
