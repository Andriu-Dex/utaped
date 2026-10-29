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
  return <section className="card institutional-units-card">
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
      <div className="section-heading" style={{ marginBottom: 0 }}>
        <span className="section-icon teal"><Icon name="users" /></span>
        <div>
          <span className="section-kicker">PROCEDENCIA INSTITUCIONAL</span>
          <h2>Unidades académicas y administrativas</h2>
          <p className="field-help" style={{ margin: '4px 0 0' }}>Las unidades académicas pueden tener carreras. Las administrativas no requieren carrera. Desactivar impide nuevas asignaciones y conserva el historial.</p>
        </div>
      </div>
      {!open && (
        <button type="button" onClick={e => { origin.current = e.currentTarget; setEditing(null); setOpen(true) }}>
          <Icon name="plus" />Crear unidad
        </button>
      )}
    </div>
    {error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    <div className="table-scroll" style={{ marginTop: 16 }}>
      <table>
        <thead>
          <tr>
            <th>Unidad</th>
            <th>Tipo</th>
            <th>Estado</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          {units.map(unit => (
            <tr key={unit.id}>
              <th scope="row" style={{ fontWeight: 600 }}>{unit.name}</th>
              <td><span className="scope-tag">{unit.unit_type === 'ACADEMIC' ? 'Académica' : 'Administrativa'}</span></td>
              <td><span className={'status-badge ' + (unit.active ? 'active' : 'inactive')}>{unit.active ? 'Activa' : 'Inactiva'}</span></td>
              <td className="row-actions">
                <button
                  type="button"
                  className="secondary table-action-icon-btn"
                  disabled={open || busy}
                  title={'Editar ' + unit.name}
                  aria-label={'Editar unidad ' + unit.name}
                  onClick={e => { origin.current = e.currentTarget; setEditing(unit); setOpen(true) }}
                >
                  <Icon name="edit" size={16} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
    {open && (
      <form ref={form} key={editing?.id ?? 'new'} onSubmit={save} onChange={() => { setDirty(true); onDirtyChange(true) }} style={{ marginTop: 18 }}>
        <fieldset disabled={busy}>
          <legend>{editing ? 'Editar unidad' : 'Nueva unidad institucional'}</legend>
          <div className="grid">
            <label>Tipo de unidad<select aria-label="Tipo de unidad" name="unitType" defaultValue={editing?.unit_type ?? 'ACADEMIC'}><option value="ACADEMIC">Académica</option><option value="ADMINISTRATIVE">Administrativa</option></select></label>
            <label>Nombre de la unidad<input aria-label="Nombre de la unidad" name="name" required maxLength={200} defaultValue={editing?.name ?? ''} /></label>
            <label>Estado de la unidad<select aria-label="Estado de la unidad" name="active" defaultValue={String(editing?.active ?? true)}><option value="true">Activa</option><option value="false">Inactiva</option></select></label>
          </div>
          <div className="action-form-actions" style={{ marginTop: 14 }}>
            <button><Icon name="check" size={16} />Guardar unidad</button>
            <button type="button" className="secondary" onClick={() => void close()}>Cancelar unidad</button>
          </div>
        </fieldset>
      </form>
    )}
  </section>
}
