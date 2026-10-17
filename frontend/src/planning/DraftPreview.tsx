import { useEffect, useRef, useState } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'
import { Icon } from '../shared/Icon'
import { DocumentPageNavigator } from '../shared/DocumentPageNavigator'
import './draft-preview.css'

interface Preview {
  sourceRowVersion: number
  pages: { number: number; width: number; height: number }[]
  sections: Record<'JUSTIFICATION' | 'OBJECTIVE', number>
  pageIndex: number
  image: string
}
export function DraftPreview({ targetDocId, rowVersion, title, institutionalUnit, career, justification, objective, focus, dirty }: {
  targetDocId: string; rowVersion: number; title: string; institutionalUnit: string; career: string; justification: string; objective: string
  focus: 'JUSTIFICATION' | 'OBJECTIVE'; dirty: boolean
}) {
  const [result, setResult] = useState<{ value: Preview; fingerprint: string } | null>(null)
  const [requestedPage, setRequestedPage] = useState(-1)
  const [retry, setRetry] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const pending = useRef<Promise<void> | null>(null)
  const dialog = useRef<HTMLDialogElement>(null)
  const fingerprint = JSON.stringify([targetDocId, rowVersion, title, institutionalUnit, career, justification, objective])
  const [oldRequest, setOldRequest] = useState({ fingerprint, focus })
  if (oldRequest.fingerprint !== fingerprint || oldRequest.focus !== focus) {
    setOldRequest({ fingerprint, focus })
    if (requestedPage !== -1) setRequestedPage(-1)
  }
  useEffect(() => {
    let active = true
    const timer = window.setTimeout(() => {
      const previous = pending.current
      pending.current = (async () => {
        // Serialize this editor's requests; obsolete responses never replace newer text or another document.
        if (previous) await previous
        if (!active) return
        setBusy(true); setError('')
        try {
          const value = await api<Preview>('/work-plans/' + targetDocId + '/artifacts/draft-preview', 'POST', {
            rowVersion, title, institutionalUnit, career, justification, objective, pageIndex: requestedPage, section: focus,
          })
          if (active) setResult({ value, fingerprint })
        } catch (e) { if (active) setError(e instanceof Error ? e.message : 'No se pudo actualizar la vista.') }
        finally { if (active) setBusy(false) }
      })()
    }, requestedPage >= 0 ? 0 : 1400)
    return () => { active = false; window.clearTimeout(timer) }
  }, [targetDocId, rowVersion, title, institutionalUnit, career, justification, objective, fingerprint, focus, requestedPage, retry])
  const stale = result?.fingerprint !== fingerprint
  const image = result ? 'data:image/png;base64,' + result.value.image : undefined
  return <aside className="draft-preview-panel" aria-label="Previsualización simultánea del Plan">
    <header className="draft-preview-heading"><span className="section-icon"><Icon name="document" /></span><div><span className="section-kicker">DOCUMENTO A4 · T1</span><h3>Así queda tu Plan</h3></div><span className="status-badge">Borrador</span></header>
    <p className="draft-preview-explanation">Vista de edición con tus textos actuales. Guardar borrador conserva los cambios; la firma se realiza en el paso 6.</p>
    <div className="draft-preview-toolbar"><span role="status">{busy ? 'Actualizando vista…' : stale ? 'Cambios pendientes de previsualizar' : dirty ? 'Vista actualizada · cambios sin guardar' : 'Vista del contenido guardado'}</span><button type="button" className="secondary" onClick={() => setRetry(n => n + 1)} disabled={busy}>Actualizar vista</button><button type="button" className="secondary" disabled={!result || busy || stale} onClick={() => dialog.current?.showModal()} title="Ampliar página" aria-label="Ampliar página"><Icon name="eye" size={18} /></button></div>
    {error && <><Toast message={error} onDismiss={() => setError('')} /><p className="field-help">La vista no se actualizó. Tus textos permanecen en el formulario. Pulsa Actualizar vista para reintentar.</p></>}
    {result ? <>
      <DocumentPageNavigator page={result.value.pageIndex} pageCount={result.value.pages.length} disabled={busy || stale} onChange={setRequestedPage} references={[{ page: result.value.sections.JUSTIFICATION, label: 'Justificación' }, { page: result.value.sections.OBJECTIVE, label: 'Objetivo' }]} />
      <div className={'draft-preview-desk' + (stale ? ' is-stale' : '')} aria-busy={busy}><img src={image} alt={'Página ' + (result.value.pageIndex + 1) + ' del borrador A4 del Plan'} /></div>
      <dialog ref={dialog} className="draft-preview-dialog" aria-labelledby="draft-preview-expanded-title"><header><h2 id="draft-preview-expanded-title">Borrador A4 · Página {result.value.pageIndex + 1}</h2><button type="button" className="secondary" onClick={() => dialog.current?.close()}>Cerrar vista ampliada</button></header><img src={image} alt={'Página ampliada ' + (result.value.pageIndex + 1) + ' del borrador'} /></dialog>
    </> : <div className="draft-preview-empty"><Icon name="document" size={40} /><strong>Tu documento, a la vista</strong><p>La previsualización aparecerá aquí. Puedes seguir escribiendo mientras se prepara.</p></div>}
    <p className="field-help">Las secciones incompletas pueden quedar vacías. La preparación final comprobará los requisitos antes de firmar.</p>
  </aside>
}
