import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'

type Change = { id: string; display_name: string; membership_role?: string; position_label?: string; active?: boolean; password_reset_authorized?: boolean; created_at: string; changed_by_name: string | null; origin: string }
type History = { memberships: Change[]; principals: Change[] }
export function ScopeHistory({ scopeId, revision = 0 }: { scopeId: string; revision?: number }) {
  const [data, setData] = useState<History | null>(null), [error, setError] = useState('')
  useEffect(() => { let active = true; api<History>('/organization/scopes/' + scopeId + '/history').then(h => { if (active) setData(h) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [scopeId, revision])
  return <details className="scope-history"><summary>Historial de cargos y responsables</summary><p className="field-help">Los registros iniciales muestran el estado al incorporar el historial; no reconstruyen fechas anteriores.</p>{error && <Toast message={error} onDismiss={() => setError('')} />}{!data ? <p role="status">Cargando historial…</p> : <>{(['memberships', 'principals'] as const).map(kind => <div key={kind}><h3>{kind === 'memberships' ? 'Pertenencias y cargos' : 'Designaciones de responsable principal'}</h3><ol className="scope-timeline">{data[kind].map(h => <li key={h.id}><strong>{h.display_name}</strong><span>{kind === 'memberships' ? (h.membership_role === 'COORDINATOR' ? 'Coordinador' : 'Miembro') + (h.position_label ? ' · ' + h.position_label : '') + (h.active ? ' · Activo' : ' · Retirado') : 'Responsable principal'}{h.password_reset_authorized && ' · Recuperación autorizada'}</span><small>{new Date(h.created_at).toLocaleString('es-EC')} · {h.origin === 'BASELINE' ? 'Registro inicial' : h.changed_by_name ?? 'Sin nombre'}</small></li>)}</ol>{!data[kind].length && <p className="muted">Sin registros.</p>}</div>)}</>}</details>
}
