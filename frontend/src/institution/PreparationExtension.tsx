import { useEffect, useState, type FormEvent } from 'react'
import { ActionForm } from '../shared/ActionForm'
import { api } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { Icon } from '../shared/Icon'

type Change = { id: string; old_deadline: string; new_deadline: string; reason: string; created_at: string; authorized_by_name: string }
type State = { periodPreparationEndsOn: string; effectiveEndsOn: string; maxEndsOn: string; maxBusinessDays: number; rowVersion: number; canGrant: boolean; history: Change[] }
const day = (value: string) => value.slice(0, 10).split('-').reverse().join('/')

export function PreparationExtension({ scopeId, onDirtyChange, onNotice, onError }: { scopeId: string; onDirtyChange: (dirty: boolean) => void; onNotice: (message: string) => void; onError: (message: string) => void }) {
  const [state, setState] = useState<State | null>(null), [busy, setBusy] = useState(false), [dirty, setDirty] = useState(false), [reload, setReload] = useState(0)
  const path = '/organization/scopes/' + scopeId + '/preparation-extension'
  useEffect(() => { onDirtyChange(dirty) }, [dirty, onDirtyChange])
  useEffect(() => { let active = true; api<State>(path).then(s => { if (active) setState(s) }).catch(e => { if (active) onError(e.message) }); return () => { active = false } }, [path, reload, onError])
  async function grant(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = event.currentTarget, data = Object.fromEntries(new FormData(form))
    if (!state || !(await confirmAction('¿Reabrir la elaboración de este ámbito hasta el ' + day(String(data.endsOn)) + '?'))) return
    setBusy(true)
    try { setState(await api<State>(path, 'PUT', { ...data, rowVersion: state.rowVersion })); form.reset(); setDirty(false); onNotice('Plazo de elaboración ampliado.') }
    catch (e) { onError(e instanceof Error ? e.message : 'No se pudo ampliar el plazo.'); setReload(r => r + 1) } finally { setBusy(false) }
  }
  if (!state) return <p role="status">Cargando plazo de elaboración…</p>
  const extended = state.effectiveEndsOn !== state.periodPreparationEndsOn
  return <section className="contextual-recovery"><div className="section-heading"><span className="section-icon"><Icon name="calendar" /></span><div><span className="section-kicker">PLAZO DE ELABORACIÓN</span><h2>Elaboración del Plan</h2></div></div>
    <p>Cierre del período: <strong>{day(state.periodPreparationEndsOn)}</strong>{extended && <> · Ampliado hasta: <strong>{day(state.effectiveEndsOn)}</strong></>}</p>
    {state.canGrant && state.effectiveEndsOn < state.maxEndsOn && <ActionForm label="Reabrir elaboración" dirty={dirty} disabled={busy} onCancel={() => setDirty(false)}><form onChange={() => setDirty(true)} onSubmit={grant}><fieldset disabled={busy}><div className="grid"><label>Nuevo plazo<input type="date" name="endsOn" required min={state.effectiveEndsOn} max={state.maxEndsOn} /></label><label>Motivo<input name="reason" required maxLength={1000} /></label></div><p className="field-help">Máximo {state.maxBusinessDays} días hábiles acumulados desde el cierre original (hasta el {day(state.maxEndsOn)}); no cuentan fines de semana ni feriados registrados.</p><button>Registrar ampliación</button></fieldset></form></ActionForm>}
    {state.history.length > 0 && <details className="scope-history"><summary>Historial de ampliaciones</summary><ol className="scope-timeline">{state.history.map(h => <li key={h.id}><strong>{day(h.old_deadline)} → {day(h.new_deadline)}</strong><span>{h.reason}</span><small>{h.authorized_by_name} · {new Date(h.created_at).toLocaleString('es-EC')}</small></li>)}</ol></details>}
  </section>
}
