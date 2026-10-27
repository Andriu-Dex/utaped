import { useCallback, useEffect, useState } from 'react'
import { api } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { Toast } from '../shared/Toast'
import { AuditViewer } from '../audit/AuditViewer'
import { ReviewInspector } from '../workflow/ReviewWorkspace'
import { ReviewHistory } from '../workflow/ReviewHistory'
import { ExecutionWorkspace } from './ExecutionWorkspace'
import { AuthorSigning, type SignedArtifact } from './AuthorSigning'
import { PlanPreview, type PreviewArtifact } from './PlanPreview'
import type { WorkPlan } from './types'
import './document-consultation.css'

type Round = { id: string; status: string; round_number: number; closed_at: string | null }
type StoredArtifact = PreviewArtifact & { createdAt: string }
type Author = { id: string; name: string; position: string }
type Consultation = { plan: WorkPlan; rounds: Round[]; authors: Author[]; artifact: PreviewArtifact | null; signed: SignedArtifact | null; actorSigned: boolean }
const modes = ['consultation', 'rounds', 'execution', 'history'] as const
type Mode = typeof modes[number]
export function DocumentConsultation({ initial, accountId, initialStep, onStepChange, onBack, onDirtyChange }: {
  initial: WorkPlan; accountId: string; initialStep?: string | null; onStepChange?: (step: string) => void
  onBack: () => void; onDirtyChange: (dirty: boolean) => void
}) {
  const [state, setState] = useState<Consultation | null>(null)
  const [error, setError] = useState(''), [loading, setLoading] = useState(true), [reload, setReload] = useState(0)
  const [dirty, setDirty] = useState(false), [signingBusy, setSigningBusy] = useState(false), [reviewed, setReviewed] = useState(false)
  const [mode, setMode] = useState<Mode>(() => modes.find(value => value === initialStep) ?? 'consultation')
  const [previousStep, setPreviousStep] = useState(initialStep)
  if (previousStep !== initialStep) { setPreviousStep(initialStep); setMode(modes.find(value => value === initialStep) ?? 'consultation') }
  useEffect(() => { onDirtyChange(dirty); return () => onDirtyChange(false) }, [dirty, onDirtyChange])
  useEffect(() => {
    let active = true
    Promise.resolve().then(async () => {
      if (!active) return
      setLoading(true); setError('')
      try {
        const base = '/work-plans/' + initial.id
        const [plan, rounds, authors, artifacts] = await Promise.all([
          api<WorkPlan>(base), api<Round[]>(base + '/reviews'), api<{ authors: Author[] }>(base + '/authors'), api<StoredArtifact[]>(base + '/artifacts'),
        ])
        const isCoauthor = authors.authors.some(author => author.id === accountId)
        const correction = plan.documentState === 'DRAFT' && isCoauthor && rounds[0]?.status === 'RETURNED'
        const candidates = correction ? artifacts.filter(value => value.current || rounds[0].closed_at && Date.parse(value.createdAt) >= Date.parse(rounds[0].closed_at)) : artifacts
        const artifact = candidates.find(value => value.current) ?? candidates[0] ?? null
        const needsAuthorView = !rounds.length || plan.documentState === 'DRAFT' && isCoauthor
        const signing = artifact && needsAuthorView ? await api<{ signed: SignedArtifact[]; actorSigned: boolean }>(base + '/artifacts/' + artifact.id + '/signing') : null
        if (active) { setState({ plan, rounds, authors: authors.authors, artifact, signed: signing?.signed[0] ?? null, actorSigned: signing?.actorSigned ?? false }); setReviewed(false) }
      } catch (e) { if (active) setError(e instanceof Error ? e.message : 'No se pudo consultar el documento.') }
      finally { if (active) setLoading(false) }
    })
    return () => { active = false }
  }, [initial.id, accountId, reload])
  const captureSigned = useCallback((signed: SignedArtifact | null, actorSigned: boolean) => {
    setState(current => current ? { ...current, signed, actorSigned } : current)
  }, [])
  function navigate(next: Mode) { if (dirty || signingBusy) return; setMode(next); onStepChange?.(next) }
  async function back() { if (signingBusy) return; if (!dirty || await confirmAction('Hay cambios sin guardar. ¿Salir y descartarlos?')) onBack() }
  const plan = state?.plan ?? initial
  const coauthor = state?.authors.some(author => author.id === accountId) ?? false
  const latest = state?.rounds[0]
  const toolbar = <div className="review-actions consultation-actions">
    <button type="button" className="secondary" disabled={dirty || signingBusy} onClick={() => setReload(value => value + 1)}>Actualizar consulta</button>
    <button type="button" className="secondary" disabled={dirty} onClick={() => navigate('rounds')}>Rondas y observaciones</button>
    {plan.documentType === 'T1' && plan.documentState === 'APPROVED' && <button type="button" className="secondary" disabled={dirty} onClick={() => navigate('execution')}>Ejecución y evidencias</button>}
    <button type="button" className="secondary" disabled={dirty} onClick={() => navigate('history')}>Historial del documento</button>
  </div>
  if (mode === 'rounds') return <ReviewHistory documentId={plan.id} title={plan.title} onBack={() => navigate('consultation')} onDirtyChange={setDirty} />
  if (mode === 'history') return <AuditViewer targetDocId={plan.id} onBack={() => navigate('consultation')} />
  if (mode === 'execution' && plan.documentType === 'T1' && plan.documentState === 'APPROVED') return <ExecutionWorkspace targetDocId={plan.id} onBack={() => navigate('consultation')} onDirtyChange={setDirty} />
  if (!loading && !error && latest && !(coauthor && plan.documentState === 'DRAFT')) return <>
    {toolbar}<ReviewInspector key={latest.id} id={latest.id} accountId={accountId} title={plan.title} backLabel="Volver a Documentos" onBack={onBack} onDirtyChange={setDirty} />
  </>
  return <section className="document-consultation">
    <button type="button" className="secondary" disabled={signingBusy} onClick={() => void back()}>Volver a Documentos</button>
    <span className="section-kicker review-kicker">CONSULTA DOCUMENTAL · {plan.documentType}</span><h1>{plan.title}</h1>
    {error && <Toast message={error} />}
    {loading ? <p role="status">Cargando consulta del documento…</p> : error ? <button type="button" className="secondary" onClick={() => setReload(value => value + 1)}>Reintentar consulta</button> : state && <>
      {toolbar}<div className="document-consultation-layout"><div>
        <section className="card" aria-label="Información del documento"><h2>Información del documento</h2>
          <dl><dt>Responsable principal</dt><dd>{plan.teacherName}</dd><dt>Grupo</dt><dd>{plan.groupName}</dd><dt>Período</dt><dd>{plan.periodName}</dd><dt>Estado</dt><dd>{plan.documentState === 'DRAFT' ? 'Borrador' : plan.documentState === 'IN_REVIEW' ? 'En revisión' : 'Aprobado'}</dd></dl>
          <p className="field-help">La edición corresponde al responsable principal.</p>
        </section>
        {coauthor && plan.documentState === 'DRAFT' && state.artifact && <>
          {!state.actorSigned && <label className="check consultation-confirmation"><input type="checkbox" checked={reviewed} disabled={dirty || signingBusy} onChange={event => setReviewed(event.target.checked)} />He revisado el documento completo y confirmo que está listo para firmar.</label>}
          <AuthorSigning key={state.artifact.id} targetDocId={plan.id} artifactId={state.artifact.id} disabled={!reviewed && !state.actorSigned} onDirtyChange={setDirty} onBusyChange={setSigningBusy} onPreviewChange={captureSigned} onSigned={() => setReload(value => value + 1)} onFinish={() => void back()} />
        </>}
      </div>
      {state.artifact ? <PlanPreview plan={plan} draft={plan} step="signing" focus="JUSTIFICATION" dirty={dirty} textDirty={false} prepared={{ artifact: state.artifact, signed: state.signed, busy: false, error: '' }} /> : <section className="card empty-state"><h2>Documento aún no preparado</h2><p>El responsable principal todavía no ha preparado un PDF para consultar.</p></section>}
      </div>
    </>}
  </section>
}
