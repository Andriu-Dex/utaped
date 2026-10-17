import { containDialogFocus } from '../shared/dialog-focus'
import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { api } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import './writing-assistant.css'

type Configuration = { available: boolean; provider: string; maxInputCharacters: number }
type Proposal = { documentId: string; original: string; text: string; rowVersion: number }

export function WritingAssistant({
  documentId,
  rowVersion,
  field,
  label,
  value,
  disabled,
  onApply,
}: {
  documentId: string
  rowVersion: number
  field: string
  label: string
  value: string
  disabled: boolean
  onApply: (text: string) => void
}) {
  const [configuration, setConfiguration] = useState<Configuration | null>(null)
  const [open, setOpen] = useState(false)
  const [consent, setConsent] = useState(false)
  const [tone, setTone] = useState('CLEAR')
  const [proposal, setProposal] = useState<Proposal | null>(null)
  const [editableText, setEditableText] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const sequence = useRef(0)
  const descriptionId = useId()
  const panelId = useId()
  const modalRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    let active = true
    const requests = sequence
    api<Configuration>('/writing/configuration')
      .then(c => {
        if (active) setConfiguration(c)
      })
      .catch(() => {
        if (active) setError('No se pudo consultar la asistencia de redacción.')
      })
    return () => {
      active = false
      requests.current++
    }
  }, [])

  const changed =
    !!proposal &&
    (proposal.documentId !== documentId || proposal.original !== value || proposal.rowVersion !== rowVersion)

  async function suggest() {
    if (
      !configuration?.available ||
      disabled ||
      !consent ||
      !value.trim() ||
      value.length > configuration.maxInputCharacters
    )
      return
    if (proposal && editableText !== proposal.text && !(await confirmAction('Hay modificaciones en la propuesta sugerida. ¿Descartarlas y solicitar otra sugerencia?'))) return
    const original = value,
      version = rowVersion,
      request = ++sequence.current
    setBusy(true)
    setError('')
    setProposal(null)
    setEditableText('')
    try {
      const result = await api<{ text: string; rowVersion: number }>(
        '/work-plans/' + documentId + '/writing',
        'POST',
        { rowVersion: version, field, tone, text: original, consent },
      )
      if (sequence.current === request) {
        setProposal({ documentId, original, text: result.text, rowVersion: result.rowVersion })
        setEditableText(result.text)
      }
    } catch (e) {
      if (sequence.current === request)
        setError(e instanceof Error ? e.message : 'No se pudo obtener la sugerencia. Su texto se conserva.')
    } finally {
      if (sequence.current === request) setBusy(false)
    }
  }

  function discard() {
    sequence.current++
    setBusy(false)
    setProposal(null)
    setEditableText('')
    setError('')
  }

  const handleClose = useCallback(async () => {
    if (proposal && editableText !== proposal.text) {
      const confirmed = await confirmAction('Hay modificaciones en la propuesta sugerida. ¿Descartarlas y cerrar?')
      if (!confirmed) return
    }
    discard()
    setOpen(false)
  }, [proposal, editableText])

  useEffect(() => {
    if (!open || !modalRef.current) return
    const dialog = modalRef.current
    dialog.showModal()
    const release = containDialogFocus(dialog)
    return () => { release(); dialog.close() }
  }, [open])

  useEffect(() => {
    if (!open) return
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prevOverflow
    }
  }, [open])

  useEffect(() => {
    if (!open || !proposal || editableText === proposal.text) return
    const prevent = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = '' }
    window.addEventListener('beforeunload', prevent)
    return () => window.removeEventListener('beforeunload', prevent)
  }, [open, proposal, editableText])

  const words = (text: string) => (text.trim() ? text.trim().split(/\s+/).length : 0)

  return (
    <section className="writing-assistant" aria-label={'Asistencia para ' + label}>
      <button
        type="button"
        className="writing-trigger-btn"
        aria-expanded={open}
        aria-controls={panelId}
        disabled={disabled}
        onClick={() => {
          if (open) void handleClose()
          else setOpen(true)
        }}
        title={`Mejorar redacción de ${label} con IA`}
      >
        <Icon name="sparkles" size={13} />
        <span>{open ? 'Cerrar asistencia' : 'Mejorar redacción'}</span>
      </button>

      {open && (
        <dialog ref={modalRef} className="writing-modal-backdrop" aria-label={'Estudio de redacción asistida para ' + label}
          onCancel={e => { e.preventDefault(); if ([...document.querySelectorAll('dialog[open]')].at(-1) === e.currentTarget) void handleClose() }}
          onClick={e => { if (e.target === e.currentTarget) void handleClose() }}>
          <div
            id={panelId}
            aria-label={'Estudio de redacción asistida para ' + label}
            className="writing-modal-card writing-body"
          >
            <header className="writing-modal-header">
              <div className="writing-modal-header-left">
                <span className="writing-modal-badge-icon">
                  <Icon name="sparkles" size={18} />
                </span>
                <div>
                  <span className="section-kicker">ASISTENCIA INTELIGENTE DE REDACCIÓN</span>
                  <h2>{label}: Estudio y propuesta institucional</h2>
                </div>
              </div>
              <button
                type="button"
                className="secondary writing-modal-close-btn"
                aria-label="Cerrar"
                title="Cerrar ventana de asistencia"
                onClick={() => void handleClose()}
              >
                <Icon name="x" size={16} />
                <span>Cerrar</span>
              </button>
            </header>

            <div className="writing-modal-content-area">
              {error && <Toast message={error} onDismiss={() => setError('')} />}

              {!configuration ? (
                <div className="writing-status-message">
                  <p role="status">Consultando disponibilidad del servicio…</p>
                </div>
              ) : !configuration.available ? (
                <div className="writing-unavailable-card">
                  <Icon name="sparkles" size={28} />
                  <p className="field-help">
                    La asistencia aún no está habilitada. Puedes continuar escribiendo y guardando manualmente.
                  </p>
                </div>
              ) : (
                <>
                  {!proposal ? (
                    <div className="writing-setup-grid">
                      <section className="writing-col-preview">
                        <div className="writing-column-heading">
                          <span>01</span>
                          <h3>Texto original del documento</h3>
                          <small>
                            {words(value)} palabras · {value.length.toLocaleString('es-EC')} caracteres
                          </small>
                        </div>
                        <div className="writing-text-display">
                          {value.trim() ? (
                            <p className="writing-source-text">{value}</p>
                          ) : (
                            <div className="writing-empty-notice">
                              <Icon name="edit" size={28} />
                              <p>
                                Escribe primero tu contenido en el documento. La asistencia requiere texto existente para proponer mejoras.
                              </p>
                            </div>
                          )}
                        </div>
                      </section>

                      <section className="writing-col-setup">
                        <div className="writing-column-heading">
                          <span>02</span>
                          <h3>Configuración de la sugerencia</h3>
                          <span className="writing-status-pill">
                            {configuration.provider === 'OPENAI' ? 'OpenAI GPT' : 'Groq IA'}
                          </span>
                        </div>

                        <div className="writing-setup-body">
                          <label className="writing-tone-field">
                            <span className="writing-field-title">Estilo de redacción:</span>
                            <select value={tone} disabled={busy || disabled} onChange={e => setTone(e.target.value)}>
                              <option value="CLEAR">Más claro y pedagógico</option>
                              <option value="FORMAL">Más formal e institucional</option>
                              <option value="CONCISE">Más conciso y ejecutivo</option>
                            </select>
                            <span className="writing-tone-hint">
                              {tone === 'CLEAR' && 'Optimiza la fluidez y comprensión pedagógica manteniendo los términos técnicos.'}
                              {tone === 'FORMAL' && 'Eleva la rigurosidad y solemnidad formal conforme a los estándares institucionales UTA.'}
                              {tone === 'CONCISE' && 'Sintetiza las ideas esenciales eliminando redundancias y conectores innecesarios.'}
                            </span>
                          </label>

                          <div className="writing-privacy-card">
                            <div className="writing-privacy-header">
                              <Icon name="lock" size={14} />
                              <strong>Privacidad institucional</strong>
                            </div>
                            <p id={descriptionId} className="writing-disclaimer">
                              Se enviará únicamente el texto de «{label}» a {configuration.provider === 'OPENAI' ? 'OpenAI' : 'Groq'}. Revisa que no contenga información personal o confidencial que no desees compartir. La propuesta no se guarda automáticamente hasta que decidas aplicarla.
                            </p>
                          </div>

                          <label className={'writing-consent' + (consent ? ' is-active' : '')}>
                            <input
                              type="checkbox"
                              checked={consent}
                              disabled={busy || disabled}
                              onChange={e => setConsent(e.target.checked)}
                              aria-describedby={descriptionId}
                            />
                            <span className="writing-consent-label">
                              Autorizo el envío de este texto para obtener una sugerencia.
                            </span>
                          </label>

                          {!value.trim() && (
                            <p className="field-help warning-help">
                              Escribe primero tu contenido en el documento. La asistencia requiere texto existente para proponer mejoras.
                            </p>
                          )}
                          {value.length > configuration.maxInputCharacters && (
                            <p className="field-help warning-help">
                              Este texto supera el límite de asistencia ({configuration.maxInputCharacters.toLocaleString('es-EC')} caracteres). Puedes seguir editándolo y guardarlo manualmente.
                            </p>
                          )}

                          <div className="writing-actions">
                            <button
                              type="button"
                              className="writing-suggest-btn"
                              disabled={
                                busy ||
                                disabled ||
                                !consent ||
                                !value.trim() ||
                                value.length > configuration.maxInputCharacters
                              }
                              onClick={() => void suggest()}
                            >
                              <Icon name="sparkles" size={16} />
                              <span>{busy ? 'Preparando sugerencia…' : 'Obtener sugerencia'}</span>
                            </button>
                            {busy ? (
                              <span role="status" className="writing-busy-text">Procesando propuesta… Tu texto original se conserva intacto.</span>
                            ) : (
                              <span className="writing-ready-hint">Tu borrador original no se alterará hasta que apliques la propuesta.</span>
                            )}
                          </div>
                        </div>
                      </section>
                    </div>
                  ) : (
                    <div className="writing-results-zone animate-reveal">
                      <div className="writing-results-topbar">
                        <div className="writing-results-tone">
                          <span>Estilo aplicado:</span>
                          <select value={tone} disabled={busy || disabled} onChange={e => setTone(e.target.value)}>
                            <option value="CLEAR">Más claro y pedagógico</option>
                            <option value="FORMAL">Más formal e institucional</option>
                            <option value="CONCISE">Más conciso y ejecutivo</option>
                          </select>
                        </div>
                        <button
                          type="button"
                          className="secondary writing-refresh-btn"
                          disabled={busy || disabled}
                          onClick={() => void suggest()}
                        >
                          <Icon name="refresh" size={13} />
                          <span>{busy ? 'Generando…' : 'Solicitar otra sugerencia'}</span>
                        </button>
                      </div>

                      <div className="writing-comparison">
                        <section className="writing-col-original">
                          <div className="writing-column-heading">
                            <span>01</span>
                            <h3>Texto enviado</h3>
                            <small>{words(proposal.original)} palabras · {proposal.original.length} caracteres</small>
                          </div>
                          <div className="writing-text-display">
                            <p>{proposal.original}</p>
                          </div>
                        </section>

                        <section className="writing-proposal writing-col-suggested">
                          <div className="writing-column-heading">
                            <span>02</span>
                            <h3>Propuesta de redacción</h3>
                            <div className="writing-proposal-badge-row">
                              <span className="writing-editable-tag" title="Puedes hacer ajustes directamente en este texto">
                                <Icon name="edit" size={12} /> Editable antes de aplicar
                              </span>
                              <small>{words(editableText)} palabras · {editableText.length} caracteres</small>
                            </div>
                          </div>
                          <div className="writing-text-display writing-editable-wrapper">
                            <textarea
                              rows={8}
                              maxLength={50000}
                              value={editableText}
                              disabled={disabled || changed || busy}
                              onChange={e => setEditableText(e.target.value)}
                              aria-label="Texto de la propuesta de redacción editable"
                              className="writing-live-textarea"
                            />
                          </div>
                        </section>
                      </div>

                      {changed && (
                        <p role="status" className="field-help writing-changed-alert">
                          Tu texto o la versión del documento cambió en el editor de fondo. Solicita otra sugerencia para conservar tus últimas modificaciones.
                        </p>
                      )}

                      <div className="writing-modal-actions-footer">
                        <button
                          type="button"
                          className="secondary"
                          onClick={() => void handleClose()}
                        >
                          Descartar propuesta
                        </button>
                        <div className="writing-modal-right-actions">
                          <small className="writing-save-reminder">
                            Después de aplicar, guarda el documento para conservar el cambio.
                          </small>
                          <button
                            type="button"
                            className="primary-apply-btn"
                            disabled={disabled || changed || busy || !editableText.trim() || editableText.length > 50000}
                            onClick={() => {
                              const finalText = editableText.trim() || proposal.text
                              if (!changed && !disabled && finalText && finalText.length <= 50000) {
                                onApply(finalText)
                                discard()
                                setOpen(false)
                              }
                            }}
                          >
                            <Icon name="check" size={16} />
                            Aplicar al borrador
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </dialog>
      )}
    </section>
  )
}
