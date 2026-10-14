import { confirmAction } from '../shared/confirm-action'
import { ActionForm } from '../shared/ActionForm'
import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'
import type { WorkPlan } from './types'
type Version = { id: string; formal_version: string; document_state: string; review_round: number; archived_at: string | null; version_reason: string; archive_reason: string }
export function DocumentLifecycle({ plan, disabled, onChange, onDirtyChange }: { plan: WorkPlan; disabled: boolean; onChange: (plan: WorkPlan) => void; onDirtyChange: (dirty: boolean) => void }) {
  const [versions, setVersions] = useState<Version[]>([]); const [reason, setReason] = useState(''); const [busy, setBusy] = useState(false); const [error, setError] = useState('')
  useEffect(() => { onDirtyChange(!!reason); return () => onDirtyChange(false) }, [reason, onDirtyChange])
  useEffect(() => { let active = true; api<Version[]>('/work-plans/' + plan.id + '/versions').then(v => { if (active) setVersions(v) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [plan.id, plan.rowVersion])
  async function change(action: 'archive' | 'versions') {
    if (!(await confirmAction(action === 'archive' ? '¿Archivar este documento como historial de solo lectura?' : '¿Crear una nueva versión formal y conservar la aprobada como historial?'))) return
    setBusy(true); setError('')
    try { const next = await api<WorkPlan>('/work-plans/' + plan.id + '/' + action, 'POST', { rowVersion: plan.rowVersion, reason }); setReason(''); onChange(next) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo actualizar.') } finally { setBusy(false) }
  }
  return <details className="card"><summary>Versiones formales y archivo {plan.archivedAt ? '· Archivado' : ''}</summary>{error && <Toast message={error} onDismiss={() => setError('')} />}<p className="field-help">Una devolución inicia una ronda y conserva la versión formal. Una actualización material de un aprobado crea una versión independiente. El archivo conserva los PDFs y la auditoría.</p><ul>{versions.map(v => <li key={v.id}>Versión {v.formal_version} · Ronda {v.review_round} · {v.document_state === 'APPROVED' ? 'Aprobado' : v.document_state === 'IN_REVIEW' ? 'En revisión' : 'Borrador'}{v.archived_at ? ' · Archivado' : ''}<small className="field-help">{v.version_reason || v.archive_reason}</small></li>)}</ul>{plan.canManage && !plan.archivedAt && plan.documentState !== 'IN_REVIEW' && <ActionForm label="Actualizar o archivar documento" dirty={!!reason} disabled={busy || disabled} onCancel={() => setReason('')}><fieldset disabled={busy || disabled}><label>Motivo de actualización o archivo<textarea maxLength={1000} value={reason} onChange={e => setReason(e.target.value)} rows={2} /></label><div className="review-pagination">{plan.documentState === 'APPROVED' && <button type="button" disabled={!reason.trim()} onClick={() => void change('versions')}>Crear nueva versión formal</button>}<button type="button" className="secondary" disabled={!reason.trim()} onClick={() => void change('archive')}>Archivar documento</button><button type="button" className="link" disabled={!reason} onClick={() => setReason('')}>Cancelar motivo</button></div></fieldset></ActionForm>}</details>
}
