import { Toast } from '../shared/Toast'
import { useEffect, useState, type FormEvent } from 'react'
import { api, type Group, type Period } from '../shared/api'
import { categoryLabels, type Catalog, type Category, type Definition } from './matrixTypes'

export function PlanningAdministration({ groups, section, onDirtyChange }: { groups: Group[]; section: 'resources' | 'activities' | 'holidays'; onDirtyChange: (dirty: boolean) => void }) {
  const [periods, setPeriods] = useState<Period[]>([])
  const [catalogs, setCatalogs] = useState<Catalog[]>([]); const [definitions, setDefinitions] = useState<Definition[]>([])
  const [holidays, setHolidays] = useState<{ date: string; label: string }[]>([])
  const [groupId, setGroupId] = useState(''); const [label, setLabel] = useState('')
  const [holidayPeriod, setHolidayPeriod] = useState(''); const [holidayEnabled, setHolidayEnabled] = useState(false)
  const [catalog, setCatalog] = useState({ id: '', kind: 'RESOURCE' as Catalog['kind'], evidenceKind: 'PDF' as 'PDF' | 'URL', label: '', active: true })
  const [activity, setActivity] = useState({ id: '', title: '', category: 'POA' as Category, mandatory: false, active: true })
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState('')
  const [dirtyForms, setDirtyForms] = useState<string[]>([])
  const dirty = dirtyForms.length > 0
  function markDirty(key: string) { setDirtyForms(current => current.includes(key) ? current : [...current, key]) }
  function discard(keys: string[]) {
    if (dirtyForms.some(key => keys.includes(key)) && !window.confirm('Hay cambios sin guardar. ¿Descartarlos?')) return false
    setDirtyForms(current => current.filter(key => !keys.includes(key))); return true
  }
  useEffect(() => { onDirtyChange(dirty); return () => onDirtyChange(false) }, [dirty, onDirtyChange])
  useEffect(() => {
    const prevent = (event: BeforeUnloadEvent) => { if (dirty) { event.preventDefault(); event.returnValue = '' } }
    window.addEventListener('beforeunload', prevent); return () => window.removeEventListener('beforeunload', prevent)
  }, [dirty])
  async function load() {
    const [p, c, h] = await Promise.all([api<Period[]>('/periods'), api<Catalog[]>('/admin/planning/catalogs'), api<{ date: string; label: string }[]>('/admin/planning/holidays')])
    setPeriods(p); setCatalogs(c); setHolidays(h)
  }
  useEffect(() => { Promise.resolve().then(load).catch(e => setError(e.message)) }, [groups])
  useEffect(() => {
    let cancelled = false
    if (groupId) api<Definition[]>('/admin/planning/groups/' + groupId + '/activities').then(d => { if (!cancelled) setDefinitions(d) }).catch(e => { if (!cancelled) setError(e.message) })
    return () => { cancelled = true }
  }, [groupId])
  async function mutate(path: string, method: string, body?: unknown, formKey?: string) {
    setBusy(true); setError(''); setNotice('')
    try { await api(path, method, body); await load(); if (groupId) setDefinitions(await api('/admin/planning/groups/' + groupId + '/activities')); if (formKey) setDirtyForms(current => current.filter(key => key !== formKey)); setNotice('Configuración de planificación guardada.'); return true }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.'); return false } finally { setBusy(false) }
  }
  async function saveCatalog(e: FormEvent) { e.preventDefault(); if (await mutate('/admin/planning/catalogs' + (catalog.id ? '/' + catalog.id : ''), catalog.id ? 'PUT' : 'POST', catalog, 'catalog')) setCatalog({ id: '', kind: 'RESOURCE', evidenceKind: 'PDF', label: '', active: true }) }
  async function saveActivity(e: FormEvent) { e.preventDefault(); if (await mutate('/admin/planning/activities' + (activity.id ? '/' + activity.id : ''), activity.id ? 'PUT' : 'POST', { ...activity, groupId }, 'activity')) setActivity({ id: '', title: '', category: 'POA', mandatory: false, active: true }) }
  return <section className="card"><p>Los catálogos comienzan vacíos. Registre las denominaciones y fechas institucionales aplicables.</p>
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    <fieldset disabled={busy}><div>{section === 'resources' && <section><h3>Recursos y medios</h3><form onChange={() => markDirty('catalog')} onSubmit={saveCatalog}>
      <label>Tipo de catálogo<select value={catalog.kind} disabled={!!catalog.id} onChange={e => setCatalog({ ...catalog, kind: e.target.value as Catalog['kind'], evidenceKind: 'PDF' })}><option value="RESOURCE">Recurso</option><option value="MEANS">Medio de verificación</option></select></label>
      <p className="field-help">El formato queda conservado en cada matriz guardada. Cambiar este catálogo se aplica a nuevas selecciones.</p>{catalog.kind === 'MEANS' && <label>Formato de evidencia<select value={catalog.evidenceKind} onChange={e => setCatalog({ ...catalog, evidenceKind: e.target.value as 'PDF' | 'URL' })}><option value="PDF">Archivo PDF privado</option><option value="URL">Enlace HTTPS externo</option></select></label>}
      <label>Denominación del catálogo<input required maxLength={200} value={catalog.label} onChange={e => setCatalog({ ...catalog, label: e.target.value })} /></label>
      <label className="check"><input type="checkbox" checked={catalog.active} onChange={e => setCatalog({ ...catalog, active: e.target.checked })} />Elemento activo</label><button>{catalog.id ? 'Actualizar elemento' : 'Crear elemento'}</button>
      {catalog.id && <button type="button" className="secondary" onClick={() => { if (discard(['catalog'])) setCatalog({ id: '', kind: 'RESOURCE', evidenceKind: 'PDF', label: '', active: true }) }}>Cancelar edición de elemento</button>}
    </form><ul>{catalogs.map(c => <li key={c.id}>{c.kind === 'RESOURCE' ? 'Recurso' : 'Medio'}: {c.label}{c.kind === 'MEANS' ? ' · ' + (c.evidenceKind === 'URL' ? 'Enlace HTTPS' : 'PDF') : ''}{!c.active ? ' · Inactivo' : ''} <button className="secondary" title="Editar elemento" aria-label={'Editar elemento ' + c.label} onClick={() => { if (discard(['catalog'])) setCatalog({ ...c, evidenceKind: c.evidenceKind ?? 'PDF' }) }}>✎</button></li>)}</ul></section>}
    {section === 'activities' && <section><h3>Actividades por grupo</h3><label>Grupo del catálogo<select value={groupId} onChange={e => { if (!discard(['activity', 'collective'])) return; setGroupId(e.target.value); setDefinitions([]); setLabel(groups.find(g => g.id === e.target.value)?.collective_label ?? ''); setActivity({ id: '', title: '', category: 'POA', mandatory: false, active: true }) }}><option value="">Seleccione</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>
      {groupId && <><form onChange={() => markDirty('activity')} onSubmit={saveActivity}><label>Título de actividad del catálogo<input required maxLength={500} value={activity.title} onChange={e => setActivity({ ...activity, title: e.target.value })} /></label>
        <label>Clasificación de actividad<select value={activity.category} onChange={e => setActivity({ ...activity, category: e.target.value as Category })}>{Object.entries(categoryLabels).map(([id, label]) => <option value={id} key={id}>{label}</option>)}</select></label>
        <label className="check"><input type="checkbox" checked={activity.mandatory} onChange={e => setActivity({ ...activity, mandatory: e.target.checked })} />Obligatoria para este grupo</label><label className="check"><input type="checkbox" checked={activity.active} onChange={e => setActivity({ ...activity, active: e.target.checked })} />Actividad activa</label><button>{activity.id ? 'Actualizar actividad' : 'Crear actividad del catálogo'}</button>
        {activity.id && <button type="button" className="secondary" onClick={() => { if (discard(['activity'])) setActivity({ id: '', title: '', category: 'POA', mandatory: false, active: true }) }}>Cancelar edición de actividad</button>}</form>
        <ul>{definitions.map(d => <li key={d.id}>{d.title} · {categoryLabels[d.category]}{d.mandatory ? ' · Obligatoria' : ''}{!d.active ? ' · Inactiva' : ''} <button className="secondary" title="Editar actividad" aria-label={'Editar actividad ' + d.title} onClick={() => { if (discard(['activity'])) setActivity(d) }}>✎</button></li>)}</ul>
        <form onChange={() => markDirty('collective')} onSubmit={e => { e.preventDefault(); void mutate('/admin/planning/groups/' + groupId + '/collective-label', 'PUT', { label }, 'collective') }}><label>Denominación colectiva del grupo<input maxLength={200} value={label} onChange={e => setLabel(e.target.value)} /></label><button>Guardar denominación colectiva</button></form>
      </>}
    </section>}</div>
    {section === 'holidays' && <section><h3>Feriados y restricciones</h3><form className="grid" onChange={() => markDirty('holiday')} onSubmit={e => { e.preventDefault(); const form = e.currentTarget; const data = Object.fromEntries(new FormData(form)); void mutate('/admin/planning/holidays', 'POST', data, 'holiday').then(ok => { if (ok) form.reset() }) }}><label>Fecha del feriado<input required type="date" name="date" /></label><label>Nombre del feriado<input required maxLength={200} name="label" /></label><button>Guardar feriado</button></form>
      <ul>{holidays.map(h => <li key={h.date}>{h.date.split('-').reverse().join('/')} · {h.label} <button title="Eliminar feriado" aria-label={'Eliminar feriado ' + h.label} className="secondary" onClick={() => { if (window.confirm('¿Eliminar este feriado configurado?')) void mutate('/admin/planning/holidays/' + h.date, 'DELETE') }}>Eliminar</button></li>)}</ul>
      <form className="grid" onChange={() => markDirty('policy')} onSubmit={e => { e.preventDefault(); void mutate('/admin/planning/periods/' + holidayPeriod + '/holiday-policy', 'PUT', { enabled: holidayEnabled }, 'policy') }}><label>Período de la restricción<select required value={holidayPeriod} onChange={e => { setHolidayPeriod(e.target.value); setHolidayEnabled(periods.find(p => p.id === e.target.value)?.restrict_holiday_endpoints ?? false) }}><option value="">Seleccione</option>{periods.map(p => <option key={p.id} value={p.id}>{p.name}{p.restrict_holiday_endpoints ? ' · Restricción activa' : ' · Sin restricción'}</option>)}</select></label><label className="check"><input type="checkbox" checked={holidayEnabled} onChange={e => setHolidayEnabled(e.target.checked)} />Impedir inicio o fin en feriados</label><button>Guardar restricción de feriados</button></form>
    </section>}</fieldset>
  </section>
}
