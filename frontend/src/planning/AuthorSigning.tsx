import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, download } from '../shared/api'
import { confirmAndSign, type SigningPhase } from '../shared/confirmed-signature'
import { Icon } from '../shared/Icon'

export interface SignedArtifact { id: string; signerName: string; signedAt: string; pageCount: number; signaturePageIndex: number; actorName?: string; certificateSubject?: string; approvedReviewId?: string }
interface SigningState {
  automaticSubmission: boolean; documentState: string; actorSigned: boolean; pendingAuthors: { id: string; name: string; position: string }[]
  signingEnabled: boolean; signingBlockers: { section: string; message: string }[]; signed: SignedArtifact[]
  preparation: { rowVersion: number; pdfHash: string }
}
export function AuthorSigning({ targetDocId, artifactId, onSigned, onDirtyChange, onBusyChange, onPreviewChange, onFinish, disabled = false }: { targetDocId: string; artifactId: string; onSigned?: () => void; onFinish?: () => void; onDirtyChange?: (dirty: boolean) => void; onBusyChange?: (busy: boolean) => void; onPreviewChange?: (signed: SignedArtifact | null, actorSigned: boolean) => void; disabled?: boolean }) {
  const path = '/work-plans/' + targetDocId + '/artifacts/' + artifactId + '/signing'
  const operationContext = useRef({ active: false })
  useEffect(() => { const context = { active: true }; operationContext.current = context; return () => { context.active = false } }, [path])
  const [state, setState] = useState<SigningState | null>(null); const [error, setError] = useState('')
  const [busy, setBusy] = useState(false); const [notice, setNotice] = useState(''); const [refresh, setRefresh] = useState(0)
  const [requestKey] = useState(() => crypto.randomUUID())
  const [dirty, setDirty] = useState(false)
  const [signingPhase, setSigningPhase] = useState<SigningPhase>('checking')
  function markPending(value: boolean) { setDirty(value) }
  useEffect(() => { onDirtyChange?.(dirty || busy) }, [dirty, busy, onDirtyChange])
  useEffect(() => {
    let active = true
    api<SigningState>(path).then(async value => {
      if (value.documentState === 'APPROVED' && value.signed.length) {
        const rounds = await api<{ id: string; status: string }[]>('/work-plans/' + targetDocId + '/reviews')
        const approved = rounds.find(round => round.status === 'APPROVED')
        if (approved) {
          const review = await api<{ review: { artifact_id: string } }>('/reviews/' + approved.id)
          if (review.review.artifact_id === artifactId) value = { ...value, signed: value.signed.map(signature => ({ ...signature, approvedReviewId: approved.id })) }
        }
      }
      if (active) setState(value)
    }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [path, targetDocId, artifactId, refresh])
  useEffect(() => { onBusyChange?.(busy); return () => onBusyChange?.(false) }, [busy, onBusyChange])
  useEffect(() => () => onDirtyChange?.(false), [onDirtyChange])
  async function sign(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!state?.signingEnabled || busy || disabled) return
    const context = operationContext.current
    const form = event.currentTarget; const certificate = form.elements.namedItem('certificate') as HTMLInputElement
    const credential = form.elements.namedItem('certificatePassword') as HTMLInputElement
    const file = certificate.files?.[0]; const password = credential.value; form.reset()
    if (!file) return
    setBusy(true); setError(''); setNotice('')
    try {
      if (!(await confirmAndSign(path + '?' + new URLSearchParams({ rowVersion: String(state.preparation.rowVersion), inputHash: state.preparation.pdfHash, requestKey }), file, password, () => context.active, setSigningPhase))) return
      setNotice('Firma aplicada. Vigencia e integridad comprobadas.')
      onSigned?.()
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo firmar el documento.') }
    finally { form.reset(); setBusy(false); setRefresh(value => value + 1) }
  }
  async function downloadApproved() {
    if (busy || state?.documentState !== 'APPROVED') return
    setBusy(true); setError('')
    try {
      const rounds = await api<{ id: string; status: string }[]>('/work-plans/' + targetDocId + '/reviews')
      const approved = rounds.find(round => round.status === 'APPROVED')
      if (!approved) throw new Error('No hay un PDF aprobado disponible para este documento.')
      await download('/reviews/' + approved.id + '/content', 'documento-aprobado.pdf')
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo descargar el documento aprobado.') }
    finally { setBusy(false) }
  }
  const signed = state?.signed[0]
  const collectingSignatures = state?.documentState === 'DRAFT' && state.pendingAuthors.length > 0
  useEffect(() => { if (state) onPreviewChange?.(signed ?? null, state.actorSigned) }, [state, signed, onPreviewChange])
  return <section className="card" aria-label="Firma del elaborador">
    <div className="section-heading">
      <span className="section-icon"><Icon name="lock" /></span>
      <div>
        <span className="section-kicker">FIRMA DEL DOCUMENTO</span>
        <h2>{state?.actorSigned || state && !collectingSignatures ? 'Firmas del documento' : 'Firmar PDF'}</h2>
      </div>
    </div>
    <p>{state?.actorSigned && state.pendingAuthors.length === 0 ? 'Su firma está registrada. Puede consultar el documento y el estado de revisión.' : state?.automaticSubmission ? 'Cada elaborador firma el mismo artefacto. Al completarse la última firma se activa automáticamente la revisión.' : 'La firma se aplica al artefacto conservado. Este documento anterior necesita reconciliación con un ámbito para iniciar el flujo.'}</p>
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    {!state ? (error ? <button type="button" className="secondary" disabled={busy || disabled} onClick={() => { setError(''); setRefresh(value => value + 1) }}>Reintentar requisitos de firma</button> : <p role="status">Consultando requisitos de firma…</p>) : <>
      {signed && state.pendingAuthors.length > 0 && <p className="field-help">Recopilando firmas de elaboradores. El contenido está bloqueado; la revisión empezará al completar todas las firmas.</p>}
      {state.pendingAuthors.length > 0 && <p role="status">Pendientes de firma: {state.pendingAuthors.map(person => person.name).join(' → ')}.</p>}
      {collectingSignatures && !state.actorSigned && state.signingBlockers.length > 0 && <ul>{state.signingBlockers.map((b, i) => <li key={i}><strong>{b.section}:</strong> {b.message}</li>)}</ul>}
      {collectingSignatures && !state.actorSigned && <ActionForm label="Preparar firma" dirty={dirty} onCancel={() => markPending(false)} disabled={busy || disabled || !state.signingEnabled}><form onChangeCapture={() => markPending(true)} onResetCapture={() => markPending(false)} onSubmit={event => void sign(event)}><fieldset disabled={busy || disabled || !state.signingEnabled}>
        <label>Certificado personal (.p12/.pfx)<input type="file" name="certificate" accept=".p12,.pfx" required /></label>
        <label>Contraseña del certificado<input type="password" name="certificatePassword" autoComplete="off" required maxLength={128} /></label>
        <button>{busy ? signingPhase === 'checking' ? 'Comprobando certificado…' : signingPhase === 'confirming' ? 'Esperando confirmación…' : 'Firmando…' : 'Firmar'}</button>
      </fieldset></form></ActionForm>}
      {signed && <>
        <div className="signing-success-banner">
          <Icon name="check" size={24} />
          <div>
            <strong>Documento firmado digitalmente</strong>
            <span>Integridad y vigencia comprobadas.</span>
          </div>
        </div>
        <h3>Firmas de elaboración registradas</h3><ol className="scope-timeline">{[...state.signed].reverse().map(signature => <li key={signature.id}><strong>{signature.signerName}</strong> · {new Date(signature.signedAt).toLocaleString('es-EC')}</li>)}</ol>
        {signed.actorName && <p className="field-help">Acción registrada por: {signed.actorName}.</p>}
        <p>Firmado por {signed.signerName} · {new Date(signed.signedAt).toLocaleString('es-EC')}.</p>
        {state.documentState === 'APPROVED' && <button type="button" className="secondary" disabled={busy} onClick={() => void downloadApproved()}>Descargar PDF aprobado</button>}
        {state.automaticSubmission && <p>Estado documental: {state.documentState === 'IN_REVIEW' ? 'En revisión' : state.documentState === 'APPROVED' ? 'Aprobado' : state.pendingAuthors.length ? 'Pendiente de otras firmas' : 'Borrador'}. Consulte las etapas y observaciones desde el documento.</p>}

      </>}
    </>}
    <div className="review-actions"><button type="button" className="secondary" disabled={busy || dirty} onClick={() => { setError(''); setRefresh(value => value + 1) }}>Actualizar estado de firma</button>
    {onFinish && (state?.actorSigned || state?.documentState === 'APPROVED') && <button type="button" disabled={busy || dirty} onClick={onFinish}>Finalizar y volver a Documentos<Icon name="arrow" size={17} /></button>}</div>
  </section>
}
