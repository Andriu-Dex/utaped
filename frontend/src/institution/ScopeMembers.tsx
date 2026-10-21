import { useEffect, useState, type FormEvent } from 'react'
import { ActionForm } from '../shared/ActionForm'
import { api } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { Icon } from '../shared/Icon'

type Member = { user_id: string; display_name: string; membership_role: string; position_label: string; active: boolean; row_version: number }
type Candidate = { id: string; display_name: string; username: string | null }

/** Coordinators add or retire ordinary members of their own scope; other people only see the list. */
export function ScopeMembers({ scopeId, principalName, onDirtyChange, onNotice, onError }: { scopeId: string; principalName: string | null; onDirtyChange: (dirty: boolean) => void; onNotice: (message: string) => void; onError: (message: string) => void }) {
  const [members, setMembers] = useState<Member[]>([]), [candidates, setCandidates] = useState<Candidate[] | null>(null)
  const [busy, setBusy] = useState(false), [dirty, setDirty] = useState(false), [reload, setReload] = useState(0)
  const base = '/organization/scopes/' + scopeId
  useEffect(() => { onDirtyChange(dirty) }, [dirty, onDirtyChange])
  useEffect(() => {
    let active = true
    api<Member[]>(base + '/members').then(m => { if (active) setMembers(m) }).catch(e => { if (active) onError(e.message) })
    api<Candidate[]>(base + '/candidates').then(c => { if (active) setCandidates(c) }).catch(() => { if (active) setCandidates(null) })
    return () => { active = false }
  }, [base, reload, onError])
  async function save(userId: string, positionLabel: string, active: boolean, rowVersion: number, message: string) {
    setBusy(true)
    try { await api(base + '/members', 'PUT', { userId, membershipRole: 'MEMBER', positionLabel, active, rowVersion }); setDirty(false); onNotice(message); setReload(r => r + 1) }
    catch (e) { onError(e instanceof Error ? e.message : 'No se pudo actualizar la pertenencia.') } finally { setBusy(false) }
  }
  function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = event.currentTarget, data = new FormData(form), userId = String(data.get('userId'))
    const existing = members.find(m => m.user_id === userId)
    void save(userId, String(data.get('positionLabel')).trim(), true, existing?.row_version ?? 0, 'Integrante agregado al ámbito.').then(() => form.reset())
  }
  const activeMembers = members.filter(m => m.active), current = new Set(activeMembers.map(m => m.user_id))
  return <section className="contextual-recovery"><div className="section-heading"><span className="section-icon"><Icon name="users" /></span><div><span className="section-kicker">INTEGRANTES</span><h2>Integrantes del ámbito</h2></div></div>
    <ul className="scope-member-list">{activeMembers.map(m => <li key={m.user_id}><span><strong>{m.display_name}</strong><small>{m.membership_role === 'COORDINATOR' ? 'Coordinación' : m.display_name === principalName ? 'Responsable principal' : 'Integrante'}{m.position_label ? ' · ' + m.position_label : ''}</small></span>
      {candidates && m.membership_role === 'MEMBER' && m.display_name !== principalName && <button className="link" disabled={busy} onClick={async () => { if (await confirmAction('¿Retirar a ' + m.display_name + ' del ámbito? Conserva su historial y los documentos ya firmados.')) void save(m.user_id, m.position_label ?? '', false, m.row_version, 'Integrante retirado del ámbito.') }}>Retirar</button>}</li>)}</ul>
    {candidates && <ActionForm label="Agregar integrante" disabled={busy} dirty={dirty} onCancel={() => setDirty(false)}><form onChange={() => setDirty(true)} onSubmit={add}><fieldset disabled={busy}><div className="grid"><label>Docente<select name="userId" required><option value="">Seleccione una persona</option>{candidates.filter(c => !current.has(c.id)).map(c => <option key={c.id} value={c.id}>{c.display_name}{c.username ? ' · ' + c.username : ''}</option>)}</select></label><label>Cargo en el ámbito<input name="positionLabel" maxLength={160} placeholder="Docente integrante" /></label></div><p className="field-help">La coordinación agrega o retira integrantes. Designar coordinación o responsable principal sigue a cargo de administración.</p><button>Agregar</button></fieldset></form></ActionForm>}
  </section>
}
