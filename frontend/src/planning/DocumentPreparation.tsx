import { StepPagination } from './StepPagination'
import { usePlanStepGuard } from './PlanStepNavigation'
import { confirmAction } from '../shared/confirm-action'
import { FormalSubmission } from './FormalSubmission'
import { DocumentSections } from './DocumentSections'
import { Toast } from '../shared/Toast'
import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { SignaturePreparation } from './SignaturePreparation'
import { AuthorSigning } from './AuthorSigning'
import { Icon } from '../shared/Icon'
import { DocumentPageNavigator } from '../shared/DocumentPageNavigator'
import type { WorkPlan } from './types'
interface Readiness { rowVersion: number; ready: boolean; blockers: { section: string; message: string }[] }
interface Artifact { id: string; pageCount: number; sourceRowVersion: number; createdAt: string; current: boolean; pages: { number: number; kind: string; width: number; height: number }[]; signatureSlots: { label: string; pageNumber: number }[] }
export function DocumentPreparation({ targetDocId, onBack, documentType = 'T1', mode = 'combined', onDirtyChange, onPrevious, onNext, onResolveSection, onDocumentChange }: { onDocumentChange?: (plan: WorkPlan) => void; onResolveSection?: (section: 'general' | 'content' | 'matrix' | 'attachments') => Promise<void>; onPrevious?: () => Promise<void>; onNext?: () => Promise<void>; targetDocId: string; onBack: () => void; documentType?: 'T1' | 'T2' | 'T3'; mode?: 'preview' | 'sign' | 'combined'; onDirtyChange?: (value: boolean) => void }) {
  const path = '/work-plans/' + targetDocId + '/artifacts'
  const [ready, setReady] = useState<Readiness | null>(null); const [artifacts, setArtifacts] = useState<Artifact[]>([])
  const [signingDirty, setSigningDirty] = useState(false), [signingBusy, setSigningBusy] = useState(false)
  const [sectionsDirty, setSectionsDirty] = useState(false), [formalDirty, setFormalDirty] = useState(false)
  useEffect(() => { onDirtyChange?.(sectionsDirty || formalDirty || signingDirty); return () => onDirtyChange?.(false) }, [sectionsDirty, formalDirty, signingDirty, onDirtyChange])
  const [selected, setSelected] = useState<Artifact | null>(null); const [page, setPage] = useState(0); const [plan, setPlan] = useState<WorkPlan | null>(null)
  const [error, setError] = useState(''); const [busy, setBusy] = useState(true); const [imageError, setImageError] = useState(false); const [reload, setReload] = useState(0)
  const [loadedImage, setLoadedImage] = useState(''), [imageAttempt, setImageAttempt] = useState(0)
  const label = documentType === 'T1' ? 'Plan' : 'Informe'
  const automaticPreview = mode === 'preview'
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
          const artifact = await api<Artifact>(path, 'POST', { rowVersion: r.rowVersion })
          if (!active) return
          setArtifacts([artifact, ...a.filter(item => item.id !== artifact.id)]); setSelected(artifact)
        } else setSelected(current ?? a[0] ?? null)
      } catch (e) { if (active) setError(e instanceof Error ? e.message : 'No se pudo preparar el documento.') }
      finally { if (active) setBusy(false) }
    }
    void load()
    return () => { active = false }
  }, [path, targetDocId, reload, automaticPreview, onDocumentChange])
  const embedded = usePlanStepGuard(async () => {
    if (busy || signingBusy) return false
    return !(sectionsDirty || formalDirty || signingDirty) || await confirmAction('Hay cambios sin guardar. ¿Cambiar de paso y descartarlos?')
  }, busy || signingBusy)
  async function navigate(action: () => Promise<void>) {
    if ((sectionsDirty || formalDirty || signingDirty) && !(await confirmAction('Hay cambios sin guardar. ¿Cambiar de paso y descartarlos?'))) return
    setBusy(true); setError('')
    try { await action() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo cambiar de paso.') } finally { setBusy(false) }
  }
  async function generate() {
    if (!ready) return; setBusy(true); setError('')
    try { const artifact = await api<Artifact>(path, 'POST', { rowVersion: ready.rowVersion }); setSelected(artifact); setPage(0); setImageError(false); setArtifacts(await api<Artifact[]>(path)) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo generar el PDF.') }
    finally { setBusy(false) }
  }
  return <section className={embedded ? 'plan-step-panel' : undefined}>{!embedded && <button className="secondary" disabled={busy || formalDirty || sectionsDirty} onClick={onBack}>Volver al {label}</button>}<span className="section-kicker review-kicker">{documentType === 'T1' ? (mode === 'sign' ? 'PASO 6 DE 6 · FIRMA Y FINALIZACIÓN' : 'PASO 5 DE 6 · PREPARACIÓN Y PREVISUALIZACIÓN') : 'DOCUMENTO INSTITUCIONAL · ' + documentType}</span>{embedded ? <h2 className="plan-step-title">{mode === 'sign' ? 'Firma y finalización' : 'Preparación y previsualización ' + documentType}</h2> : <h1>{mode === 'sign' ? 'Firma y finalización' : 'Preparación y previsualización ' + documentType}</h1>}
    {error && <Toast message={error} onDismiss={() => setError('')} />}
    <FormalSubmission targetDocId={targetDocId} onDirtyChange={setFormalDirty} />
    <DocumentSections key={targetDocId + reload} targetDocId={targetDocId} disabled={busy} onDirtyChange={setSectionsDirty} onSaved={() => setReload(n => n + 1)} />
    {automaticPreview ? <>
      {busy && <p role="status">Preparando documento…</p>}
      {!busy && (!ready || (ready.ready && !selected && plan?.editable)) && <button type="button" className="secondary" onClick={() => setReload(n => n + 1)}>Reintentar previsualización</button>}
      {!busy && ready && !ready.ready && <section className="card" aria-label="Datos pendientes"><h2>Complete los datos pendientes</h2><ul>{ready.blockers.map((blocker, index) => <li key={index}><strong>{blocker.section}:</strong> {blocker.message}{onResolveSection && blockerSections[blocker.section] && <button type="button" className="link" disabled={formalDirty || sectionsDirty} onClick={() => void navigate(() => onResolveSection(blockerSections[blocker.section]))}>Ir a {blocker.section.toLowerCase()}</button>}</li>)}</ul></section>}
      {!busy && !error && plan && !plan.editable && !selected && <p role="status">Este documento de solo lectura todavía no tiene una previsualización conservada.</p>}
    </> : !ready ? <p role="status">Consultando preparación…</p> : <div className="card"><div className="section-heading"><span className="section-icon"><Icon name="document" /></span><div><span className="section-kicker">PREPARACIÓN DOCUMENTAL</span><h2>Revisión del contenido</h2></div></div>{ready.ready ? <p role="status">Contenido completo para previsualizar.</p> : <ul>{ready.blockers.map((b, i) => <li key={i}><strong>{b.section}:</strong> {b.message}</li>)}</ul>}{plan?.editable ? <button disabled={busy || formalDirty || sectionsDirty || !ready.ready} onClick={() => void generate()}>{busy ? 'Generando PDF…' : 'Generar previsualización'}</button> : <p className="field-help">Documento de solo lectura. Consulte su artefacto conservado y las rondas de revisión.</p>}</div>}
    {artifacts.length > (automaticPreview ? 1 : 0) && <label>Previsualizaciones guardadas<select value={selected?.id ?? ''} disabled={busy} onChange={e => { setSelected(artifacts.find(a => a.id === e.target.value) ?? null); setPage(0); setImageError(false) }}>{artifacts.map(a => <option value={a.id} key={a.id}>{new Date(a.createdAt).toLocaleString('es-EC')} · {a.pageCount} páginas · {a.current ? 'Contenido actual' : 'Artefacto conservado'}</option>)}</select></label>}
    {selected && <div className="card">
      <div className="review-summary">
        <span className={'status-badge ' + (selected.current ? 'active' : '')}>
          {selected.current ? 'Coincide con el contenido actual' : 'Artefacto conservado'}
        </span>
        <span className="muted">
          Espacios de firma: {selected.signatureSlots.map(s => s.label + ', página ' + s.pageNumber).join('; ')}.
        </span>
      </div>
      {!selected.current && <p className="field-help">Artefacto conservado de una preparación anterior. Consulte las rondas para ver los bytes firmados.</p>}
      {mode !== 'preview' && <SignaturePreparation key={targetDocId + selected.id} targetDocId={targetDocId} artifactId={selected.id} />}
      {mode !== 'preview' && <AuthorSigning key={'sign-' + targetDocId + selected.id} targetDocId={targetDocId} artifactId={selected.id} onDirtyChange={setSigningDirty} onBusyChange={setSigningBusy} onSigned={() => { setReload(n => n + 1) }} />}
      <DocumentPageNavigator key={selected.id} page={page} pageCount={selected.pageCount} disabled={busy || (!imageError && loadedImage !== selected.id + '-' + page)} onChange={target => { setPage(target); setImageError(false);setImageAttempt(0) }} references={[...selected.signatureSlots.map(slot => ({ page: slot.pageNumber - 1, label: slot.label })), ...selected.pages.filter((item, index) => item.kind === 'ANNEX' && selected.pages[index - 1]?.kind !== 'ANNEX').map(item => ({ page: item.number - 1, label: 'Anexos y evidencias' }))]} />
      {imageError && <><Toast message="No se pudo cargar la página. Pulse Reintentar página." /><button type="button" className="secondary" onClick={() => { setImageError(false);setLoadedImage('');setImageAttempt(n => n + 1) }}>Reintentar página</button></>}
      {!imageError && loadedImage !== selected.id + '-' + page && <p role="status" className="field-help">Cargando página {page + 1}…</p>}
      <div className="document-reading-desk">
        <div className="document-page-wrapper">
          <img className="document-page" key={selected.id + '-' + page + '-' + imageAttempt} width={Math.round(selected.pages[page].width * 1.5)} height={Math.round(selected.pages[page].height * 1.5)} src={'/api' + path + '/' + selected.id + '/pages/' + page + '?attempt=' + imageAttempt} alt={'Página ' + (page + 1) + ' del ' + label + ' ' + documentType} onLoad={() => setLoadedImage(selected.id + '-' + page)} onError={() => setImageError(true)} />
        </div>
      </div>
    </div>}
    {(onPrevious || onNext) && <StepPagination previous={onPrevious ? { label: mode === 'sign' ? 'Preparación y previsualización' : 'Anexos y opciones', action: () => void navigate(onPrevious) } : undefined} next={onNext ? { label: 'Firma y finalización', action: () => void navigate(onNext) } : undefined} disabled={busy || signingBusy} nextDisabled={sectionsDirty || formalDirty || signingDirty || automaticPreview && (!selected || !selected.current || !ready?.ready || !!error)} />}
  </section>
}
