import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { api, ApiError } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { containDialogFocus } from '../shared/dialog-focus'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import { displayStageLabel } from './workflowLabels'
import type { Stage } from './ReviewWorkspace'
import '../shared/action-form.css'
import './reassignment.css'

export interface TaskCandidate {
  id: string
  name: string
}

export interface TaskReassignmentHistoryItem {
  id: string
  previous_actor_id: string
  previous_actor_name: string
  actor_id: string
  actor_name: string
  authorized_by: string
  authorized_by_name: string
  reason: string
  created_at: string
}

export interface ReassignmentOptions {
  rowVersion: number
  actorId: string
  actorName: string
  canReassign: boolean
  candidates: TaskCandidate[]
  history: TaskReassignmentHistoryItem[]
}

export function TaskReassignmentModal({
  task,
  documentTitle,
  onClose,
  onComplete,
  onDirtyChange,
}: {
  task: Stage
  documentTitle: string
  onClose: () => void
  onComplete: () => void
  onDirtyChange?: (dirty: boolean) => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  const cancelButton = useRef<HTMLButtonElement>(null)
  const id = useId()
  const submitting = useRef(false)
  const requestKey = useRef(crypto.randomUUID())
  const lastSubmittedContent = useRef<{ actorId: string; reason: string } | null>(null)

  const [options, setOptions] = useState<ReassignmentOptions | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [conflict, setConflict] = useState(false)
  const [actorId, setActorId] = useState('')
  const [reason, setReason] = useState('')
  const [reload, setReload] = useState(0)

  const dirty = !!actorId || !!reason.trim()
  useEffect(() => {
    onDirtyChange?.(dirty)
    return () => onDirtyChange?.(false)
  }, [dirty, onDirtyChange])

  // Focus and modal setup
  useEffect(() => {
    const element = dialog.current
    if (!element) return
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const previousOverflow = document.body.style.overflow
    element.showModal()
    cancelButton.current?.focus()
    document.body.style.overflow = 'hidden'
    const removeTrap = containDialogFocus(element)
    return () => {
      removeTrap()
      element.close()
      document.body.style.overflow = previousOverflow
      if (previousFocus?.isConnected) previousFocus.focus()
    }
  }, [])

  const reloadOptions = () => {
    setLoading(true)
    setError('')
    setConflict(false)
    setReload(r => r + 1)
  }

  // Fetch options
  useEffect(() => {
    let active = true
    api<ReassignmentOptions>('/reviews/tasks/' + task.id + '/reassignment')
      .then(res => {
        if (active) {
          setOptions(res)
          setLoading(false)
        }
      })
      .catch(e => {
        if (active) {
          setError(e instanceof Error ? e.message : 'No se pudieron cargar las opciones de sustitución.')
          setLoading(false)
        }
      })
    return () => { active = false }
  }, [task.id, reload])

  async function cancel() {
    if (submitting.current) return
    if (dirty && !(await confirmAction('Hay cambios sin guardar en la sustitución. ¿Descartarlos y cancelar?'))) {
      return
    }
    onClose()
  }

  function handleActorChange(newActorId: string) {
    setActorId(newActorId)
    setConflict(false)
    setError('')
  }

  function handleReasonChange(newReason: string) {
    setReason(newReason)
    setConflict(false)
    setError('')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting.current || !options?.canReassign || !actorId || !options.candidates.some(candidate => candidate.id === actorId) || !reason.trim()) return

    // Verify if content changed to renew requestKey
    const currentContent = { actorId, reason: reason.trim() }
    if (lastSubmittedContent.current &&
        (lastSubmittedContent.current.actorId !== currentContent.actorId ||
         lastSubmittedContent.current.reason !== currentContent.reason)) {
      requestKey.current = crypto.randomUUID()
    }
    lastSubmittedContent.current = currentContent

    submitting.current = true
    setBusy(true)
    setError('')
    setConflict(false)

    try {
      await api<ReassignmentOptions>('/reviews/tasks/' + task.id + '/reassignment', 'POST', {
        rowVersion: options.rowVersion,
        actorId,
        reason: reason.trim(),
        requestKey: requestKey.current,
      })
      window.dispatchEvent(new Event('notifications-changed'))
      onComplete()
    } catch (e) {
      submitting.current = false
      setBusy(false)
      if (e instanceof ApiError && e.status === 409) {
        setConflict(true)
        setError('La tarea fue modificada concurrentemente o la versión ya no es válida. Recargue las opciones para actualizar la información sin perder lo escrito.')
      } else {
        setError(e instanceof Error ? e.message : 'No se pudo completar la sustitución del participante.')
      }
    }
  }

  const stageLabel = displayStageLabel(task.stage_label, task.stage_action)

  return (
    <dialog
      ref={dialog}
      className="action-form-dialog task-reassignment-dialog"
      aria-labelledby={id + '-title'}
      aria-describedby={id + '-description'}
      onCancel={event => {
        event.preventDefault()
        void cancel()
      }}
    >
      <div className="action-form-heading">
        <h2 id={id + '-title'}>Sustituir participante de la etapa</h2>
        <button
          ref={cancelButton}
          type="button"
          className="secondary"
          disabled={busy}
          onClick={() => void cancel()}
          aria-label="Cancelar: Sustituir participante"
        >
          Cancelar
        </button>
      </div>

      <div className="action-form-body">
        <div className="reassignment-context-card">
          <p className="reassignment-document-title">
            <Icon name="document" size={18} />
            <span>Documento: <strong>{documentTitle}</strong></span>
          </p>
          <div className="reassignment-metadata-grid">
            <div>
              <span className="metadata-label">Etapa:</span>
              <strong>{stageLabel}</strong>
            </div>
            <div>
              <span className="metadata-label">Participante anterior:</span>
              <strong>{options?.actorName || task.actor_name}</strong>
            </div>
          </div>
        </div>

        <p id={id + '-description'} className="field-help">
          El documento conservará su contenido y las firmas anteriores. El nuevo participante y el motivo de la sustitución quedarán registrados en el historial.
        </p>

        {error && <Toast message={error} onDismiss={() => setError('')} />}

        {loading ? (
          <p role="status" className="reassignment-loading">Cargando opciones de sustitución…</p>
        ) : !options ? (
          <div className="empty-state">
            <p>No se pudo consultar la disponibilidad para esta etapa.</p>
            <button type="button" className="secondary" onClick={reloadOptions}>
              <Icon name="refresh" size={15} />
              Reintentar
            </button>
          </div>
        ) : !options.canReassign ? (
          <div className="empty-state">
            <Icon name="lock" size={24} />
            <p>Esta etapa no admite sustitución en este momento.</p>
            <small>Solo un coordinador activo del ámbito puede sustituir a un participante indisponible en una etapa activa o pendiente.</small>
          </div>
        ) : (
          <form onSubmit={submit}>
            <fieldset disabled={busy}>
              {options.candidates.length === 0 ? (
                <div className="reassignment-no-candidates">
                  <p className="muted">No hay otros integrantes activos y elegibles en este ámbito para cubrir esta etapa.</p>
                </div>
              ) : (
                <label htmlFor={id + '-candidate'}>
                  Nuevo participante
                  <select
                    id={id + '-candidate'}
                    required
                    value={actorId}
                    onChange={e => handleActorChange(e.target.value)}
                  >
                    <option value="">Seleccione un nuevo participante</option>
                    {options.candidates.map(candidate => (
                      <option key={candidate.id} value={candidate.id}>
                        {candidate.name}
                      </option>
                    ))}
                  </select>
                </label>
              )}

              <label htmlFor={id + '-reason'}>
                Motivo justificado de la sustitución
                <textarea
                  id={id + '-reason'}
                  required
                  rows={4}
                  maxLength={1000}
                  value={reason}
                  onChange={e => handleReasonChange(e.target.value)}
                  placeholder="Describa la causa de indisponibilidad o motivo de la sustitución institucional…"
                />
                <span className="char-count">{reason.length} / 1000 caracteres</span>
              </label>

              <div className="action-form-actions">
                <button
                  type="submit"
                  disabled={busy || !actorId || !reason.trim() || !options.candidates.some(candidate => candidate.id === actorId)}
                >
                  <Icon name="users" size={16} />
                  {busy ? 'Procesando sustitución…' : 'Sustituir participante'}
                </button>
                {conflict && (
                  <button
                    type="button"
                    className="secondary"
                    onClick={reloadOptions}
                  >
                    <Icon name="refresh" size={15} />
                    Recargar opciones
                  </button>
                )}
              </div>
            </fieldset>
          </form>
        )}
      </div>
    </dialog>
  )
}
