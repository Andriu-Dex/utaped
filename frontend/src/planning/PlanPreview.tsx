import { useEffect, useId, useRef, useState } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import { DocumentPageNavigator } from '../shared/DocumentPageNavigator'
import { navigatePdfWithArrow } from '../shared/pdf-keyboard'
import { containDialogFocus } from '../shared/dialog-focus'
import type { WorkPlan } from './types'
import type { PlanStep } from './PlanStepNavigation'
import './plan-preview.css'

export interface PreviewArtifact {
  id: string; pageCount: number; current: boolean
  pages: { number: number; width: number; height: number; kind: string }[]
  signatureSlots: { label: string; pageNumber: number }[]
}
export interface PreparedPreview { artifact: PreviewArtifact | null; busy: boolean; error: string }
type Focus = 'COVER' | 'JUSTIFICATION' | 'OBJECTIVE' | 'MATRIX' | 'ANNEXES' | 'SIGNATURES'
interface DraftView { sourceRowVersion: number; pages: PreviewArtifact['pages']; sections: Partial<Record<Focus, number>>; pageIndex: number; image: string }
const sectionLabels: Record<Focus, string> = { COVER: 'Portada', JUSTIFICATION: 'Justificación', OBJECTIVE: 'Objetivo', MATRIX: 'Matriz de actividades', ANNEXES: 'Anexos', SIGNATURES: 'Firmas' }

