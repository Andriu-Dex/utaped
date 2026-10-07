import { Toast } from '../shared/Toast'
import { useEffect, useState, type FormEvent } from 'react'
import { api, type Group, type Period } from '../shared/api'
import { categoryLabels, type Catalog, type Category, type Definition } from './matrixTypes'

export function PlanningAdministration({ groups }: { groups: Group[] }) {
  const [periods, setPeriods] = useState<Period[]>([])
  const [catalogs, setCatalogs] = useState<Catalog[]>([]); const [definitions, setDefinitions] = useState<Definition[]>([])
  const [holidays, setHolidays] = useState<{ date: string; label: string }[]>([])
  const [groupId, setGroupId] = useState(''); const [label, setLabel] = useState('')
  const [holidayPeriod, setHolidayPeriod] = useState(''); const [holidayEnabled, setHolidayEnabled] = useState(false)
  const [catalog, setCatalog] = useState({ id: '', kind: 'RESOURCE' as Catalog['kind'], label: '', active: true })
  const [activity, setActivity] = useState({ id: '', title: '', category: 'POA' as Category, mandatory: false, active: true })
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState('')
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
  async function mutate(path: string, method: string, body?: unknown) {
    setBusy(true); setError(''); setNotice('')
    try { await api(path, method, body); await load(); if (groupId) setDefinitions(await api('/admin/planning/groups/' + groupId + '/activities')); setNotice('Configuración de planificación guardada.'); return true }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.'); return false } finally { setBusy(false) }
  }
  async function saveCatalog(e: FormEvent) { e.preventDefault(); if (await mutate('/admin/planning/catalogs' + (catalog.id ? '/' + catalog.id : ''), catalog.id ? 'PUT' : 'POST', catalog)) setCatalog({ id: '', kind: 'RESOURCE', label: '', active: true }) }
  async function saveActivity(e: FormEvent) { e.preventDefault(); if (await mutate('/admin/planning/activities' + (activity.id ? '/' + activity.id : ''), activity.id ? 'PUT' : 'POST', { ...activity, groupId })) setActivity({ id: '', title: '', category: 'POA', mandatory: false, active: true }) }
  return <section className="card"><h2>Configuración de planificación</h2><p>Los catálogos comienzan vacíos. Registre las denominaciones y fechas institucionales aplicables.</p>
    {error && <Toast message={error} />}{notice && <Toast tone="success" message={notice} />}
    <fieldset disabled={busy}><div className="grid"><section><h3>Recursos y medios</h3><form onSubmit={saveCatalog}>
      <label>Tipo de catálogo<select value={catalog.kind} disabled={!!catalog.id} onChange={e => setCatalog({ ...catalog, kind: e.target.value as Catalog['kind'] })}><option value="RESOURCE">Recurso</option><option value="MEANS">Medio de verificación</option></select></label>
      <label>Denominación del catálogo<input required maxLength={200} value={catalog.label} onChange={e => setCatalog({ ...catalog, label: e.target.value })} /></label>
      <label className="check"><input type="checkbox" checked={catalog.active} onChange={e => setCatalog({ ...catalog, active: e.target.checked })} />Elemento activo</label><button>{catalog.id ? 'Actualizar elemento' : 'Crear elemento'}</button>
      {catalog.id && <button type="button" className="secondary" onClick={() => setCatalog({ id: '', kind: 'RESOURCE', label: '', active: true })}>Cancelar edición de elemento</button>}
    </form><ul>{catalogs.map(c => <li key={c.id}>{c.kind === 'RESOURCE' ? 'Recurso' : 'Medio'}: {c.label}{!c.active ? ' · Inactivo' : ''} <button className="secondary" title="Editar elemento" aria-label={'Editar elemento ' + c.label} onClick={() => setCatalog(c)}>✎</button></li>)}</ul></section>
    <section><h3>Actividades por grupo</h3><label>Grupo del catálogo<select value={groupId} onChange={e => { setGroupId(e.target.value); setDefinitions([]); setLabel(groups.find(g => g.id === e.target.value)?.collective_label ?? ''); setActivity({ id: '', title: '', category: 'POA', mandatory: false, active: true }) }}><option value="">Seleccione</option>{groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}</select></label>
      {groupId && <><form onSubmit={saveActivity}><label>Título de actividad del catálogo<input required maxLength={500} value={activity.title} onChange={e => setActivity({ ...activity, title: e.target.value })} /></label>
        <label>Clasificación de actividad<select value={activity.category} onChange={e => setActivity({ ...activity, category: e.target.value as Category })}>{Object.entries(categoryLabels).map(([id, label]) => <option value={id} key={id}>{label}</option>)}</select></label>
        <label className="check"><input type="checkbox" checked={activity.mandatory} onChange={e => setActivity({ ...activity, mandatory: e.target.checked })} />Obligatoria para este grupo</label><label className="check"><input type="checkbox" checked={activity.active} onChange={e => setActivity({ ...activity, active: e.target.checked })} />Actividad activa</label><button>{activity.id ? 'Actualizar actividad' : 'Crear actividad del catálogo'}</button>
        {activity.id && <button type="button" className="secondary" onClick={() => setActivity({ id: '', title: '', category: 'POA', mandatory: false, active: true })}>Cancelar edición de actividad</button>}</form>
        <ul>{definitions.map(d => <li key={d.id}>{d.title} · {categoryLabels[d.category]}{d.mandatory ? ' · Obligatoria' : ''}{!d.active ? ' · Inactiva' : ''} <button className="secondary" title="Editar actividad" aria-label={'Editar actividad ' + d.title} onClick={() => setActivity(d)}>✎</button></li>)}</ul>
        <form onSubmit={e => { e.preventDefault(); void mutate('/admin/planning/groups/' + groupId + '/collective-label', 'PUT', { label }) }}><label>Denominación colectiva del grupo<input maxLength={200} value={label} onChange={e => setLabel(e.target.value)} /></label><button>Guardar denominación colectiva</button></form>
      </>}
    </section></div>
    <section><h3>Feriados y restricciones</h3><form className="grid" onSubmit={e => { e.preventDefault(); const form = e.currentTarget; const data = Object.fromEntries(new FormData(form)); void mutate('/admin/planning/holidays', 'POST', data).then(ok => { if (ok) form.reset() }) }}><label>Fecha del feriado<input required type="date" name="date" /></label><label>Nombre del feriado<input required maxLength={200} name="label" /></label><button>Guardar feriado</button></form>
      <ul>{holidays.map(h => <li key={h.date}>{h.date.split('-').reverse().join('/')} · {h.label} <button title="Eliminar feriado" aria-label={'Eliminar feriado ' + h.label} className="secondary" onClick={() => { if (window.confirm('¿Eliminar este feriado configurado?')) void mutate('/admin/planning/holidays/' + h.date, 'DELETE') }}>Eliminar</button></li>)}</ul>
      <form className="grid" onSubmit={e => { e.preventDefault(); void mutate('/admin/planning/periods/' + holidayPeriod + '/holiday-policy', 'PUT', { enabled: holidayEnabled }) }}><label>Período de la restricción<select required value={holidayPeriod} onChange={e => { setHolidayPeriod(e.target.value); setHolidayEnabled(periods.find(p => p.id === e.target.value)?.restrict_holiday_endpoints ?? false) }}><option value="">Seleccione</option>{periods.map(p => <option key={p.id} value={p.id}>{p.name}{p.restrict_holiday_endpoints ? ' · Restricción activa' : ' · Sin restricción'}</option>)}</select></label><label className="check"><input type="checkbox" checked={holidayEnabled} onChange={e => setHolidayEnabled(e.target.checked)} />Impedir inicio o fin en feriados</label><button>Guardar restricción de feriados</button></form>
    </section></fieldset>
  </section>
}
