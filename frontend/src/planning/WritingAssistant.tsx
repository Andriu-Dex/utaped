import { useEffect, useId, useRef, useState } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import './writing-assistant.css'

type Configuration = { available: boolean; provider: string; maxInputCharacters: number }
type Proposal = { documentId: string; original: string; text: string; rowVersion: number }
export function WritingAssistant({ documentId, rowVersion, field, label, value, disabled, onApply }: {
  documentId: string; rowVersion: number; field: string; label: string; value: string; disabled: boolean; onApply: (text: string) => void
}) {
  const [configuration, setConfiguration] = useState<Configuration | null>(null)
  const [open, setOpen] = useState(false); const [consent, setConsent] = useState(false); const [tone, setTone] = useState('CLEAR')
  const [proposal, setProposal] = useState<Proposal | null>(null); const [busy, setBusy] = useState(false); const [error, setError] = useState('')
  const sequence = useRef(0); const descriptionId = useId(); const panelId = useId()
  useEffect(() => { let active = true; const requests = sequence; api<Configuration>('/writing/configuration').then(c => { if (active) setConfiguration(c) }).catch(() => { if (active) setError('No se pudo consultar la asistencia de redacción.') }); return () => { active = false; requests.current++ } }, [])
  const changed = !!proposal && (proposal.documentId !== documentId || proposal.original !== value || proposal.rowVersion !== rowVersion)
  async function suggest() {
    if (!configuration?.available || disabled || !consent || !value.trim() || value.length > configuration.maxInputCharacters) return
    const original = value, version = rowVersion, request = ++sequence.current
    setBusy(true); setError(''); setProposal(null)
    try {
      const result = await api<{ text: string; rowVersion: number }>('/work-plans/' + documentId + '/writing', 'POST', { rowVersion: version, field, tone, text: original, consent })
      if (sequence.current === request) setProposal({ documentId, original, text: result.text, rowVersion: result.rowVersion })
    } catch (e) { if (sequence.current === request) setError(e instanceof Error ? e.message : 'No se pudo obtener la sugerencia. Su texto se conserva.') }
    finally { if (sequence.current === request) setBusy(false) }
  }
  function discard() { sequence.current++; setBusy(false); setProposal(null); setError('') }
  return <section className={'writing-assistant' + (open ? ' writing-expanded' : '')} aria-label={'Asistencia para ' + label}>
    <div className="writing-heading"><span className="writing-symbol"><Icon name="sparkles" /></span><div><strong>Una segunda mirada a tu texto</strong><small>Mejora la claridad sin cambiar el contenido institucional.</small></div><button type="button" className="secondary" aria-expanded={open} aria-controls={panelId} disabled={disabled} onClick={() => { if (open) discard(); setOpen(!open) }}>{open ? 'Cerrar asistencia' : 'Mejorar redacción'}</button></div>
    {open && <div id={panelId} className="writing-body">{error && <Toast message={error} onDismiss={() => setError('')} />}
      {!configuration ? <p role="status">Consultando disponibilidad…</p> : !configuration.available ? <p className="field-help">La asistencia aún no está habilitada. Puedes continuar escribiendo y guardando manualmente.</p> : <>
        <div className="writing-options"><label>Estilo de redacción<select value={tone} disabled={busy || disabled} onChange={e => setTone(e.target.value)}><option value="CLEAR">Más claro</option><option value="FORMAL">Más formal</option><option value="CONCISE">Más conciso</option></select></label><span className="writing-counter">{value.length.toLocaleString('es-EC')} / {configuration.maxInputCharacters.toLocaleString('es-EC')} caracteres para asistencia</span></div>
        <p id={descriptionId} className="field-help">Se enviará únicamente el texto de «{label}» a {configuration.provider === 'OPENAI' ? 'OpenAI' : 'Groq'}. Revisa que no contenga información que no deseas compartir. La propuesta no se guarda automáticamente.</p>
        <label className="writing-consent"><input type="checkbox" checked={consent} disabled={busy || disabled} onChange={e => setConsent(e.target.checked)} aria-describedby={descriptionId} />Autorizo el envío de este texto para obtener una sugerencia.</label>
        {!value.trim() && <p className="field-help">Escribe primero tu contenido. La asistencia mejora texto existente.</p>}
        {value.length > configuration.maxInputCharacters && <p className="field-help">Este texto supera el límite de asistencia. Puedes seguir editándolo y guardarlo manualmente.</p>}
        <div className="writing-actions"><button type="button" disabled={busy || disabled || !consent || !value.trim() || value.length > configuration.maxInputCharacters} onClick={() => void suggest()}><Icon name="sparkles" size={16} />{busy ? 'Preparando sugerencia…' : proposal ? 'Obtener otra sugerencia' : 'Obtener sugerencia'}</button>{busy && <span role="status">Tu texto original se conserva.</span>}</div>
        {proposal && <><div className="writing-comparison"><section><div className="writing-column-heading"><span>01</span><h3>Texto enviado</h3><small>{proposal.original.length} caracteres</small></div><p>{proposal.original}</p></section><section className="writing-proposal"><div className="writing-column-heading"><span>02</span><h3>Propuesta de redacción</h3><small>{proposal.text.length} caracteres</small></div><p>{proposal.text}</p></section></div>
          {changed && <p role="status" className="field-help">Tu texto o la versión del documento cambió. Solicita otra sugerencia para conservar tus últimas modificaciones.</p>}
          <div className="writing-actions"><button type="button" disabled={disabled || changed || busy} onClick={() => { if (!changed && !disabled) { onApply(proposal.text); discard() } }}><Icon name="check" size={16} />Aplicar al borrador</button><button type="button" className="secondary" onClick={discard}>Descartar propuesta</button><small>Después de aplicar, guarda el documento para conservar el cambio.</small></div></>}
      </>}
    </div>}
  </section>
}
