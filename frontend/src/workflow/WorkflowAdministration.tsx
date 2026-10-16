import { confirmAction } from '../shared/confirm-action'
import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { useEffect, useState } from 'react'
import { api, type Group, type User } from '../shared/api'
import { Icon } from '../shared/Icon'
import './workflow-admin.css'
interface Stage { id: string; label: string; action: string; recipientKind: string; assigneeIds: string[]; groupRole: string | null; recipientLabel: string; requiresSignature: boolean }
interface Definition { name: string; stages: Stage[] }
interface Revision { id: string; revisionNumber: number; definition: Definition; configuredBy: string; configuredAt: string }
interface State { rowVersion: number; groupActive: boolean; draft: Definition; current: Revision | null; revisions: Revision[]; blockers: string[] }
export function WorkflowAdministration({ groups, users, suggestedQuery, onDirtyChange }: { groups: Group[]; users: User[]; suggestedQuery: string; onDirtyChange: (dirty: boolean) => void }) {
  const [group, setGroup] = useState(''); const [type, setType] = useState('T1'); const [state, setState] = useState<State | null>(null)
  const [draft, setDraft] = useState<Definition>({ name: '', stages: [] }); const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const [revision, setRevision] = useState<Revision | null>(null)
  const [search, setSearch] = useState(''); const [page, setPage] = useState(0); const [options, setOptions] = useState<{ items: User[]; total: number }>({ items: [], total: 0 })
  const [cachedUsers, setKnown] = useState<Record<string, User>>({})
  const known = { ...cachedUsers, ...Object.fromEntries(users.map(u => [u.id, u])) }
  const [previousSuggestion, setPreviousSuggestion] = useState(suggestedQuery)
  if (previousSuggestion !== suggestedQuery) { setPreviousSuggestion(suggestedQuery); setSearch(suggestedQuery); setPage(0) }
  useEffect(() => { let active = true; if (group) api<{ items: User[]; total: number }>('/admin/users/directory?' + new URLSearchParams({ query: search, page: String(page), size: '20', active: 'true' })).then(data => { if (active) { setOptions(data); setKnown(previous => ({ ...previous, ...Object.fromEntries(data.items.map(u => [u.id, u])) })) } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [search, page, group])
  const path = '/admin/groups/' + group + '/workflows/' + type
  const dirty = state !== null && JSON.stringify(draft) !== JSON.stringify(state.draft)
  useEffect(() => { onDirtyChange(dirty); const prevent = (e: BeforeUnloadEvent) => { if (dirty) { e.preventDefault(); e.returnValue = '' } }; window.addEventListener('beforeunload', prevent); return () => { window.removeEventListener('beforeunload', prevent); onDirtyChange(false) } }, [dirty, onDirtyChange])
  useEffect(() => { let active = true; if (group) api<State>(path).then(s => { if (active) { setState(s); setDraft(s.draft); setRevision(null) } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [group, path])
  async function choose(nextGroup: string, nextType: string) { if (!dirty || (await confirmAction('¿Descartar los cambios del flujo sin guardar?'))) { setGroup(nextGroup); setType(nextType); setState(null); setError(''); setNotice(''); setRevision(null) } }
  function change(id: string, values: Partial<Stage>) { setDraft(d => ({ ...d, stages: d.stages.map(s => s.id === id ? { ...s, ...values } : s) })); setNotice('') }
  function move(index: number, delta: number) { setDraft(d => { const stages = [...d.stages]; [stages[index], stages[index + delta]] = [stages[index + delta], stages[index]]; return { ...d, stages } }) }
  async function operation(suffix: string, method: string, body?: unknown) {
    setBusy(true); setError(''); setNotice('')
    try { const saved = await api<State>(path + suffix, method, body); setState(saved); setDraft(saved.draft); setNotice('Configuración guardada.'); setRevision(null) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar el flujo.') } finally { setBusy(false) }
  }
  return <section className="card">
    <div className="section-heading">
      <div className="section-heading-icon" aria-hidden="true"><Icon name="settings" size={24} /></div>
      <div>
        <h3>Plantillas de flujo por grupo</h3>
        <p>Prepare revisiones reutilizables para T1 y T2. En Períodos, asigne una revisión a cada ámbito y active sus flujos. Cambiar esta plantilla no modifica las asignaciones ni los PDFs históricos.</p>
      </div>
    </div>
    <div className="filter-toolbar" style={{ margin: '16px 0' }}>
      <div className="grid">
        <label>Grupo del flujo<select value={group} disabled={busy} onChange={e => choose(e.target.value, type)}><option value="">Seleccione</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>
        <label>Tipo documental del flujo<select value={type} disabled={busy} onChange={e => choose(group, e.target.value)}><option value="T1">Plan T1</option><option value="T2">Informe T2</option></select></label>
      </div>
    </div>
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    {group && !state && <p role="status">Cargando flujo…</p>}
    {state && <>
      <div className="workflow-status-card">
        <p className="workflow-status-badge">{state.current ? 'Revisión de configuración vigente: ' + state.current.revisionNumber : 'Sin configuración vigente.'}</p>
        {!state.groupActive && <p className="muted" style={{ margin: 0 }}>Grupo inactivo: configuración de solo lectura.</p>}
      </div>
      <div className="filter-toolbar" style={{ margin: '16px 0' }}>
        <label>Buscar participantes del flujo<input maxLength={200} value={search} onChange={e => { setSearch(e.target.value); setPage(0) }} /></label>
        <p style={{ margin: '8px 0 0' }}>{options.total} participantes activos coinciden.</p>
      </div>
      <div className="pagination">
        <button className="secondary" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Participantes anteriores</button>
        <button className="secondary" disabled={(page + 1) * 20 >= options.total} onClick={() => setPage(p => p + 1)}>Participantes siguientes</button>
      </div>
      <ActionForm label="Editar flujo" dirty={dirty} disabled={busy || !state.groupActive} onCancel={() => setDraft(state.draft)}>
        <form onSubmit={e => { e.preventDefault(); void operation('', 'PUT', { rowVersion: state.rowVersion, definition: draft }) }}>
          <fieldset disabled={busy || !state.groupActive}>
            <label>Nombre del flujo<input required maxLength={160} value={draft.name} onChange={e => setDraft(d => ({ ...d, name: e.target.value }))} /></label>
            {draft.stages.map((s, index) => <section className="card workflow-stage-card" key={s.id} aria-label={'Etapa ' + (index + 1)}>
              <div className="activity-card-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span className="activity-index" aria-hidden="true">#{index + 1}</span>
                  <h4 style={{ margin: 0 }}>Etapa {index + 1}</h4>
                  <span className={`status-badge ${s.action === 'APPROVE' ? 'active' : 'pending'}`}>
                    {s.action === 'APPROVE' ? 'Aprobación' : s.action === 'VALIDATE' ? 'Validación' : 'Revisión'}
                  </span>
                </div>
                <div style={{ display: 'flex', gap: '4px' }}>
                  <button className="secondary" type="button" title="Subir etapa" aria-label={'Subir etapa ' + (index + 1)} disabled={index === 0} onClick={() => move(index, -1)}>↑</button>
                  <button className="secondary" type="button" title="Bajar etapa" aria-label={'Bajar etapa ' + (index + 1)} disabled={index + 1 === draft.stages.length} onClick={() => move(index, 1)}>↓</button>
                  <button className="danger" type="button" title="Quitar etapa del borrador" aria-label={'Quitar etapa ' + (index + 1)} onClick={() => setDraft(d => ({ ...d, stages: d.stages.filter(a => a.id !== s.id) }))}>×</button>
                </div>
              </div>
              <div className="grid">
                <label>Nombre de la etapa<input required maxLength={160} value={s.label} onChange={e => change(s.id, { label: e.target.value })} /></label>
                <label>Acción de la etapa<select value={s.action} onChange={e => change(s.id, { action: e.target.value })}><option value="REVIEW">Revisión</option><option value="VALIDATE">Validación</option><option value="APPROVE">Aprobación</option></select></label>
                <label>Destinatario de la etapa<select value={s.recipientKind} onChange={e => change(s.id, { recipientKind: e.target.value, assigneeIds: [], groupRole: e.target.value === 'GROUP_ROLE' ? 'COORDINATOR' : null, recipientLabel: '' })}><option value="PERSON">Personas</option><option value="GROUP_ROLE">Rol de pertenencia del grupo</option><option value="COLLEGIATE">Órgano colegiado</option></select></label>
                {s.recipientKind === 'COLLEGIATE' && <label>Nombre del órgano colegiado<input required maxLength={160} value={s.recipientLabel} onChange={e => change(s.id, { recipientLabel: e.target.value })} /></label>}
                {s.recipientKind === 'GROUP_ROLE' && <label>Rol destinatario<select value={s.groupRole ?? ''} onChange={e => change(s.id, { groupRole: e.target.value })}><option value="MEMBER">Miembros</option><option value="COORDINATOR">Coordinadores</option></select></label>}
              </div>
              <label className="check"><input type="checkbox" checked={s.requiresSignature} onChange={e => change(s.id, { requiresSignature: e.target.checked, assigneeIds: s.recipientKind === 'COLLEGIATE' ? [] : s.assigneeIds })} />Requiere firma personal</label>
              {(s.recipientKind === 'PERSON' || s.recipientKind === 'COLLEGIATE' && s.requiresSignature) && <fieldset className="choice-group">
                <legend>Participantes de la etapa</legend>
                <p>Seleccione personas; varias personas representan participantes de la misma etapa. El avance efectivo depende del flujo institucional.</p>
                <div className="choice-chip-grid">
                  {options.items.map(u => <label className="check choice-chip" key={u.id}><input type="checkbox" checked={s.assigneeIds.includes(u.id)} onChange={e => change(s.id, { assigneeIds: e.target.checked ? [...s.assigneeIds, u.id] : s.assigneeIds.filter(id => id !== u.id) })} />{u.display_name}</label>)}
                  {s.assigneeIds.filter(id => !options.items.some(u => u.id === id)).map(id => <label className="check choice-chip" key={id}><input type="checkbox" checked onChange={() => change(s.id, { assigneeIds: s.assigneeIds.filter(value => value !== id) })} />{known[id]?.display_name ?? id}{known[id]?.active === false && ' · Inactivo'}</label>)}
                </div>
              </fieldset>}
            </section>)}
            <div style={{ display: 'flex', gap: '8px', marginTop: '16px' }}>
              <button type="button" className="secondary" disabled={draft.stages.length >= 20} onClick={() => setDraft(d => ({ ...d, stages: [...d.stages, { id: crypto.randomUUID(), label: '', action: 'REVIEW', recipientKind: 'PERSON', assigneeIds: [], groupRole: null, recipientLabel: '', requiresSignature: false }] }))}>Agregar etapa</button>
              <button disabled={!dirty}>Guardar borrador del flujo</button>
            </div>
          </fieldset>
        </form>
      </ActionForm>
      {state.blockers.length > 0 && <div className="workflow-blockers-alert" role="alert"><strong>Bloqueos para activar la configuración:</strong><ul>{state.blockers.map((b, i) => <li key={i}>{b}</li>)}</ul></div>}
      <div className="workflow-actions-bar">
        <button disabled={busy || dirty || !state.groupActive || state.blockers.length > 0} onClick={() => void operation('/revisions', 'POST', { rowVersion: state.rowVersion })}>Guardar como configuración vigente</button>
        <button className="secondary" disabled={busy || dirty || !state.groupActive || !state.current} onClick={async () => { if ((await confirmAction('¿Deshabilitar la configuración vigente conservando su historial?'))) void operation('/disable', 'POST', { rowVersion: state.rowVersion }) }}>Deshabilitar configuración</button>
        <button className="secondary" disabled={busy} onClick={async () => { if (!dirty || (await confirmAction('¿Descartar los cambios locales y recargar?'))) void operation('', 'GET') }}>Recargar flujo</button>
      </div>
      {state.revisions.length > 0 && <label>Historial de configuración<select value={revision?.id ?? ''} onChange={e => setRevision(state.revisions.find(r => r.id === e.target.value) ?? null)}><option value="">Seleccione revisión</option>{state.revisions.map(r => <option key={r.id} value={r.id}>{r.revisionNumber} · {r.definition.name}</option>)}</select></label>}
      {revision && <article className="card workflow-revision-card"><h4>Revisión {revision.revisionNumber}: {revision.definition.name}</h4><p className="muted">{revision.configuredBy} · {new Date(revision.configuredAt).toLocaleString('es-EC')}</p><ol className="scope-timeline">{revision.definition.stages.map(s => <li key={s.id}><strong>{s.label}</strong> · {s.recipientLabel || s.groupRole || s.assigneeIds.map(id => known[id]?.display_name ?? id).join(', ')} · {s.requiresSignature ? 'Firma personal requerida' : 'Sin firma personal'}</li>)}</ol></article>}
    </>}
  </section>
}
