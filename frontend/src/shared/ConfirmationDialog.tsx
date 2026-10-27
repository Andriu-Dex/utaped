import { useEffect, useRef, useSyncExternalStore } from 'react'
import { createPortal } from 'react-dom'
import { currentConfirmation, finishConfirmation, subscribeConfirmation } from './confirm-action'
import { containDialogFocus } from './dialog-focus'
import { Icon } from './Icon'
import './confirmation-dialog.css'

interface MetadataItem {
  label: string
  value: string
  isMono?: boolean
}

function parseConfirmationMessage(raw: string) {
  const lines = raw.trim().split('\n').map(l => l.trim()).filter(Boolean)
  const meta: MetadataItem[] = []
  const textLines: string[] = []
  let prompt: string | null = null

  for (const line of lines) {
    if ((line.startsWith('¿') || line.startsWith('?') || line.endsWith('?')) && !line.includes(':')) {
      prompt = line
      continue
    }
    const match = line.match(/^([^:\n]{3,35}):\s*(.+)$/)
    if (match) {
      const label = match[1].trim()
      const value = match[2].trim()
      const isMono = label.toLowerCase() === 'emisor' || value.includes('CN=') || value.includes('OU=')
      meta.push({ label, value, isMono })
    } else {
      textLines.push(line)
    }
  }

  return { meta, textLines, prompt }
}

export function ConfirmationDialog() {
  const request = useSyncExternalStore(subscribeConfirmation, currentConfirmation)
  const dialog = useRef<HTMLDialogElement>(null)
  const cancel = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    if (!request || !dialog.current) return
    const element = dialog.current, previous = document.body.style.overflow
    // Reattaching an open dialog removes it from the modal layer but keeps
    // its `open` attribute. Keep the pending decision visible and reachable.
    const ensureModal = () => {
      if (!element.isConnected || element.matches(':modal')) return
      if (element.open) element.close()
      element.showModal()
      cancel.current?.focus()
    }
    ensureModal(); document.body.style.overflow = 'hidden'
    const observer = new MutationObserver(ensureModal)
    observer.observe(document.body, { childList: true, subtree: true })
    const removeTrap = containDialogFocus(element)
    return () => { observer.disconnect(); removeTrap(); element.close(); document.body.style.overflow = previous }
  }, [request])
  if (!request) return null
  function finish(accepted: boolean) { dialog.current?.close(); finishConfirmation(accepted) }

  const { meta, textLines, prompt } = parseConfirmationMessage(request.message)
  const hasMeta = meta.length > 0
  const isCert = hasMeta && meta.some(m => m.label.toLowerCase().includes('certificado') || m.label.toLowerCase() === 'emisor')

  return createPortal(<dialog ref={dialog} className="confirmation-dialog" aria-labelledby="confirmation-title" aria-describedby="confirmation-message" onCancel={event => { event.preventDefault(); finish(false) }}>
    <div className="confirmation-header">
      <div className="confirmation-symbol"><Icon name="lock" size={22} /></div>
      <div className="confirmation-header-text">
        <span className="confirmation-kicker">{isCert ? 'Firma digital' : 'Confirmación requerida'}</span>
        <h2 id="confirmation-title">Confirmar acción</h2>
      </div>
    </div>
    <div id="confirmation-message" className="confirmation-content">
      {hasMeta ? <>
        <div className="confirmation-metadata-card">
          <div className="confirmation-metadata-kicker">
            <Icon name="document" size={13} />
            <span>{isCert ? 'Datos del certificado digital' : 'Detalles de la operación'}</span>
          </div>
          <div className="confirmation-details">
            {meta.map((item, idx) => (
              <div key={idx} className="confirmation-detail-row">
                <span className="confirmation-detail-label">{item.label}: </span>
                <span className={'confirmation-detail-value' + (item.isMono ? ' is-mono' : '')}>{item.value}</span>
              </div>
            ))}
          </div>
        </div>
        {textLines.length > 0 && <div className="confirmation-extra-text">{textLines.map((line, i) => <p key={i}>{line}</p>)}</div>}
        {prompt && <p className="confirmation-prompt">{prompt}</p>}
      </> : <p className="confirmation-body-text">{request.message}</p>}
    </div>
    <div className="confirmation-buttons">
      <button ref={cancel} type="button" className="secondary" onClick={() => finish(false)}>Cancelar</button>
      <button type="button" onClick={() => finish(true)}>Confirmar</button>
    </div>
  </dialog>, document.body)
}
