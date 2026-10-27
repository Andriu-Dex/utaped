import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'
import { Icon } from '../shared/Icon'
import { confirmAction } from '../shared/confirm-action'
export type InstitutionalUnit = { id: string; name: string; unit_type: 'ACADEMIC' | 'ADMINISTRATIVE'; active: boolean; row_version: number }
export const FISEI_UNIT = 'd091bc85-0823-4977-8b45-493cb466f858'
export function InstitutionalUnitsEditor({ onDirtyChange, onSaved }: { onDirtyChange: (value: boolean) => void; onSaved: () => void }) {
  const form = useRef<HTMLFormElement>(null), origin = useRef<HTMLButtonElement | null>(null)
  const [units, setUnits] = useState<InstitutionalUnit[]>([]), [revision, setRevision] = useState(0)
  const [editing, setEditing] = useState<InstitutionalUnit | null>(null), [open, setOpen] = useState(false)
  const [dirty, setDirty] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  useEffect(() => { if (open) form.current?.querySelector<HTMLElement>('select,input')?.focus() }, [open])
  useEffect(() => { let active = true; api<InstitutionalUnit[]>('/institutional-units').then(v => { if (active) setUnits(v) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [revision])
  async function close() { if (dirty && !(await confirmAction('¿Descartar los cambios de la unidad?'))) return; setOpen(false); setDirty(false); onDirtyChange(false); requestAnimationFrame(() => origin.current?.focus()) }
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const data = Object.fromEntries(new FormData(event.currentTarget)); setBusy(true); setError('')
    try { await api('/admin/institutional-units' + (editing ? '/' + editing.id : ''), editing ? 'PUT' : 'POST', { ...data, active: data.active === 'true', rowVersion: editing?.row_version ?? 0 }); setOpen(false); setDirty(false); onDirtyChange(false); setRevision(n => n + 1); onSaved(); setNotice('Unidad guardada. Los documentos existentes conservan su procedencia.') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar la unidad.') } finally { setBusy(false) }
  }
  return <section className="card"><div className="section-heading"><span className="section-icon"><Icon name="users" /></span><div><span className="section-kicker">PROCEDENCIA INSTITUCIONAL</span><h2>Unidades académicas y administrativas</h2></div></div><p className="field-help">Las unidades académicas pueden tener carreras. Las administrativas no requieren carrera. Desactivar impide nuevas asignaciones y conserva el historial.</p>
    {error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    {!open && <button type="button" onClick={e => { origin.current = e.currentTarget; setEditing(null); setOpen(true) }}><Icon name="plus" />Crear unidad</button>}
    <div className="table-scroll"><table><thead><tr><th>Unidad</th><th>Tipo</th><th>Estado</th><th>Acción</th></tr></thead><tbody>{units.map(unit => <tr key={unit.id}><th scope="row">{unit.name}</th><td>{unit.unit_type === 'ACADEMIC' ? 'Académica' : 'Administrativa'}</td><td>{unit.active ? 'Activa' : 'Inactiva'}</td><td><button type="button" className="secondary" disabled={open || busy} title={'Editar ' + unit.name} aria-label={'Editar unidad ' + unit.name} onClick={e => { origin.current = e.currentTarget; setEditing(unit); setOpen(true) }}><Icon name="edit" /></button></td></tr>)}</tbody></table></div>
    {open && <form ref={form} key={editing?.id ?? 'new'} onSubmit={save} onChange={() => { setDirty(true); onDirtyChange(true) }}><fieldset disabled={busy}><legend>{editing ? 'Editar unidad' : 'Nueva unidad institucional'}</legend><div className="grid"><label>Tipo de unidad<select aria-label="Tipo de unidad" name="unitType" defaultValue={editing?.unit_type ?? 'ACADEMIC'}><option value="ACADEMIC">Académica</option><option value="ADMINISTRATIVE">Administrativa</option></select></label><label>Nombre de la unidad<input aria-label="Nombre de la unidad" name="name" required maxLength={200} defaultValue={editing?.name ?? ''} /></label><label>Estado de la unidad<select aria-label="Estado de la unidad" name="active" defaultValue={String(editing?.active ?? true)}><option value="true">Activa</option><option value="false">Inactiva</option></select></label></div><div className="action-form-actions"><button>Guardar unidad</button><button type="button" className="secondary" onClick={() => void close()}>Cancelar unidad</button></div></fieldset></form>}
  </section>
}
