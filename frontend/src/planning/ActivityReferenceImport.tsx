import { useEffect, useState } from 'react'
import { api, type Group, type Period } from '../shared/api'
import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { categoryLabels, type ActivityContext, type Category } from './matrixTypes'

interface Entry { key: string; title: string; category: Category; suggestedGroups: string[]; careerCode?: string | null; context: ActivityContext }
interface Reference { revision: string; sources: { file: string; sha256: string }[]; entries: Entry[] }
export function ActivityReferenceImport({ groupId, groups, periods, onSaved, onDirtyChange }: { groupId: string; groups: Group[]; periods: Period[]; onSaved: () => Promise<void>; onDirtyChange: (dirty: boolean) => void }) {
  const [periodId, setPeriodId] = useState(''), [careerId, setCareerId] = useState('')
  const [careers, setCareers] = useState<{ id: string; code: string; name: string; active: boolean }[]>([])
  const [reload, setReload] = useState(0)
  const [reference, setReference] = useState<Reference | null>(null)
  const [selected, setSelected] = useState<string[]>([]), [query, setQuery] = useState('')
  const [category, setCategory] = useState(''), [all, setAll] = useState(false), [busy, setBusy] = useState(false)
  const [error, setError] = useState(''), [notice, setNotice] = useState('')
  useEffect(() => { let active = true; Promise.all([api<Reference>('/admin/planning/activity-reference'), api<{ id: string; code: string; name: string; active: boolean }[]>('/careers')]).then(([data, values]) => { if (active) { setReference(data); setCareers(values) } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [reload])
  useEffect(() => { onDirtyChange(selected.length > 0); return () => onDirtyChange(false) }, [selected.length, onDirtyChange])
  const group = groups.find(g => g.id === groupId)
  const visible = reference?.entries.filter(entry => (all || entry.suggestedGroups.includes(group?.name ?? '')) && (!careerId || !entry.careerCode || careers.find(c => c.id === careerId)?.code === entry.careerCode) && (!category || entry.category === category) && [entry.title, entry.context.objective, entry.careerCode, entry.context.criterion, entry.context.indicator, entry.context.strategy].join(' ').toLocaleLowerCase('es').includes(query.toLocaleLowerCase('es'))) ?? []
  async function apply() {
    setBusy(true); setError('')
    try {
      const result = await api<{ created: number; unchanged: number }>('/admin/planning/activity-reference/import', 'POST', { selections: selected.map(key => ({ key, groupId, periodId: periodId || null, careerId: careerId || null })) })
      setSelected([]); await onSaved(); setNotice(`${result.created} actividades incorporadas · ${result.unchanged} ya existentes.`)
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo incorporar la selección.') }
    finally { setBusy(false) }
  }
  return <section className="card" aria-label="Referencias oficiales de actividades">
    <h3>Catálogos institucionales</h3>
    <p className="muted">Revise las actividades oficiales y seleccione las que corresponden a {group?.name}.</p>
    {error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    {!reference && error && <button type="button" className="secondary" onClick={() => { setError(''); setReload(n => n + 1) }}>Reintentar referencias</button>}
    <ActionForm label="Incorporar actividades oficiales" modal disabled={busy || !reference} dirty={selected.length > 0} onCancel={() => setSelected([])}>
      <div className="grid"><label>Origen<select value={category} onChange={e => setCategory(e.target.value)}><option value="">Todos los orígenes</option>{(['POA', 'IMPROVEMENT_PLAN', 'IMPROVEMENT_ACTION'] as const).map(value => <option key={value} value={value}>{categoryLabels[value]}</option>)}</select></label><label>Buscar actividades oficiales<input type="search" value={query} onChange={e => setQuery(e.target.value)} /></label></div>
      <div className="grid"><label>Período de aplicación<select value={periodId} onChange={e => setPeriodId(e.currentTarget.value)}><option value="">Según el año de la fuente</option>{periods.filter(p => p.status === 'ACTIVE' && reference?.entries.some(entry => !entry.context.sourceYear || Number(p.starts_on.slice(0, 4)) <= entry.context.sourceYear && Number(p.ends_on.slice(0, 4)) >= entry.context.sourceYear)).map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label><label>Carrera de aplicación<select value={careerId} onChange={e => { setCareerId(e.currentTarget.value); setSelected([]) }}><option value="">Según la fuente o todas las carreras</option>{careers.filter(c => c.active).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label></div>
      <label className="check"><input type="checkbox" checked={all} onChange={e => setAll(e.target.checked)} />Consultar también referencias de otras comisiones</label>
      <p>{visible.length} opciones · {selected.length} seleccionadas</p>
      <div className="catalog-reference-list">{visible.map(entry => <label key={entry.key} className="catalog-reference-option"><input type="checkbox" disabled={busy} checked={selected.includes(entry.key)} onChange={e => setSelected(ids => e.target.checked ? [...ids, entry.key] : ids.filter(id => id !== entry.key))} /><span><strong>{entry.title}</strong><small>{[categoryLabels[entry.category], entry.careerCode, entry.context.criterion, entry.context.indicator, entry.context.strategy].filter(Boolean).join(' · ')}</small><small>{entry.context.sourceFile}</small></span></label>)}{!visible.length && <p className="muted">No hay referencias con esta selección.</p>}</div>
      <div className="action-form-actions"><button type="button" disabled={busy || !selected.length} onClick={() => void apply()}>{busy ? 'Incorporando…' : 'Incorporar selección'}</button></div>
    </ActionForm>
  </section>
}
