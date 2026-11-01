import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { confirmAction } from '../shared/confirm-action'

type Holiday = { date: string; label: string; sourceUrl: string }
export function HolidaySuggestions({ existing, onSaved, onDirtyChange }: { existing: string[]; onSaved: () => Promise<void>; onDirtyChange: (dirty: boolean) => void }) {
  const [year, setYear] = useState(() => new Date().getFullYear()), [rows, setRows] = useState<Holiday[]>([]), [selected, setSelected] = useState<string[]>([])
  const [reviewed, setReviewed] = useState(false), [overwrite, setOverwrite] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  useEffect(() => { onDirtyChange(selected.length > 0); return () => onDirtyChange(false) }, [selected.length, onDirtyChange])
  async function suggest() {
    if (selected.length && !(await confirmAction('¿Descartar la selección actual y consultar una nueva propuesta?'))) return
    setBusy(true); setError(''); setNotice('')
    try { const data = await api<{ items: Holiday[] }>('/admin/planning/holiday-suggestions', 'POST', { year }); setRows(data.items); setSelected([]); setReviewed(false); setOverwrite(false); if (!data.items.length) setNotice('No se obtuvieron fechas respaldadas por fuentes para este año.') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo obtener la propuesta. El calendario se conserva.') }
    finally { setBusy(false) }
  }
  async function apply() {
    setBusy(true); setError('')
    try { await api('/admin/planning/holiday-suggestions/apply', 'POST', { year, items: rows.filter(row => selected.includes(row.date)), reviewed, overwrite }); await onSaved(); setSelected([]); setReviewed(false); setOverwrite(false); setNotice('Fechas revisadas incorporadas al calendario.') }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo incorporar la selección.') }
    finally { setBusy(false) }
  }
  return <section className="card"><h3>Consulta asistida de feriados</h3><p className="muted">Ambato, Ecuador. Revise las fuentes oficiales antes de incorporar las fechas.</p>{error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}<ActionForm label="Consultar propuesta de feriados" modal disabled={busy} dirty={selected.length > 0} onCancel={() => { setSelected([]); setReviewed(false) }}><div className="grid"><label>Año de consulta<input type="number" min={2000} max={2200} value={year} disabled={busy} onChange={async e => { const value = Number(e.currentTarget.value); if (!selected.length || await confirmAction('¿Descartar la selección para cambiar de año?')) { setYear(value); setRows([]); setSelected([]); setReviewed(false) } }} /></label><button type="button" disabled={busy || year < 2000 || year > 2200} onClick={() => void suggest()}>{busy ? 'Consultando…' : 'Obtener propuesta'}</button></div><div className="catalog-reference-list">{rows.map(row => <label className="catalog-reference-option" key={row.date}><input type="checkbox" checked={selected.includes(row.date)} onChange={e => { const checked = e.currentTarget.checked; setReviewed(false); setSelected(values => checked ? [...values, row.date] : values.filter(value => value !== row.date)) }} /><span><strong>{row.date.split('-').reverse().join('/')} · {row.label}</strong><a href={row.sourceUrl} target="_blank" rel="noreferrer">Revisar fuente</a><small>{row.sourceUrl}</small>{existing.includes(row.date) && <small>Esta fecha ya está registrada.</small>}</span></label>)}</div>{selected.length > 0 && <><label className="check"><input type="checkbox" checked={reviewed} onChange={e => setReviewed(e.currentTarget.checked)} />He revisado las fechas y fuentes seleccionadas.</label>{selected.some(date => existing.includes(date)) && <label className="check"><input type="checkbox" checked={overwrite} onChange={e => setOverwrite(e.currentTarget.checked)} />Reemplazar las fechas ya registradas de esta selección.</label>}</>}<div className="action-form-actions"><button type="button" disabled={busy || !selected.length || !reviewed} onClick={() => void apply()}>Incorporar fechas revisadas ({selected.length})</button></div></ActionForm></section>
}