export function PlanPreview({ plan, draft, step, focus, dirty, textDirty, prepared }: {
  plan: WorkPlan; draft: Pick<WorkPlan, 'title' | 'institutionalUnit' | 'career' | 'justification' | 'objective'>
  step: PlanStep; focus: 'JUSTIFICATION' | 'OBJECTIVE'; dirty: boolean; textDirty: boolean; prepared: PreparedPreview | null
}) {
  const [result, setResult] = useState<{ value: DraftView; fingerprint: string } | null>(null)
  const [requestedPage, setRequestedPage] = useState(-1), [retry, setRetry] = useState(0)
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  const [retained, setRetained] = useState<PreviewArtifact | null>(null)
  const [artifactPage, setArtifactPage] = useState(0)
  const [loadedImage, setLoadedImage] = useState(''), [imageError, setImageError] = useState(''), [imageRetry, setImageRetry] = useState(0)
  const pending = useRef<Promise<void> | null>(null)
  const [expanded, setExpanded] = useState(false)
  const dialog = useRef<HTMLDialogElement>(null), origin = useRef<HTMLElement | null>(null)
  const helpId = useId(), titleId = useId()
  const finalStep = step === 'preparation' || step === 'signing'
  const editing = plan.editable && plan.documentState === 'DRAFT' && !finalStep
  const section: Focus = step === 'general' ? 'COVER' : step === 'content' ? focus : step === 'matrix' ? 'MATRIX' : step === 'attachments' ? 'ANNEXES' : step === 'signing' ? 'SIGNATURES' : 'COVER'
  const fingerprint = JSON.stringify([plan.id, plan.rowVersion, draft])
  const [previous, setPrevious] = useState({ fingerprint, section, step })
  if (previous.fingerprint !== fingerprint || previous.section !== section || previous.step !== step) {
    setPrevious({ fingerprint, section, step }); setRequestedPage(-1)
    setArtifactPage(step === 'signing' ? Math.max(0, (prepared?.artifact?.signatureSlots[0]?.pageNumber ?? 1) - 1) : 0)
  }
  useEffect(() => {
    if (!editing) return
    let active = true
    const timer = window.setTimeout(() => {
      const earlier = pending.current
      pending.current = (async () => {
        if (earlier) await earlier
        if (!active) return
        setBusy(true); setError('')
        try {
          const value = await api<DraftView>('/work-plans/' + plan.id + '/artifacts/draft-preview', 'POST', {
            ...draft, rowVersion: plan.rowVersion, section, pageIndex: requestedPage,
          })
          if (active) setResult({ value, fingerprint })
        } catch (e) { if (active) setError(e instanceof Error ? e.message : 'No se pudo actualizar la vista.') }
        finally { if (active) setBusy(false) }
      })()
    }, requestedPage >= 0 ? 0 : 1400)
    return () => { active = false; window.clearTimeout(timer) }
  }, [editing, plan.id, plan.rowVersion, draft, fingerprint, section, requestedPage, retry])
  useEffect(() => {
    if (editing || finalStep) return
    let active = true
    api<PreviewArtifact[]>('/work-plans/' + plan.id + '/artifacts').then(items => {
      if (active) { setRetained(items.find(item => item.current) ?? items[0] ?? null); setError('') }
    }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [plan.id, plan.rowVersion, editing, finalStep, retry])
  const artifact = finalStep ? prepared?.artifact : retained
  const [previousArtifact, setPreviousArtifact] = useState<string | undefined>(undefined)
  if (previousArtifact !== artifact?.id) {
    setPreviousArtifact(artifact?.id)
    setArtifactPage(step === 'signing' ? Math.max(0, (artifact?.signatureSlots[0]?.pageNumber ?? 1) - 1) : 0)
  }
  const pages = editing ? result?.value.pages : artifact?.pages
  const page = editing ? result?.value.pageIndex ?? 0 : Math.min(artifactPage, Math.max(0, (pages?.length ?? 1) - 1))
  const stale = editing && result?.fingerprint !== fingerprint
  const awaitingSection = editing && requestedPage < 0 && !!result && result.value.pageIndex !== (result.value.sections[section] ?? result.value.sections.SIGNATURES)
  const loading = editing ? busy || stale || awaitingSection : finalStep && (!prepared || prepared.busy)
  const image = editing ? result ? 'data:image/png;base64,' + result.value.image : '' : artifact ? '/api/work-plans/' + plan.id + '/artifacts/' + artifact.id + '/pages/' + page + '?attempt=' + imageRetry : ''
  const references = editing ? Object.entries(result?.value.sections ?? {}).map(([key, value]) => ({ page: value!, label: sectionLabels[key as Focus] })) : [
    { page: 0, label: 'Portada' }, ...(artifact?.signatureSlots.map(slot => ({ page: slot.pageNumber - 1, label: slot.label })) ?? []),
    ...(artifact?.pages.filter((item, index) => item.kind === 'ANNEX' && artifact.pages[index - 1]?.kind !== 'ANNEX').map(item => ({ page: item.number - 1, label: 'Anexos y evidencias' })) ?? []),
  ]
  function go(next: number) { setImageError(''); if (editing) setRequestedPage(next); else setArtifactPage(next) }
  function expand() {
    origin.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    setExpanded(true); dialog.current?.showModal()
  }
  useEffect(() => {
    const element = dialog.current
    if (!element) return
    const release = containDialogFocus(element)
    const restore = () => { setExpanded(false); origin.current?.focus() }
    element.addEventListener('close', restore)
    return () => { release(); element.removeEventListener('close', restore); element.close() }
  }, [])
  const viewError = finalStep ? '' : error
  let status = 'Artefacto conservado'
  if (loading) status = 'Actualizando vista…'
  else if (editing) status = dirty ? textDirty ? 'Vista actualizada · cambios sin guardar' : 'Vista guardada · cambios pendientes' : 'Vista del contenido guardado'
  else if (dirty) status = 'Documento preparado · cambios pendientes'
  else if (artifact?.current) status = 'Documento preparado para revisión y firma'
  return <aside className="plan-preview card" aria-label="Previsualización simultánea del Plan">
    <header className="plan-preview-heading"><div><span className="section-kicker">DOCUMENTO INSTITUCIONAL · T1</span><h2>Vista del documento</h2></div><div className="plan-preview-actions">
      {editing && <button type="button" className="secondary icon-only" title="Actualizar vista" aria-label="Actualizar vista" disabled={busy} onClick={() => setRetry(value => value + 1)}><Icon name="refresh" size={16} /></button>}
      <button type="button" className="secondary icon-only" title="Ampliar página" aria-label="Ampliar página" disabled={!image || !!loading || imageError === image} onClick={expand}><Icon name="maximize" size={16} /></button>
    </div></header>
    <div className="plan-preview-state" role="status"><span className={'status-badge ' + (loading || dirty ? 'warning' : 'active')}>{status}</span><span className="muted">{sectionLabels[section]} · A4</span></div>
    {editing && dirty && !textDirty && <p className="field-help">La vista muestra lo guardado. Guarde los cambios de este paso para actualizarla.</p>}
    {editing && step === 'attachments' && result && !result.value.sections.ANNEXES && <p className="field-help">No hay anexos incorporados. Se muestra la sección de firmas que los seguirá.</p>}
    {viewError && <><Toast message={viewError} onDismiss={() => setError('')} /><p className="field-help">Sus datos permanecen en el formulario. La última vista se conserva mientras reintenta.</p>{!finalStep && <button type="button" className="secondary" disabled={busy} onClick={() => setRetry(value => value + 1)}>Reintentar vista</button>}</>}
    {image && pages?.length ? <div className="plan-preview-viewer document-keyboard-viewer" role="region" aria-label="Previsualización del documento" tabIndex={0} aria-keyshortcuts="ArrowLeft ArrowRight" aria-describedby={helpId} onKeyDown={event => navigatePdfWithArrow(event, page, pages.length, !!loading || loadedImage !== image && imageError !== image, go)}>
      <DocumentPageNavigator page={page} pageCount={pages.length} onChange={go} disabled={!!loading || loadedImage !== image && imageError !== image} references={references} />
      <p id={helpId} className="field-help plan-preview-keyboard">Seleccione el visor y use ← → para cambiar de página.</p>
      {imageError === image && <div className="notice"><p>No se pudo cargar la página.</p><button type="button" className="secondary" onClick={() => { setImageError(''); if (editing) setRetry(value => value + 1); else setImageRetry(value => value + 1) }}>Reintentar página</button></div>}
      {loadedImage !== image && imageError !== image && <p role="status" className="field-help">Cargando página {page + 1}…</p>}
      <div className={'document-reading-desk' + (stale ? ' is-stale' : '')} aria-busy={!!loading}>
        <button type="button" className="document-page-wrapper plan-preview-paper" onClick={expand} disabled={!!loading || imageError === image} title="Ampliar documento" aria-label="Ampliar documento">
          <img className="document-page" src={image} width={Math.round(pages[page].width)} height={Math.round(pages[page].height)} alt={'Página ' + (page + 1) + (editing ? ' del borrador A4 del Plan' : ' del Plan T1')} onLoad={() => setLoadedImage(image)} onError={() => setImageError(image)} />
        </button>
      </div>
    </div> : <div className="plan-preview-empty" role="status"><Icon name="document" size={36} /><strong>{loading || editing ? 'Preparando su documento…' : 'Sin documento preparado'}</strong><p>{finalStep ? 'Revise la preparación y los pendientes de este paso.' : 'Puede continuar trabajando mientras se prepara la vista.'}</p></div>}
    <dialog ref={dialog} className="plan-preview-dialog" aria-labelledby={titleId}>
      <header><h2 id={titleId}>{editing ? 'Borrador A4' : 'Documento preparado'} · Página {page + 1} de {pages?.length ?? 0}</h2><button type="button" className="secondary" onClick={() => dialog.current?.close()}>Cerrar vista ampliada</button></header>
      {expanded && <><DocumentPageNavigator page={page} pageCount={pages?.length ?? 1} onChange={go} disabled={!!loading || loadedImage !== image && imageError !== image} references={references} />
      <div className="plan-preview-expanded-paper">{image && <img src={image} alt={'Página ampliada ' + (page + 1)} />}</div></>}
    </dialog>
  </aside>
}
