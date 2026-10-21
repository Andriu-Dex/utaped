import { useEffect, useState, type FormEvent } from 'react'
import { ActionForm } from '../shared/ActionForm'
import { api } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { Icon } from '../shared/Icon'

type Member = { user_id: string; display_name: string; membership_role: string; position_label: string; active: boolean; row_version: number }
type Candidate = { id: string; display_name: string }

/** Coordinators add or retire ordinary members of their own scope; other people only see the list. */
export function ScopeMembers({ scopeId, principalId, onDirtyChange, onNotice, onError }: { scopeId: string; principalId: string | null; onDirtyChange: (dirty: boolean) => void; onNotice: (message: string) => void; onError: (message: string) => void }) {
  const [members, setMembers] = useState<Member[]>([]), [canManage, setCanManage] = useState(false), [query, setQuery] = useState(''), [candidates, setCandidates] = useState<Candidate[]>([])
  const [busy, setBusy] = useState(false), [dirty, setDirty] = useState(false), [reload, setReload] = useState(0)
  const base = '/organization/scopes/' + scopeId
  useEffect(() => { onDirtyChange(dirty) }, [dirty, onDirtyChange])
  useEffect(() => {
    let active = true
    api<Member[]>(base + '/members').then(m => { if (active) setMembers(m) }).catch(e => { if (active) onError(e.message) })
    api<Candidate[]>(base + '/candidates').then(() => { if (active) setCanManage(true) }).catch(() => { if (active) setCanManage(false) })
    return () => { active = false }
  }, [base, reload, onError])
  async function search() {
    if (query.trim().length < 2) { onError('Escriba al menos dos letras del nombre para buscar.'); return }
    try { setCandidates(await api<Candidate[]>(base + '/candidates?' + new URLSearchParams({ query: query.trim() }))) } catch (e) { onError(e instanceof Error ? e.message : 'No se pudo buscar.') }
  }
  async function save(userId: string, positionLabel: string, active: boolean, rowVersion: number, message: string) {
    setBusy(true)
    try { await api(base + '/members', 'PUT', { userId, membershipRole: 'MEMBER', positionLabel, active, rowVersion }); setDirty(false); onNotice(message); setReload(r => r + 1); return true }
    catch (e) { onError(e instanceof Error ? e.message : 'No se pudo actualizar la pertenencia.') ; return false } finally { setBusy(false) }
  }
  function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = event.currentTarget, data = new FormData(form), userId = String(data.get('userId'))
    const existing = members.find(m => m.user_id === userId)
    void save(userId, String(data.get('positionLabel')).trim(), true, existing?.row_version ?? 0, 'Integrante agregado al ámbito.').then(ok => { if (ok) { form.reset(); setCandidates([]); setQuery('') } })
  }
  const activeMembers = members.filter(m => m.active)
  return <section className="contextual-recovery"><div className="section-heading"><span className="section-icon"><Icon name="users" /></span><div><span className="section-kicker">INTEGRANTES</span><h2>Integrantes del ámbito</h2></div></div>
    <ul className="scope-member-list">{activeMembers.map(m => <li key={m.user_id}><span><strong>{m.display_name}</strong><small>{m.membership_role === 'COORDINATOR' ? 'Coordinación' : m.user_id === principalId ? 'Responsable principal' : 'Integrante'}{m.position_label ? ' · ' + m.position_label : ''}</small></span>
      {canManage && m.membership_role === 'MEMBER' && m.user_id !== principalId && <button className="link" disabled={busy} onClick={async () => { if (await confirmAction('¿Retirar a ' + m.display_name + ' del ámbito? Conserva su historial y los documentos ya firmados.')) void save(m.user_id, m.position_label ?? '', false, m.row_version, 'Integrante retirado del ámbito.') }}>Retirar</button>}</li>)}</ul>
    {canManage && <ActionForm label="Agregar integrante" disabled={busy} dirty={dirty} onCancel={() => { setDirty(false); setCandidates([]); setQuery('') }}><div className="grid"><label>Buscar docente por nombre<input value={query} maxLength={100} onChange={e => { setQuery(e.currentTarget.value); setCandidates([]); setDirty(true) }} onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); void search() } }} /></label><button type="button" className="secondary" disabled={busy} onClick={() => void search()}><Icon name="search" size={16} />Buscar</button></div><form onChange={() => setDirty(true)} onSubmit={add}><fieldset disabled={busy}><div className="grid"><label>Docente<select name="userId" required><option value="">{candidates.length ? 'Seleccione una persona' : 'Busque primero por nombre'}</option>{candidates.map(c => <option key={c.id} value={c.id}>{c.display_name}</option>)}</select></label><label>Cargo en el ámbito<input name="positionLabel" maxLength={160} placeholder="Docente integrante" /></label></div><p className="field-help">La coordinación agrega o retira integrantes. Designar coordinación o responsable principal sigue a cargo de administración.</p><button>Agregar</button></fieldset></form></ActionForm>}
  </section>
}
