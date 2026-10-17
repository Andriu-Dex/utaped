import { confirmAction } from '../shared/confirm-action'
import { ActionForm } from '../shared/ActionForm'
import { useState, type FormEvent } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'
import { Icon } from '../shared/Icon'

export type Career = { id: string; code: string; name: string; active: boolean; row_version: number }
export function CareerEditor({ careers, onSaved, onDirtyChange }: { careers: Career[]; onSaved: () => void; onDirtyChange: (dirty: boolean) => void }) {
  const [selected, setSelected] = useState(''), [dirty, setDirty] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  const career = careers.find(c => c.id === selected)
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!career) return; const values = Object.fromEntries(new FormData(event.currentTarget))
    setBusy(true); setError(''); setNotice('')
    try { await api('/admin/careers/' + career.id, 'PUT', { ...values, active: values.active === 'true', rowVersion: career.row_version }); setDirty(false); onDirtyChange(false); onSaved(); setNotice('Carrera actualizada. Sus ámbitos e historial se conservan.') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.') } finally { setBusy(false) }
  }
  return <ActionForm label="Editar o desactivar una carrera" dirty={dirty} disabled={busy} onCancel={() => { setDirty(false); onDirtyChange(false); setSelected('') }}>{error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}<label>Carrera a editar<select value={selected} disabled={busy} onChange={async e => { const nextValue = e.currentTarget.value; if (dirty && !(await confirmAction('Hay cambios sin guardar. ¿Cambiar de carrera?'))) return; setDirty(false); onDirtyChange(false); setSelected(nextValue) }}><option value="">Seleccione una carrera</option>{careers.map(c => <option key={c.id} value={c.id}>{c.name}{!c.active && ' · Inactiva'}</option>)}</select></label>{career && <form key={career.id + ':' + career.row_version} onChange={() => { setDirty(true); onDirtyChange(true) }} onSubmit={save}><fieldset disabled={busy}><label>Código actualizado<input name="code" defaultValue={career.code} required maxLength={30} pattern="[A-Z0-9_\-]{1,30}" /></label><label>Nombre actualizado<input name="name" defaultValue={career.name} required maxLength={160} /></label><label>Estado de carrera<select name="active" defaultValue={String(career.active)}><option value="true">Activa</option><option value="false">Inactiva</option></select></label><p className="field-help">Desactivar impide elegirla para nuevos ámbitos y Planes. Los ámbitos existentes y documentos firmados conservan su historia.</p><button><Icon name="check" size={16} />Guardar carrera</button><button type="button" className="secondary" disabled={busy} onClick={async () => { if (dirty && !(await confirmAction('¿Cancelar y descartar los cambios de carrera?'))) return; setDirty(false); onDirtyChange(false); setSelected('') }}>Cancelar edición de carrera</button></fieldset></form>}</ActionForm>
}
