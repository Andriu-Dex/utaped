import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { api, ApiError } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { containDialogFocus } from '../shared/dialog-focus'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import type { CareerCopyOptions, CareerCopyResult, WorkPlan } from './types'
import '../shared/action-form.css'
import './career-copy.css'

export function CareerCopyModal({
  plan,
  onClose,
  onCreated,
  onOpenCreatedPlan,
  onDirtyChange,
  onFinish,
}: {
  plan: WorkPlan
  onClose: () => void
  onCreated: (documentIds: string[]) => void
  onOpenCreatedPlan?: (planId: string) => void
  onDirtyChange?: (dirty: boolean) => void
  onFinish?: () => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  const cancelButton = useRef<HTMLButtonElement>(null)
  const id = useId()
  const submitting = useRef(false)
  const requestKey = useRef(crypto.randomUUID())
  const lastSubmittedScopes = useRef<string[] | null>(null)

  const [options, setOptions] = useState<CareerCopyOptions | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [conflict, setConflict] = useState(false)
  const [selectedScopeIds, setSelectedScopeIds] = useState<string[]>([])
  const [reload, setReload] = useState(0)
  const [createdDocs, setCreatedDocs] = useState<string[] | null>(null)

  const dirty = selectedScopeIds.length > 0 && !createdDocs
  useEffect(() => {
    onDirtyChange?.(dirty)
    return () => onDirtyChange?.(false)
  }, [dirty, onDirtyChange])

  // Focus trap and modal setup
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

  // Fetch career copy options
  useEffect(() => {
    let active = true
    api<CareerCopyOptions>('/work-plans/' + plan.id + '/career-copies')
      .then(res => {
        if (active) {
          setOptions(res)
          setLoading(false)
        }
      })
      .catch(e => {
        if (active) {
          setError(e instanceof Error ? e.message : 'No se pudieron consultar las carreras elegibles.')
          setLoading(false)
        }
      })
    return () => { active = false }
  }, [plan.id, reload])

  async function cancel() {
    if (submitting.current) return
    if (dirty && !(await confirmAction('Hay carreras seleccionadas para la copia. ¿Descartar y cerrar?'))) {
      return
    }
    onClose()
  }

  function toggleScope(scopeId: string) {
    setSelectedScopeIds(prev => {
      const exists = prev.includes(scopeId)
      const next = exists ? prev.filter(s => s !== scopeId) : [...prev, scopeId]
      return next
    })
    setConflict(false)
    setError('')
  }

  function toggleAll() {
    if (!options) return
    setSelectedScopeIds(prev => {
      if (prev.length === Math.min(options.scopes.length, 10)) {
        return []
      }
      return options.scopes.slice(0, 10).map(s => s.id)
    })
    setConflict(false)
    setError('')
  }

  const unavailableSelections = selectedScopeIds.filter(scopeId => !options?.scopes.some(scope => scope.id === scopeId))

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting.current || !options?.canCopy || selectedScopeIds.length === 0 || selectedScopeIds.length > 10 || unavailableSelections.length > 0) return

    // Ensure request key consistency
    const currentSorted = [...selectedScopeIds].sort().join(',')
    if (lastSubmittedScopes.current) {
      const prevSorted = [...lastSubmittedScopes.current].sort().join(',')
      if (prevSorted !== currentSorted) {
        requestKey.current = crypto.randomUUID()
      }
    }
    lastSubmittedScopes.current = [...selectedScopeIds]

    submitting.current = true
    setBusy(true)
    setError('')
    setConflict(false)

    try {
      const result = await api<CareerCopyResult>('/work-plans/' + plan.id + '/career-copies', 'POST', {
        rowVersion: options.rowVersion,
        requestKey: requestKey.current,
        targetScopeIds: selectedScopeIds,
      })
      submitting.current = false
      setBusy(false)
      setCreatedDocs(result.documentIds)
      onCreated(result.documentIds)
    } catch (e) {
      submitting.current = false
      setBusy(false)
      if (e instanceof ApiError && e.status === 409) {
        setConflict(true)
        setError('El documento o alguno de los destinos cambió concurrentemente. Recargue las opciones para comprobar la disponibilidad actual.')
      } else {
        setError(e instanceof Error ? e.message : 'No se pudo completar la copia a las carreras seleccionadas.')
      }
    }
  }

  return (
    <dialog
      ref={dialog}
      className="action-form-dialog career-copy-dialog"
      aria-labelledby={id + '-title'}
      aria-describedby={id + '-description'}
      onCancel={event => {
        event.preventDefault()
        void cancel()
      }}
    >
      <div className="action-form-heading">
        <h2 id={id + '-title'}>Copiar Plan a otra carrera</h2>
        <button
          ref={cancelButton}
          type="button"
          className="secondary"
          disabled={busy}
          onClick={() => void cancel()}
          aria-label="Cancelar: Copiar Plan a otra carrera"
        >
          {createdDocs ? 'Cerrar' : 'Cancelar'}
        </button>
      </div>

      <div className="action-form-body">
        {createdDocs ? (
          <div className="career-copy-success-view">
            <div className="success-banner">
              <Icon name="check" size={28} />
              <div>
                <strong>Copia multicarrera completada</strong>
                <p>Se crearon {createdDocs.length} nuevos borradores independientes con las secciones y la matriz guardada.</p>
              </div>
            </div>
            <p className="field-help">
              Cada nuevo Plan cuenta con su propia carrera y título derivado. Las firmas, aprobaciones y evidencias no se heredan; cada documento se revisará y firmará de forma separada.
            </p>
            <div className="created-plans-list">
              {createdDocs.map((docId, index) => (
                <div key={docId} className="created-plan-row">
                  <span>Borrador {index + 1}</span>
                  {onOpenCreatedPlan && (
                    <button
                      type="button"
                      className="secondary"
                      onClick={() => {
                        onClose()
                        onOpenCreatedPlan(docId)
                      }}
                    >
                      <Icon name="document" size={15} />
                      Abrir documento
                    </button>
                  )}
                </div>
              ))}
            </div>
            <div className="action-form-actions">
              <button type="button" onClick={onFinish ?? onClose}>
                Finalizar y ver Documentos
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="career-copy-source-card">
              <div className="source-plan-header">
                <Icon name="document" size={18} />
                <span>Plan origen: <strong>{plan.title}</strong></span>
              </div>
              <div className="source-plan-metadata">
                <div>
                  <span className="metadata-label">Grupo:</span>
                  <span>{plan.groupName}</span>
                </div>
                <div>
                  <span className="metadata-label">Período:</span>
                  <span>{plan.periodName}</span>
                </div>
                <div>
                  <span className="metadata-label">Carrera actual:</span>
                  <strong>{plan.career || 'Sin carrera'}</strong>
                </div>
              </div>
            </div>

            <p id={id + '-description'} className="field-help">
              Crea nuevos borradores independientes para otras carreras del mismo grupo y período donde usted sea el único responsable. Se copiarán la justificación, objetivo, secciones y la matriz guardada. No se copian firmas ni anexos.
            </p>

            {error && <Toast message={error} onDismiss={() => setError('')} />}

            {loading ? (
              <p role="status" className="career-copy-loading">Consultando carreras elegibles…</p>
            ) : !options ? (
              <div className="empty-state">
                <p>No se pudo consultar la disponibilidad de copias para este Plan.</p>
                <button type="button" className="secondary" onClick={reloadOptions}>
                  <Icon name="refresh" size={15} />
                  Reintentar
                </button>
              </div>
            ) : !options.canCopy || options.scopes.length === 0 ? (
              <div className="empty-state">
                <Icon name="lock" size={24} />
                <p>No hay carreras elegibles disponibles para copiar este Plan.</p>
                <small>
                  Solo se pueden copiar Planes T1 en borrador editable, con ámbito unipersonal, hacia otras carreras del mismo grupo y período donde aún no exista un Plan vigente.
                </small>
              </div>
            ) : (
              <form onSubmit={submit}>
                <fieldset disabled={busy}>
                  <div className="career-copy-targets-header">
                    <span className="field-label">Seleccione las carreras de destino (máximo 10):</span>
                    {options.scopes.length > 1 && (
                      <button
                        type="button"
                        className="link select-all-btn"
                        onClick={toggleAll}
                      >
                        {selectedScopeIds.length === Math.min(options.scopes.length, 10) ? 'Deseleccionar todas' : options.scopes.length > 10 ? 'Seleccionar primeras 10' : 'Seleccionar todas'}
                      </button>
                    )}
                  </div>

                  {unavailableSelections.length > 0 && (
                    <div role="status" className="field-help">
                      <p>{unavailableSelections.length} destino(s) seleccionado(s) ya no están disponibles.</p>
                      <button type="button" className="secondary" onClick={() => setSelectedScopeIds(ids => ids.filter(scopeId => !unavailableSelections.includes(scopeId)))}>Quitar destinos no disponibles</button>
                    </div>
                  )}
                  <div className="career-copy-scopes-list" role="group" aria-label="Carreras de destino">
                    {options.scopes.map(scope => {
                      const checked = selectedScopeIds.includes(scope.id)
                      return (
                        <label
                          key={scope.id}
                          className={'scope-copy-checkbox-card' + (checked ? ' is-selected' : '')}
                        >
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={() => toggleScope(scope.id)}
                            disabled={busy || (!checked && selectedScopeIds.length >= 10)}
                          />
                          <div className="scope-copy-info">
                            <strong>{scope.careerName || scope.groupName}</strong>
                            <small className="muted">{scope.suggestedTitle}</small>
                          </div>
                        </label>
                      )
                    })}
                  </div>

                  <div className="action-form-actions">
                    <button
                      type="submit"
                      disabled={busy || selectedScopeIds.length === 0 || unavailableSelections.length > 0}
                    >
                      <Icon name="copy" size={16} />
                      {busy ? 'Creando copias…' : selectedScopeIds.length > 0 ? `Copiar a ${selectedScopeIds.length} carrera(s)` : 'Seleccione destinos'}
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
          </>
        )}
      </div>
    </dialog>
  )
}
