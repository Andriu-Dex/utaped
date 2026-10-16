import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { useEffect, useState, type FormEvent } from 'react'
import { api, signPdf } from '../shared/api'
import { Icon } from '../shared/Icon'

interface Signed { id: string; signerName: string; signedAt: string; pageCount: number; signaturePageIndex: number }
interface SigningState {
  automaticSubmission: boolean; documentState: string
  signingEnabled: boolean; signingBlockers: { section: string; message: string }[]; signed: Signed[]
  preparation: { rowVersion: number; pdfHash: string }
}
export function AuthorSigning({ targetDocId, artifactId, onSigned }: { targetDocId: string; artifactId: string; onSigned?: () => void }) {
  const path = '/work-plans/' + targetDocId + '/artifacts/' + artifactId + '/signing'
  const [state, setState] = useState<SigningState | null>(null); const [error, setError] = useState('')
  const [busy, setBusy] = useState(false); const [notice, setNotice] = useState(''); const [refresh, setRefresh] = useState(0)
  const [requestKey] = useState(() => crypto.randomUUID()); const [page, setPage] = useState<number | null>(null)
  const [imageError, setImageError] = useState(false)
  useEffect(() => { let active = true; api<SigningState>(path).then(value => { if (active) setState(value) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [path, refresh])
  async function sign(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!state?.signingEnabled || busy) return
    const form = event.currentTarget; const certificate = form.elements.namedItem('certificate') as HTMLInputElement
    const credential = form.elements.namedItem('certificatePassword') as HTMLInputElement
    const file = certificate.files?.[0]; const password = credential.value; form.reset()
    if (!file) return
    setBusy(true); setError(''); setNotice('')
    try {
      await signPdf(path + '?' + new URLSearchParams({ rowVersion: String(state.preparation.rowVersion), inputHash: state.preparation.pdfHash, requestKey }), file, password)
      setNotice('Firma aplicada. Identidad vinculada, vigencia e integridad comprobadas.'); setImageError(false); setPage(null)
      onSigned?.()
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo firmar el documento.') }
    finally { form.reset(); setBusy(false); setRefresh(value => value + 1) }
  }
  const signed = state?.signed[0]
  const index = page ?? signed?.signaturePageIndex ?? 0
  return <section className="card" aria-label="Firma del elaborador">
    <div className="section-heading">
      <span className="section-icon"><Icon name="lock" /></span>
      <div>
        <span className="section-kicker">AUTORÍA Y VALIDEZ JURÍDICA</span>
        <h2>Firmar PDF</h2>
      </div>
    </div>
    <p>{state?.automaticSubmission ? 'Al firmar se conserva el artefacto y se activa automáticamente la primera etapa del flujo configurado.' : 'La firma se aplica al artefacto conservado. Este documento anterior necesita reconciliación con un ámbito para iniciar el flujo.'}</p>
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    {!state ? <p role="status">Consultando requisitos de firma…</p> : <>
      {state.signingBlockers.length > 0 && !signed && <ul>{state.signingBlockers.map((b, i) => <li key={i}><strong>{b.section}:</strong> {b.message}</li>)}</ul>}
      {!signed && <ActionForm label="Preparar firma" disabled={busy || !state.signingEnabled}><form onSubmit={event => void sign(event)}><fieldset disabled={busy || !state.signingEnabled}>
        <label>Certificado personal (.p12/.pfx)<input type="file" name="certificate" accept=".p12,.pfx" required /></label>
        <label>Contraseña del certificado<input type="password" name="certificatePassword" autoComplete="off" required maxLength={128} /></label>
        <button>{busy ? 'Firmando…' : 'Firmar'}</button>
      </fieldset></form></ActionForm>}
      {signed && <>
        <div className="signing-success-banner">
          <Icon name="check" size={24} />
          <div>
            <strong>Documento firmado digitalmente</strong>
            <span>Identidad vinculada, integridad y vigencia comprobadas.</span>
          </div>
        </div>
        <p>Firmado por {signed.signerName} · {new Date(signed.signedAt).toLocaleString('es-EC')}.</p>
        <p className="field-help">Firma e identidad vinculada comprobadas. La cadena de la entidad emisora y la revocación no fueron comprobadas.</p>
        {!state.automaticSubmission && <a href={'/api/work-plans/' + targetDocId + '/signed-artifacts/' + signed.id + '/content'}>Descargar PDF firmado</a>}
        {state.automaticSubmission && <p>Estado documental: {state.documentState === 'IN_REVIEW' ? 'En revisión' : state.documentState === 'APPROVED' ? 'Aprobado' : 'Borrador'}. Consulte las etapas y observaciones desde el documento.</p>}
        <nav aria-label="Páginas del PDF firmado" style={{ margin: '14px 0' }}><button className="secondary" disabled={index === 0} onClick={() => { setPage(index - 1); setImageError(false) }}>Anterior del firmado</button><span> Página {index + 1} de {signed.pageCount} </span><button className="secondary" disabled={index + 1 >= signed.pageCount} onClick={() => { setPage(index + 1); setImageError(false) }}>Siguiente del firmado</button></nav>
        {imageError && <Toast message="No se pudo cargar la página firmada. Puede descargar el PDF." />}
        <div className="document-reading-desk">
          <div className="document-page-wrapper">
            <img className="document-page" src={'/api/work-plans/' + targetDocId + '/signed-artifacts/' + signed.id + '/pages/' + index} alt={'Página ' + (index + 1) + ' del PDF firmado'} onError={() => setImageError(true)} />
          </div>
        </div>
      </>}
    </>}
    <button className="secondary" disabled={busy} onClick={() => { setError(''); setRefresh(value => value + 1) }}>Actualizar estado de firma</button>
  </section>
}
