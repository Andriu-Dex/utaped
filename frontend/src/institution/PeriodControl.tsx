import { confirmAction } from '../shared/confirm-action'
import { ActionForm } from '../shared/ActionForm'
import { useEffect, useState } from 'react'
import { api, download, type Period } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import './period-control.css'

type Scope = { id: string; period_id: string; group_id: string; group_name: string; career_name: string | null; principal_name: string | null; active: boolean }
type Revision = { id: string; revisionNumber: number; definition: { name: string } }
type Assignment = { document_type: string; revision_id: string; row_version: number; revision_number: number }
type Control = Period & { archive_sha256: string | null; archive_size: number | null; closure_reason: string | null; custody_downloaded_at: string | null; purged_at: string | null; purge_reason: string | null; purge_available_at: string | null; purge_ready: boolean; purge_grace_days: number; states: { document_type: string; document_state: string; count: number }[] }

export function PeriodControl({ periods, onChange, onDirtyChange }: { periods: Period[]; onChange: () => Promise<void>; onDirtyChange: (dirty: boolean) => void }) {
  const [selected, setSelected] = useState('')
  const [control, setControl] = useState<Control | null>(null)
  const [scopes, setScopes] = useState<Scope[]>([])
  const [scopeId, setScopeId] = useState('')
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [revisions, setRevisions] = useState<Record<string, Revision[]>>({})
  const [chosen, setChosen] = useState<Record<string, string>>({})
  const [reason, setReason] = useState('')
  const [purgeReason, setPurgeReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [reload, setReload] = useState(0)
  const scope = scopes.find(s => s.id === scopeId)
  const dirty = Boolean(reason) || Boolean(purgeReason) || ['T1', 'T2'].some(type => (chosen[type] ?? '') !== (assignments.find(a => a.document_type === type)?.revision_id ?? ''))
  useEffect(() => {
    onDirtyChange(dirty)
    const prevent = (e: BeforeUnloadEvent) => { if (dirty) { e.preventDefault(); e.returnValue = '' } }
    window.addEventListener('beforeunload', prevent)
    return () => { window.removeEventListener('beforeunload', prevent); onDirtyChange(false) }
  }, [dirty, onDirtyChange])
  useEffect(() => {
    let active = true
    if (selected) Promise.all([api<Control>('/admin/periods/' + selected + '/control'), api<Scope[]>('/organization/scopes')])
      .then(([data, list]) => { if (active) { setControl(data); setScopes(list.filter(s => s.period_id === selected)) } })
      .catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [selected, reload])
  useEffect(() => {
    let active = true
    if (scope) Promise.all([api<Assignment[]>('/admin/scopes/' + scope.id + '/workflows'), ...['T1', 'T2'].map(type => api<{ revisions: Revision[] }>('/admin/groups/' + scope.group_id + '/workflows/' + type))])
      .then(([data, t1, t2]) => {
        if (active) { const a = data as Assignment[]; setAssignments(a); setChosen(Object.fromEntries(a.map(item => [item.document_type, item.revision_id]))); setRevisions({ T1: (t1 as { revisions: Revision[] }).revisions, T2: (t2 as { revisions: Revision[] }).revisions }) }
      }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [scope, reload])
  async function discard() { return !dirty || (await confirmAction('¿Descartar los cambios del control de período sin guardar?')) }
  async function operation(path: string, method: string, body: unknown, message: string) {
    setBusy(true); setError(''); setNotice('')
    try { await api(path, method, body); if (path.endsWith('/close')) setReason(''); if (path.endsWith('/purge')) setPurgeReason(''); setNotice(message); setReload(r => r + 1); await onChange() }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo completar la operación.') }
    finally { setBusy(false) }
  }
  const locked = control?.workflow_locked_at != null
  const closed = control?.status === 'CLOSED'
  const total = control?.states.reduce((sum, state) => sum + Number(state.count), 0) ?? 0
  const pending = control?.states.filter(s => s.document_state !== 'APPROVED').reduce((sum, s) => sum + Number(s.count), 0) ?? 0
  return <section className="period-control">
    <div className="section-heading"><span className="section-icon amber"><Icon name="calendar" /></span><div><span className="section-kicker">CONTINUIDAD INSTITUCIONAL</span><h2>Flujos y cierre del período</h2></div></div>
    <p className="muted">Asigne flujos por carrera y comisión, actívelos para fijar sus participantes y conserve el historial al cerrar. El cierre nunca equivale a aprobar documentos.</p>
    {error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    <label className="period-selector">Período a administrar<select value={selected} disabled={busy} onChange={async e => { const nextValue = e.currentTarget.value; if ((await discard())) { setSelected(nextValue); setControl(null); setScopeId(''); setAssignments([]); setChosen({}); setReason(''); setError(''); setNotice('') } }}><option value="">Seleccione un período</option>{periods.map(p => <option key={p.id} value={p.id}>{p.name} · {p.status === 'CLOSED' ? 'Cerrado' : p.is_current ? 'Actual' : 'Activo'}</option>)}</select></label>
    {!selected ? <div className="card empty-state"><Icon name="calendar" size={32} /><p>Seleccione un período para ver su organización, los flujos asignados y el estado de sus documentos.</p></div> : !control ? <p role="status">Consultando período…</p> : <>
      <div className="period-overview"><div className="card"><span className="section-kicker">ESTADO DEL CICLO</span><h3>{control.name}</h3><p>{control.starts_on} — {control.ends_on}</p><span className={'status-badge ' + (closed ? '' : 'active')}>{closed ? 'Cerrado · Solo lectura' : 'Activo'}</span><p className="field-help">{closed ? (control.purged_at ? 'La documentación del período fue eliminada; se conservan el inventario y la auditoría.' : 'El archivo conserva todas las versiones y PDFs hasta que administración elimine la documentación tras descargar la copia de custodia. No hay reapertura.') : locked ? 'Flujos activados: sus revisiones y participantes están fijados para este período.' : 'Configuración de flujos pendiente de activación. Los documentos aún no pueden firmarse.'}</p></div><div className="card period-totals"><div><strong>{total}</strong><span>Documentos conservados</span></div><div><strong>{pending}</strong><span>Sin aprobación final</span></div></div></div>
      <section className="card"><div className="section-heading"><span className="section-icon"><Icon name="users" /></span><div><span className="section-kicker">ASIGNACIÓN POR ÁMBITO</span><h3>Flujos de Plan e Informe</h3></div></div>
        <label>Ámbito del flujo del período<select value={scopeId} disabled={busy} onChange={async e => { const nextValue = e.currentTarget.value; if ((await discard())) { setScopeId(nextValue); setAssignments([]); setChosen({}); setRevisions({}); setReason('') } }}><option value="">Seleccione comisión y carrera</option>{scopes.map(s => <option key={s.id} value={s.id}>{s.group_name} · {s.career_name ?? 'Facultad'}{!s.active ? ' · Inactivo' : ''}</option>)}</select></label>
        {scope && <><p className="field-help">Principal: {scope.principal_name ?? 'Sin designar'}. Publique las revisiones en Plantillas de flujo antes de asignarlas aquí.</p><ActionForm key={scopeId} label="Configurar flujos del ámbito" disabled={busy} dirty={['T1', 'T2'].some(type => (chosen[type] ?? '') !== (assignments.find(a => a.document_type === type)?.revision_id ?? ''))} onCancel={() => setChosen(Object.fromEntries(assignments.map(a => [a.document_type, a.revision_id])))}><div className="period-assignments">{['T1', 'T2'].map(type => { const current = assignments.find(a => a.document_type === type); return <article className="period-assignment" key={type}><span className="section-kicker">{type === 'T1' ? 'PLAN DE TRABAJO' : 'INFORME FINAL'}</span><h4>{type}</h4><label>Revisión asignada al {type}<select aria-label={'Revisión asignada al ' + type} value={chosen[type] ?? ''} disabled={busy || closed || locked || !scope.active} onChange={e => setChosen(c => ({ ...c, [type]: e.target.value }))}><option value="">Seleccione revisión publicada</option>{(revisions[type] ?? []).map(r => <option value={r.id} key={r.id}>{r.revisionNumber} · {r.definition.name}</option>)}</select></label><p className="field-help">{current ? 'Revisión ' + current.revision_number + ' asignada' : 'Sin asignación'}</p></article> })}</div>{!locked && !closed && <button className="secondary" disabled={busy || !scope.active || !['T1', 'T2'].some(type => chosen[type] && chosen[type] !== assignments.find(a => a.document_type === type)?.revision_id)} onClick={() => void operation('/admin/scopes/' + scope.id + '/workflows', 'PUT', { assignments: ['T1', 'T2'].filter(type => chosen[type] && chosen[type] !== assignments.find(a => a.document_type === type)?.revision_id).map(type => ({ type, revisionId: chosen[type], rowVersion: assignments.find(a => a.document_type === type)?.row_version ?? 0 })) }, 'Asignaciones del ámbito guardadas.')}><Icon name="check" size={16} />Guardar asignaciones</button>}</ActionForm></>}
        {!locked && !closed && <div className="period-activation"><p>Activar fija T1 y T2 de todos los ámbitos activos. Verifique principales y ejecutores: no podrán cambiarse las asignaciones estructurales de este período.</p><button disabled={busy || dirty || !scopes.length} onClick={async () => { if ((await confirmAction('¿Activar y bloquear los flujos de todos los ámbitos de este período?'))) void operation('/admin/periods/' + selected + '/activate-workflows', 'POST', { rowVersion: control.row_version }, 'Flujos activados y bloqueados para el período.') }}><Icon name="lock" size={16} />Activar flujos del período</button></div>}
      </section>
      <section className="card period-closure"><div className="section-heading"><span className="section-icon amber"><Icon name="document" /></span><div><span className="section-kicker">ARCHIVO E HISTORIAL</span><h3>{closed ? 'Cierre registrado' : 'Cerrar sin eliminar'}</h3></div></div>{closed ? <><p>{control.closure_reason}</p><p className="field-help">SHA-256 del archivo interno: <code>{control.archive_sha256}</code></p><p className="muted">La descarga histórica se realiza desde Documentos, por período, y contiene únicamente documentos autorizados. ADMIN por sí solo no concede acceso a su contenido.</p>{control.purged_at ? <p className="field-help">Documentación eliminada el {new Date(control.purged_at).toLocaleString('es-EC')}: {control.purge_reason}</p> : <div className="period-activation"><p>La copia de custodia es el ZIP completo del período. Descárguela y guárdela en un repositorio institucional antes de eliminar la documentación.{control.custody_downloaded_at && <> Última descarga: {new Date(control.custody_downloaded_at).toLocaleString('es-EC')}.</>}</p><button className="secondary" disabled={busy} onClick={async () => { setBusy(true); setError(''); try { await download('/admin/periods/' + selected + '/custody', 'utaped-custodia-' + control.name + '.zip'); setNotice('Copia de custodia descargada. Verifique su SHA-256 antes de eliminar.'); setReload(r => r + 1) } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo descargar la copia.') } finally { setBusy(false) } }}><Icon name="document" size={16} />Descargar copia de custodia</button>{!control.custody_downloaded_at ? <p className="field-help">La eliminación se habilita después de descargar la copia de custodia.</p> : !control.purge_ready && control.purge_available_at ? <p className="field-help">La eliminación se habilita el {new Date(control.purge_available_at).toLocaleDateString('es-EC')} ({control.purge_grace_days} días desde el cierre).</p> : <ActionForm key={'purge-' + selected} label="Eliminar documentación del período" disabled={busy} dirty={!!purgeReason} onCancel={() => setPurgeReason('')}><form onSubmit={async e => { e.preventDefault(); const value = purgeReason; if (await confirmAction('Se eliminarán definitivamente todos los documentos, PDFs, anexos y evidencias del período ' + control.name + '. Solo quedarán el inventario y la auditoría. ¿Continuar?')) void operation('/admin/periods/' + selected + '/purge', 'POST', { reason: value }, 'Documentación del período eliminada.') }}><fieldset disabled={busy}><label>Motivo de la eliminación<input value={purgeReason} required maxLength={1000} onChange={e => setPurgeReason(e.currentTarget.value)} /></label><button className="danger">Eliminar documentación</button></fieldset></form></ActionForm>}</div>}</> : <><p>Se generará un ZIP con metadatos, auditoría y archivos conservados. También los {pending} documentos sin aprobación quedarán en solo lectura, manteniendo sus estados.</p><ActionForm key={selected} label="Preparar cierre del período" disabled={busy} dirty={!!reason} onCancel={() => setReason('')}><label>Motivo del cierre<textarea rows={3} maxLength={1000} value={reason} disabled={busy} onChange={e => setReason(e.target.value)} /></label><div className="review-pagination"><button className="danger" disabled={busy || !reason.trim() || ['T1', 'T2'].some(type => (chosen[type] ?? '') !== (assignments.find(a => a.document_type === type)?.revision_id ?? ''))} onClick={async () => { if ((await confirmAction('¿Cerrar este período y bloquear todos sus cambios? Se conservarán ' + total + ' documentos; ' + pending + ' sin aprobación. No se borrará ningún dato.'))) void operation('/admin/periods/' + selected + '/close', 'POST', { rowVersion: control.row_version, reason }, 'Período cerrado. Archivo histórico generado sin eliminar datos.') }}>Cerrar período</button><button className="link" disabled={busy || !reason} onClick={() => setReason('')}>Cancelar motivo</button></div></ActionForm></>}</section>
    </>}
  </section>
}
