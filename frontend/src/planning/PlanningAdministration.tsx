import { ActivityProposalReview } from './ActivityProposalReview'
import { HolidaySuggestions } from './HolidaySuggestions'
import { ActivityReferenceImport } from './ActivityReferenceImport'
import { confirmAction } from '../shared/confirm-action'
import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, type Group, type Period } from '../shared/api'
import { Icon } from '../shared/Icon'
import { categoryLabels, type Catalog, type Category, type Definition, type ActivityContext } from './matrixTypes'
import './planning-admin.css'

export function PlanningAdministration({ groups, section, onDirtyChange }: { groups: Group[]; section: 'resources' | 'activities' | 'holidays'; onDirtyChange: (dirty: boolean) => void }) {
  const [periods, setPeriods] = useState<Period[]>([])
  const [catalogs, setCatalogs] = useState<Catalog[]>([]); const [definitions, setDefinitions] = useState<Definition[]>([])
  const [holidays, setHolidays] = useState<{ date: string; label: string }[]>([])
  const [groupId, setGroupId] = useState(''); const [label, setLabel] = useState('')
  const [holidayPeriod, setHolidayPeriod] = useState(''); const [holidayEnabled, setHolidayEnabled] = useState(false)
  const [catalog, setCatalog] = useState({ id: '', kind: 'RESOURCE' as Catalog['kind'], evidenceKind: 'PDF' as 'PDF' | 'URL', label: '', active: true })
  const [activity, setActivity] = useState<{ id: string; title: string; category: Category; mandatory: boolean; active: boolean; context?: ActivityContext | null }>({ id: '', title: '', category: 'POA' as Category, mandatory: false, active: true })
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState('')
  const [dirtyForms, setDirtyForms] = useState<string[]>([])
  const dirty = dirtyForms.length > 0
  const [loaded, setLoaded] = useState(false), [reload, setReload] = useState(0), [loadFailed, setLoadFailed] = useState(false)
  function markDirty(key: string) { setDirtyForms(current => current.includes(key) ? current : [...current, key]) }
  async function discard(keys: string[]) {
    if (dirtyForms.some(key => keys.includes(key)) && !(await confirmAction('Hay cambios sin guardar. ¿Descartarlos?'))) return false
    setDirtyForms(current => current.filter(key => !keys.includes(key))); return true
  }
  useEffect(() => { onDirtyChange(dirty); return () => onDirtyChange(false) }, [dirty, onDirtyChange])
  useEffect(() => {
    const prevent = (event: BeforeUnloadEvent) => { if (dirty) { event.preventDefault(); event.returnValue = '' } }
    window.addEventListener('beforeunload', prevent); return () => window.removeEventListener('beforeunload', prevent)
  }, [dirty])
  async function load() {
    const [p, c, h] = await Promise.all([api<Period[]>('/periods'), api<Catalog[]>('/admin/planning/catalogs'), api<{ date: string; label: string }[]>('/admin/planning/holidays')])
    setPeriods(p); setCatalogs(c); setHolidays(h); setLoaded(true); setLoadFailed(false)
  }
  useEffect(() => { Promise.resolve().then(load).then(() => setError('')).catch(e => { setError(e.message); setLoadFailed(true) }) }, [groups, reload])
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
  const proposalsDirty = useCallback((dirty: boolean) => setDirtyForms(keys => dirty ? keys.includes('proposals') ? keys : [...keys, 'proposals'] : keys.filter(key => key !== 'proposals')), [])
  const suggestionsDirty = useCallback((dirty: boolean) => setDirtyForms(keys => dirty ? keys.includes('holiday-suggestions') ? keys : [...keys, 'holiday-suggestions'] : keys.filter(key => key !== 'holiday-suggestions')), [])
  const referenceDirty = useCallback((dirty: boolean) => setDirtyForms(keys => dirty ? keys.includes('reference') ? keys : [...keys, 'reference'] : keys.filter(key => key !== 'reference')), [])
  async function saveActivity(e: FormEvent) { e.preventDefault(); if (await mutate('/admin/planning/activities' + (activity.id ? '/' + activity.id : ''), activity.id ? 'PUT' : 'POST', { ...activity, groupId }, 'activity')) setActivity({ id: '', title: '', category: 'POA', mandatory: false, active: true }) }
  const retry = <button type="button" className="secondary" disabled={busy || dirty} onClick={() => { setError(''); setReload(value => value + 1) }}>Reintentar catálogos de planificación</button>
  if (!loaded) return <section className="card"><h2>Catálogos de planificación</h2>{error ? <><Toast message={error} />{retry}</> : <p role="status">Cargando catálogos de planificación…</p>}</section>
  return <section className="card">
    {error && <Toast message={error} />}{loadFailed && retry}{notice && <Toast tone="success" message={notice} />}
    <fieldset disabled={busy}><div>{section === 'resources' && <section>
      <div className="section-heading">
        <div className="section-heading-icon" aria-hidden="true"><Icon name="document" size={24} /></div>
        <div>
          <span className="section-heading-kicker">PLANIFICACIÓN DOCENTE</span>
          <h3>Recursos y medios</h3>
          <p className="muted">Administre los recursos institucionales y formatos de medios de verificación para matrices docentes.</p>
        </div>
      </div>
      <ActionForm label="Crear o editar elemento" openSignal={catalog.id || undefined} dirty={dirtyForms.includes('catalog')} disabled={busy} onCancel={() => { setDirtyForms(keys => keys.filter(k => k !== 'catalog')); setCatalog({ id: '', kind: 'RESOURCE', evidenceKind: 'PDF', label: '', active: true }) }}>
        <form onChange={() => markDirty('catalog')} onSubmit={saveCatalog}>
          <label>Tipo de catálogo<select value={catalog.kind} disabled={!!catalog.id} onChange={e => setCatalog({ ...catalog, kind: e.target.value as Catalog['kind'], evidenceKind: 'PDF' })}><option value="RESOURCE">Recurso</option><option value="MEANS">Medio de verificación</option></select></label>
          <p className="field-help">El formato queda conservado en cada matriz guardada. Cambiar este catálogo se aplica a nuevas selecciones.</p>
          {catalog.kind === 'MEANS' && <label>Formato de evidencia<select value={catalog.evidenceKind} onChange={e => setCatalog({ ...catalog, evidenceKind: e.target.value as 'PDF' | 'URL' })}><option value="PDF">Archivo PDF privado</option><option value="URL">Enlace HTTPS externo</option></select></label>}
          <label>Denominación del catálogo<input required maxLength={200} value={catalog.label} onChange={e => setCatalog({ ...catalog, label: e.target.value })} /></label>
          <label className="check"><input type="checkbox" checked={catalog.active} onChange={e => setCatalog({ ...catalog, active: e.target.checked })} />Elemento activo</label>
          <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
            <button>{catalog.id ? 'Actualizar elemento' : 'Crear elemento'}</button>
            {catalog.id && <button type="button" className="secondary" onClick={async () => { if ((await discard(['catalog']))) setCatalog({ id: '', kind: 'RESOURCE', evidenceKind: 'PDF', label: '', active: true }) }}>Cancelar edición de elemento</button>}
          </div>
        </form>
      </ActionForm>
      {catalogs.length === 0 ? (
        <div className="empty-state" style={{ margin: '20px 0' }}>
          <Icon name="document" size={32} />
          <p>No hay recursos ni medios configurados. Use "Crear o editar elemento" para registrar uno.</p>
        </div>
      ) : (
        <ul className="admin-catalog-list">
          {catalogs.map(c => <li key={c.id} className="admin-catalog-item">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
              <span className={`status-badge ${c.kind === 'RESOURCE' ? 'active' : 'pending'}`}>
                {c.kind === 'RESOURCE' ? 'Recurso' : 'Medio'}
              </span>
              <strong>{c.label}</strong>
              {c.kind === 'MEANS' && <span className="muted">· {c.evidenceKind === 'URL' ? 'Enlace HTTPS' : 'PDF'}</span>}
              {!c.active && <span className="status-badge inactive">Inactivo</span>}
            </div>
            <button
              className="secondary table-action-icon-btn"
              title="Editar elemento"
              aria-label={'Editar elemento ' + c.label}
              onClick={async () => { if ((await discard(['catalog']))) setCatalog({ ...c, evidenceKind: c.evidenceKind ?? 'PDF' }) }}
            >
              <Icon name="edit" size={15} />
            </button>
          </li>)}
        </ul>
      )}
    </section>}
    {section === 'activities' && <section>
      <div className="section-heading">
        <div className="section-heading-icon" aria-hidden="true"><Icon name="settings" size={24} /></div>
        <div>
          <span className="section-heading-kicker">PLANIFICACIÓN DOCENTE</span>
          <h3>Actividades por grupo</h3>
          <p className="muted">Configure las actividades POA o institucionales y la obligatoriedad para cada grupo.</p>
        </div>
      </div>
      <div className="filter-toolbar" style={{ margin: '16px 0' }}>
        <label>Grupo del catálogo
          <select value={groupId} onChange={async e => { const nextValue = e.currentTarget.value; if (!(await discard(['activity', 'collective', 'reference', 'proposals']))) return; setGroupId(nextValue); setDefinitions([]); setLabel(groups.find(g => g.id === nextValue)?.collective_label ?? ''); setActivity({ id: '', title: '', category: 'POA', mandatory: false, active: true }) }}>
            <option value="">Seleccione</option>
            {groups.map(g => <option key={g.id} value={g.id}>{g.name}</option>)}
          </select>
        </label>
      </div>
      {!groupId ? (
        <div className="empty-state" style={{ margin: '24px 0 12px' }}>
          <Icon name="settings" size={32} />
          <p>Seleccione un grupo institucional para consultar y configurar sus actividades obligatorias y del catálogo.</p>
        </div>
      ) : (
        <>
          <ActivityProposalReview key={'proposals-' + groupId} groupId={groupId} periods={periods} onDirtyChange={proposalsDirty} onSaved={async () => setDefinitions(await api('/admin/planning/groups/' + groupId + '/activities'))} />
          <ActivityReferenceImport key={groupId} groupId={groupId} groups={groups} periods={periods} onDirtyChange={referenceDirty} onSaved={async () => setDefinitions(await api('/admin/planning/groups/' + groupId + '/activities'))} />
          <ActionForm label="Crear o editar actividad" openSignal={activity.id || undefined} dirty={dirtyForms.includes('activity')} disabled={busy} onCancel={() => { setDirtyForms(keys => keys.filter(k => k !== 'activity')); setActivity({ id: '', title: '', category: 'POA', mandatory: false, active: true }) }}>
            <form onChange={() => markDirty('activity')} onSubmit={saveActivity}>
              <label>Título de actividad del catálogo<input required maxLength={500} value={activity.title} onChange={e => setActivity({ ...activity, title: e.target.value })} /></label>
              <label>Clasificación de actividad<select value={activity.category} onChange={e => setActivity({ ...activity, category: e.target.value as Category })}>{Object.entries(categoryLabels).map(([id, label]) => <option value={id} key={id}>{label}</option>)}</select></label>
              <div className="grid">{([{ key: 'objective', label: 'Objetivo estratégico', max: 4000 }, { key: 'criterion', label: 'Criterio', max: 1000 }, { key: 'indicator', label: 'Indicador', max: 1000 }, { key: 'strategy', label: 'Estrategia', max: 1000 }] as const).map(field => <label key={field.key}>{field.label}<textarea maxLength={field.max} rows={2} value={activity.context?.[field.key] ?? ''} onChange={e => setActivity({ ...activity, context: { ...activity.context, [field.key]: e.currentTarget.value } })} /></label>)}</div>
              {activity.context?.sourceFile && <p className="field-help">Origen: {activity.context.sourceFile}</p>}
              <label className="check"><input type="checkbox" checked={activity.mandatory} onChange={e => setActivity({ ...activity, mandatory: e.target.checked })} />Obligatoria para este grupo</label>
              <label className="check"><input type="checkbox" checked={activity.active} onChange={e => setActivity({ ...activity, active: e.target.checked })} />Actividad activa</label>
              <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
                <button>{activity.id ? 'Actualizar actividad' : 'Crear actividad del catálogo'}</button>
                {activity.id && <button type="button" className="secondary" onClick={async () => { if ((await discard(['activity']))) setActivity({ id: '', title: '', category: 'POA', mandatory: false, active: true }) }}>Cancelar edición de actividad</button>}
              </div>
            </form>
          </ActionForm>
          {definitions.length === 0 ? (
            <div className="empty-state" style={{ margin: '16px 0' }}>
              <Icon name="document" size={32} />
              <p>No hay actividades configuradas para este grupo institucional.</p>
            </div>
          ) : (
            <ul className="admin-catalog-list">
              {definitions.map(d => <li key={d.id} className="admin-catalog-item">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                  <strong>{d.title}</strong>
                  <span className="muted">· {categoryLabels[d.category]}</span>
                  {d.mandatory && <span className="status-badge active">Obligatoria</span>}
                  {!d.active && <span className="status-badge inactive">Inactiva</span>}
                </div>
                <button
                  className="secondary table-action-icon-btn"
                  title="Editar actividad"
                  aria-label={'Editar actividad ' + d.title}
                  onClick={async () => { if ((await discard(['activity']))) setActivity(d) }}
                >
                  <Icon name="edit" size={15} />
                </button>
              </li>)}
            </ul>
          )}
          <ActionForm label="Editar denominación colectiva" dirty={dirtyForms.includes('collective')} disabled={busy} onCancel={() => { setDirtyForms(keys => keys.filter(k => k !== 'collective')); setLabel(groups.find(g => g.id === groupId)?.collective_label ?? '') }}>
            <form onChange={() => markDirty('collective')} onSubmit={e => { e.preventDefault(); void mutate('/admin/planning/groups/' + groupId + '/collective-label', 'PUT', { label }, 'collective') }}>
              <label>Denominación colectiva del grupo<input maxLength={200} value={label} onChange={e => setLabel(e.target.value)} /></label>
              <button>Guardar denominación colectiva</button>
            </form>
          </ActionForm>
        </>
      )}
    </section>}</div>
    {section === 'holidays' && <section>
      <div className="section-heading">
        <div className="section-heading-icon" aria-hidden="true"><Icon name="calendar" size={24} /></div>
        <div>
          <span className="section-heading-kicker">CALENDARIO Y RESTRICCIONES</span>
          <h3>Feriados y restricciones</h3>
          <p className="muted">Configure fechas no laborables y restricciones de inicio o fin de actividades por período.</p>
        </div>
      </div>
      <HolidaySuggestions existing={holidays.map(h => h.date)} onSaved={load} onDirtyChange={suggestionsDirty} />
      <ActionForm label="Agregar feriado" dirty={dirtyForms.includes('holiday')} disabled={busy} onCancel={() => setDirtyForms(keys => keys.filter(k => k !== 'holiday'))}>
        <form className="grid" onChange={() => markDirty('holiday')} onSubmit={e => { e.preventDefault(); const form = e.currentTarget; const data = Object.fromEntries(new FormData(form)); void mutate('/admin/planning/holidays', 'POST', data, 'holiday').then(ok => { if (ok) form.reset() }) }}>
          <label>Fecha del feriado<input required type="date" name="date" /></label>
          <label>Nombre del feriado<input required maxLength={200} name="label" /></label>
          <button>Guardar feriado</button>
        </form>
      </ActionForm>
      {holidays.length === 0 ? (
        <div className="empty-state" style={{ margin: '16px 0' }}>
          <Icon name="calendar" size={32} />
          <p>No hay feriados institucionales registrados.</p>
        </div>
      ) : (
        <ul className="admin-catalog-list">
          {holidays.map(h => <li key={h.date} className="admin-catalog-item">
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span className="position-code-badge" style={{ fontFamily: 'var(--font-mono, monospace)' }}>{h.date.split('-').reverse().join('/')}</span>
              <strong>{h.label}</strong>
            </div>
            <button
              title="Eliminar feriado"
              aria-label={'Eliminar feriado ' + h.label}
              className="secondary danger-text"
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
              onClick={async () => { if ((await confirmAction('¿Eliminar este feriado configurado?'))) void mutate('/admin/planning/holidays/' + h.date, 'DELETE') }}
            >
              <Icon name="trash" size={14} /> Eliminar
            </button>
          </li>)}
        </ul>
      )}
      <ActionForm label="Configurar restricción de feriados" dirty={dirtyForms.includes('policy')} disabled={busy} onCancel={() => { setDirtyForms(keys => keys.filter(k => k !== 'policy')); setHolidayPeriod(''); setHolidayEnabled(false) }}>
        <form className="grid" onChange={() => markDirty('policy')} onSubmit={e => { e.preventDefault(); void mutate('/admin/planning/periods/' + holidayPeriod + '/holiday-policy', 'PUT', { enabled: holidayEnabled }, 'policy') }}>
          <label>Período de la restricción<select required value={holidayPeriod} onChange={e => { setHolidayPeriod(e.target.value); setHolidayEnabled(periods.find(p => p.id === e.target.value)?.restrict_holiday_endpoints ?? false) }}><option value="">Seleccione</option>{periods.map(p => <option key={p.id} value={p.id}>{p.name}{p.restrict_holiday_endpoints ? ' · Restricción activa' : ' · Sin restricción'}</option>)}</select></label>
          <label className="check"><input type="checkbox" checked={holidayEnabled} onChange={e => setHolidayEnabled(e.target.checked)} />Impedir inicio o fin en feriados</label>
          <button>Guardar restricción de feriados</button>
        </form>
      </ActionForm>
    </section>}
    </fieldset>
  </section>
}
