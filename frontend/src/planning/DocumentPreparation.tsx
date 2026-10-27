import { createPortal } from 'react-dom'
import { SignaturePlacement } from './SignaturePlacement'
import type { PreparedPreview } from './PlanPreview'
import { StepPagination } from './StepPagination'
import { usePlanStepGuard } from './PlanStepNavigation'
import { confirmAction } from '../shared/confirm-action'
import { FormalSubmission } from './FormalSubmission'
import { DocumentSections } from './DocumentSections'
import { Toast } from '../shared/Toast'
import { useEffect, useState } from 'react'
import { api, ApiError } from '../shared/api'
import { SignaturePreparation } from './SignaturePreparation'
import { AuthorSigning } from './AuthorSigning'
import { Icon } from '../shared/Icon'
import { DocumentPageNavigator } from '../shared/DocumentPageNavigator'
import { navigatePdfWithArrow } from '../shared/pdf-keyboard'
import type { WorkPlan } from './types'
interface Readiness { rowVersion: number; ready: boolean; blockers: { section: string; message: string }[] }
interface Artifact { id: string; pageCount: number; sourceRowVersion: number; createdAt: string; current: boolean; pages: { number: number; kind: string; width: number; height: number }[]; signatureSlots: { actorId?: string; label: string; pageNumber: number }[] }
export function DocumentPreparation({ targetDocId, onBack, documentType = 'T1', mode = 'combined', onDirtyChange, onPrevious, onNext, onResolveSection, onDocumentChange, externalPreview = false, onPreparedChange, signingTarget }: { signingTarget?: HTMLElement | null; externalPreview?: boolean; onPreparedChange?: (preview: PreparedPreview) => void; onDocumentChange?: (plan: WorkPlan) => void; onResolveSection?: (section: 'general' | 'content' | 'matrix' | 'attachments') => Promise<void>; onPrevious?: () => Promise<void>; onNext?: () => Promise<void>; targetDocId: string; onBack: () => void; documentType?: 'T1' | 'T2' | 'T3'; mode?: 'preview' | 'sign' | 'combined'; onDirtyChange?: (value: boolean) => void }) {
  const path = '/work-plans/' + targetDocId + '/artifacts'
  const [ready, setReady] = useState<Readiness | null>(null); const [artifacts, setArtifacts] = useState<Artifact[]>([])
  const [signingDirty, setSigningDirty] = useState(false), [signingBusy, setSigningBusy] = useState(false)
  const [sectionsDirty, setSectionsDirty] = useState(false), [formalDirty, setFormalDirty] = useState(false)
  const [sectionsBusy, setSectionsBusy] = useState(false), [formalBusy, setFormalBusy] = useState(false)
  useEffect(() => { onDirtyChange?.(sectionsDirty || formalDirty || signingDirty); return () => onDirtyChange?.(false) }, [sectionsDirty, formalDirty, signingDirty, onDirtyChange])
  const [selected, setSelected] = useState<Artifact | null>(null); const [page, setPage] = useState(0); const [plan, setPlan] = useState<WorkPlan | null>(null)
  const [error, setError] = useState(''); const [busy, setBusy] = useState(true); const [imageError, setImageError] = useState(false); const [reload, setReload] = useState(0)
  const [loadedImage, setLoadedImage] = useState(''), [imageAttempt, setImageAttempt] = useState(0)
  useEffect(() => { onPreparedChange?.({ artifact: selected, busy, error }) }, [selected, busy, error, onPreparedChange])
  const label = documentType === 'T1' ? 'Plan' : 'Informe'
  const automaticPreview = mode === 'preview'
  const finalReview = externalPreview && mode === 'sign'
  const [reviewedArtifact, setReviewedArtifact] = useState<string | null>(null)
  const pendingChanges = sectionsDirty || formalDirty || signingDirty
  const operationBusy = busy || signingBusy || sectionsBusy || formalBusy
  const signingNeedsPreview = mode === 'sign' && !!plan?.editable && plan.documentState === 'DRAFT' && (!selected?.current || !ready?.ready)
  const blockerSections: Record<string, 'general' | 'content' | 'matrix' | 'attachments'> = {
    'Información general': 'general', 'Contenido': 'content', 'Actividades y matriz': 'matrix', 'Anexos': 'attachments',
  }
  useEffect(() => {
    let active = true
    async function load() {
      await Promise.resolve()
      if (!active) return
      setBusy(true); setError(''); setReady(null); setSelected(null); setImageError(false); setLoadedImage('')
      try {
        const [r, a, p] = await Promise.all([api<Readiness>(path + '/readiness'), api<Artifact[]>(path), api<WorkPlan>('/work-plans/' + targetDocId)])
        if (!active) return
        setReady(r); setArtifacts(a); setPlan(p); onDocumentChange?.(p); setPage(0)
        const current = a.find(item => item.current)
        if (automaticPreview && !current && r.ready && p.editable && p.documentState === 'DRAFT') {
          let artifact: Artifact | undefined
          for (let attempt = 0; active && !artifact; attempt++) {
            try { artifact = await api<Artifact>(path, 'POST', { rowVersion: r.rowVersion }) }
            catch (e) {
              if (!(e instanceof ApiError) || e.status !== 503 || !e.message.includes('otra generación en curso') || attempt >= 3) throw e
              // A preceding live preview may still be finishing on the server.
              await new Promise(resolve => window.setTimeout(resolve, 1200))
            }
          }
          if (!active || !artifact) return
          setArtifacts([artifact, ...a.filter(item => item.id !== artifact.id)]); setSelected(artifact)
        } else setSelected(current ?? a[0] ?? null)
      } catch (e) { if (active) setError(e instanceof Error ? e.message : 'No se pudo preparar el documento.') }
      finally { if (active) setBusy(false) }
    }
    void load()
    return () => { active = false }
  }, [path, targetDocId, reload, automaticPreview, onDocumentChange])
  const embedded = usePlanStepGuard(async () => {
    if (operationBusy) return false
    return !(sectionsDirty || formalDirty || signingDirty) || await confirmAction('Hay cambios sin guardar. ¿Cambiar de paso y descartarlos?')
  }, operationBusy)
  async function navigate(action: () => Promise<void>) {
    if (operationBusy) return
    if ((sectionsDirty || formalDirty || signingDirty) && !(await confirmAction('Hay cambios sin guardar. ¿Cambiar de paso y descartarlos?'))) return
    setBusy(true); setError('')
    try { await action() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo cambiar de paso.') } finally { setBusy(false) }
  }
  async function generate() {
    if (!ready || operationBusy || pendingChanges) return; setBusy(true); setError('')
    try { const artifact = await api<Artifact>(path, 'POST', { rowVersion: ready.rowVersion }); setSelected(artifact); setPage(0); setImageError(false); setArtifacts(await api<Artifact[]>(path)) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo generar el PDF.') }
    finally { setBusy(false) }
  }
  return <section className={embedded ? 'plan-step-panel' : undefined}>{!embedded && <button className="secondary" disabled={operationBusy} onClick={() => void navigate(async () => onBack())}>Volver al {label}</button>}<span className="section-kicker review-kicker">{documentType === 'T1' ? (mode === 'sign' ? externalPreview ? 'PASO 6 DE 6 · REVISIÓN FINAL Y FIRMA' : 'PASO 6 DE 6 · FIRMA Y FINALIZACIÓN' : externalPreview ? 'PASO 5 DE 6 · PREPARACIÓN DE FIRMA' : 'PASO 5 DE 6 · PREPARACIÓN Y PREVISUALIZACIÓN') : 'DOCUMENTO INSTITUCIONAL · ' + documentType}</span>{embedded ? <h2 className="plan-step-title">{mode === 'sign' ? externalPreview ? 'Revisión final y firma' : 'Firma y finalización' : externalPreview ? 'Preparación de firma' : 'Preparación y previsualización ' + documentType}</h2> : <h1>{mode === 'sign' ? externalPreview ? 'Revisión final y firma' : 'Firma y finalización' : externalPreview ? 'Preparación de firma' : 'Preparación y previsualización ' + documentType}</h1>}
    {error && <Toast message={error} onDismiss={() => setError('')} />}
    {!finalReview && <><FormalSubmission targetDocId={targetDocId} disabled={busy || signingDirty || signingBusy || sectionsBusy} onDirtyChange={setFormalDirty} onBusyChange={setFormalBusy} onRegistered={() => setReload(n => n + 1)} />
    <DocumentSections key={targetDocId + reload} targetDocId={targetDocId} disabled={busy || signingDirty || signingBusy || formalBusy} onDirtyChange={setSectionsDirty} onBusyChange={setSectionsBusy} onSaved={() => setReload(n => n + 1)} /></>}
    {mode === 'sign' ? <>
      {busy && <p role="status">Consultando documento preparado…</p>}
      {!busy && !ready && <button type="button" className="secondary" disabled={operationBusy || pendingChanges} onClick={() => setReload(n => n + 1)}>Reintentar consulta del documento</button>}
      {!busy && signingNeedsPreview && <section className="notice" aria-label="Previsualización pendiente"><h2>Actualice la previsualización antes de firmar</h2><p>Vuelva al paso 5 para actualizar la preparación del documento. La firma utilizará ese mismo documento preparado.</p>{onPrevious && <button type="button" className="secondary" disabled={signingBusy || sectionsDirty || formalDirty || signingDirty} onClick={() => void navigate(onPrevious)}>Volver a preparación de firma</button>}</section>}
      {!busy && plan && !plan.editable && !selected && <p role="status">Este documento no tiene un artefacto preparado disponible para firmar.</p>}
    </> : automaticPreview ? <>
      {busy && <p role="status">Preparando documento…</p>}
      {!busy && (!ready || (ready.ready && !selected && plan?.editable)) && <button type="button" className="secondary" disabled={operationBusy || pendingChanges} onClick={() => setReload(n => n + 1)}>Reintentar previsualización</button>}
      {!busy && ready && !ready.ready && <section className="card" aria-label="Datos pendientes"><h2>Complete los datos pendientes</h2><ul>{ready.blockers.map((blocker, index) => <li key={index}><strong>{blocker.section}:</strong> {blocker.message}{onResolveSection && blockerSections[blocker.section] && <button type="button" className="link" disabled={formalDirty || sectionsDirty} onClick={() => void navigate(() => onResolveSection(blockerSections[blocker.section]))}>Ir a {blocker.section.toLowerCase()}</button>}</li>)}</ul></section>}
      {!busy && !error && plan && !plan.editable && !selected && <p role="status">Este documento de solo lectura todavía no tiene una previsualización conservada.</p>}
    </> : !ready ? (busy ? <p role="status">Consultando preparación…</p> : <button type="button" className="secondary" disabled={operationBusy || pendingChanges} onClick={() => setReload(n => n + 1)}>Reintentar preparación</button>) : <div className="card"><div className="section-heading"><span className="section-icon"><Icon name="document" /></span><div><span className="section-kicker">PREPARACIÓN DOCUMENTAL</span><h2>Revisión del contenido</h2></div></div>{ready.ready ? <p role="status">Contenido completo para previsualizar.</p> : <ul>{ready.blockers.map((b, i) => <li key={i}><strong>{b.section}:</strong> {b.message}</li>)}</ul>}{plan?.editable ? <button disabled={operationBusy || pendingChanges || !ready.ready} onClick={() => void generate()}>{busy ? 'Generando PDF…' : 'Generar previsualización'}</button> : <p className="field-help">Documento de solo lectura. Consulte su artefacto conservado y las rondas de revisión.</p>}</div>}
    {selected && <div className={'card ' + (externalPreview ? 'preparation-status-card' : 'document-keyboard-viewer')} role={externalPreview ? undefined : "region"} aria-label={externalPreview ? undefined : "Previsualización del documento"} tabIndex={automaticPreview && !externalPreview ? 0 : undefined} aria-keyshortcuts={automaticPreview ? 'ArrowLeft ArrowRight' : undefined} aria-describedby={automaticPreview && !externalPreview ? 'pdf-keyboard-help' : undefined} onKeyDown={event => { if (automaticPreview && !externalPreview) navigatePdfWithArrow(event, page, selected.pages.length, busy || (!imageError && loadedImage !== selected.id + '-' + page), target => { setPage(target); setImageError(false); setImageAttempt(0) }) }}>
      {automaticPreview && !externalPreview && <p id="pdf-keyboard-help" className="field-help">Con el visor seleccionado, usa ← → para cambiar de página.</p>}
      <div className="review-summary">
        <div className="review-summary-meta">
          <span className={'status-badge ' + (selected.current ? 'active' : '')}>
            {selected.current ? 'Coincide con el contenido actual' : 'Artefacto conservado'}
          </span>
          <span className="muted">
            Espacios de firma: {selected.signatureSlots.map(s => s.label + ', página ' + s.pageNumber).join('; ')}.
          </span>
        </div>
        {artifacts.length > (automaticPreview ? 1 : 0) && (
          <label className="review-artifact-picker">
            <span className="sr-only">Previsualizaciones guardadas</span>
            <select
              aria-label="Previsualizaciones guardadas"
              value={selected.id}
              disabled={operationBusy || pendingChanges}
              onChange={e => { setSelected(artifacts.find(a => a.id === e.target.value) ?? null); setPage(0); setImageError(false) }}
            >
              {artifacts.map(a => <option value={a.id} key={a.id}>{new Date(a.createdAt).toLocaleString('es-EC')} · {a.pageCount} págs. · {a.current ? 'Contenido actual' : 'Artefacto conservado'}</option>)}
            </select>
          </label>
        )}
      </div>
      {!selected.current && <p className="field-help">Artefacto conservado de una preparación anterior. Consulte las rondas para ver los bytes firmados.</p>}
      {externalPreview && mode === 'preview' && !busy && ready && <>
        <SignaturePlacement key={selected.id} current={selected.current} targetDocId={targetDocId} artifactId={selected.id} slots={selected.signatureSlots} disabled={operationBusy || pendingChanges} onEditAuthors={onResolveSection ? () => void navigate(() => onResolveSection('general')) : undefined} />
        <SignaturePreparation key={targetDocId + selected.id} targetDocId={targetDocId} artifactId={selected.id} />
      </>}
      {!externalPreview && mode !== 'preview' && !signingNeedsPreview && !busy && ready && <SignaturePreparation key={targetDocId + selected.id} targetDocId={targetDocId} artifactId={selected.id} />}
      {mode !== 'preview' && !signingNeedsPreview && !busy && ready && (() => {
        const actions = <>
          {finalReview && <section className="plan-review-confirmation" aria-label="Confirmación de revisión final">
            <label className="check"><input type="checkbox" checked={reviewedArtifact === selected.id} disabled={signingBusy || signingDirty} onChange={event => setReviewedArtifact(event.target.checked ? selected.id : null)} />He revisado el documento completo y confirmo que está listo para firmar.</label>
            <p className="field-help">La primera firma conserva este contenido. Cuando firmen todos los elaboradores se iniciará la revisión institucional.</p>
          </section>}
          <AuthorSigning key={'sign-' + targetDocId + selected.id} targetDocId={targetDocId} artifactId={selected.id} disabled={sectionsDirty || formalDirty || sectionsBusy || formalBusy || finalReview && reviewedArtifact !== selected.id} onDirtyChange={setSigningDirty} onBusyChange={setSigningBusy} onSigned={() => { setReload(n => n + 1) }} />
        </>
        return finalReview ? signingTarget ? createPortal(actions, signingTarget) : null : actions
      })()}
      {!externalPreview && <><DocumentPageNavigator key={selected.id} page={page} pageCount={selected.pageCount} disabled={busy || (!imageError && loadedImage !== selected.id + '-' + page)} onChange={target => { setPage(target); setImageError(false);setImageAttempt(0) }} references={[...selected.signatureSlots.map(slot => ({ page: slot.pageNumber - 1, label: slot.label })), ...selected.pages.filter((item, index) => item.kind === 'ANNEX' && selected.pages[index - 1]?.kind !== 'ANNEX').map(item => ({ page: item.number - 1, label: 'Anexos y evidencias' }))]} />
      {imageError && <><Toast message="No se pudo cargar la página. Pulse Reintentar página." /><button type="button" className="secondary" onClick={() => { setImageError(false);setLoadedImage('');setImageAttempt(n => n + 1) }}>Reintentar página</button></>}
      {!imageError && loadedImage !== selected.id + '-' + page && <p role="status" className="field-help">Cargando página {page + 1}…</p>}
      <div className="document-reading-desk">
        <div className="document-page-wrapper">
          <img className="document-page" key={selected.id + '-' + page + '-' + imageAttempt} width={Math.round(selected.pages[page].width * 1.5)} height={Math.round(selected.pages[page].height * 1.5)} src={'/api' + path + '/' + selected.id + '/pages/' + page + '?attempt=' + imageAttempt} alt={'Página ' + (page + 1) + ' del ' + label + ' ' + documentType} onLoad={() => setLoadedImage(selected.id + '-' + page)} onError={() => setImageError(true)} />
        </div>
      </div></>}
    </div>}
    {(onPrevious || onNext) && <StepPagination previous={onPrevious ? { label: mode === 'sign' ? externalPreview ? 'Preparación de firma' : 'Preparación y previsualización' : 'Anexos y opciones', action: () => void navigate(onPrevious) } : undefined} next={onNext ? { label: externalPreview ? 'Revisión final y firma' : 'Firma y finalización', action: () => void navigate(onNext) } : undefined} disabled={operationBusy} nextDisabled={sectionsDirty || formalDirty || signingDirty || automaticPreview && (!selected || !selected.current || !ready?.ready || !!error)} />}
  </section>
}
