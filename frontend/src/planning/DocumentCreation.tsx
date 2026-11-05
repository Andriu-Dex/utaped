import { confirmAction } from '../shared/confirm-action'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import type { PlanOptions, WorkPlan } from './types'
import './document-creation.css'

type ReportOption = { id: string; title: string; groupName: string; periodName: string; formalVersion: string; suggestedTitle: string; reportId: string | null }
type ReportOptions = { items: ReportOption[]; total: number; page: number; size: number }
type ExecutionOption = { id: string; title: string; groupName: string; periodName: string; formalVersion: string; reports: number; draftId: string | null }
const roman = (n: number) => [[10, 'X'], [9, 'IX'], [5, 'V'], [4, 'IV'], [1, 'I']].reduce((text, [value, symbol]) => { let result = text; while (n >= (value as number)) { result += symbol; n -= value as number } return result }, '')
export function DocumentCreation({ options, busy, onBusyChange, onCreated, onDirtyChange }: { options: PlanOptions; busy: boolean; onBusyChange: (busy: boolean) => void; onCreated: (document: WorkPlan) => void; onDirtyChange: (dirty: boolean) => void }) {
  const [kind, setKind] = useState<'T1' | 'T2' | 'T3'>(() => options.scopes.some(scope => scope.canCreate) ? 'T1' : 'T2'), [executionReload, setExecutionReload] = useState(0)
  const [executionOptions, setExecutionOptions] = useState<ExecutionOption[] | null>(null), [executionBaseId, setExecutionBaseId] = useState('')
  const [scopeId, setScopeId] = useState(''); const [titleOverride, setTitleOverride] = useState<string | null>(null)
  const [scopeSearch, setScopeSearch] = useState('')
  const [comboboxOpen, setComboboxOpen] = useState(false)
  const [highlightIndex, setHighlightIndex] = useState(0)
  const comboboxRef = useRef<HTMLDivElement>(null)
  const [requestKey, setRequestKey] = useState(() => crypto.randomUUID())
  const [reportId, setReportId] = useState(''); const [candidates, setCandidates] = useState<ReportOptions | null>(null)
  const [search, setSearch] = useState(''); const [filter, setFilter] = useState({ query: '', page: 0 })
  const [loading, setLoading] = useState(!options.scopes.some(scope => scope.canCreate)); const [error, setError] = useState('')
  const scope = options.scopes.find(s => s.id === scopeId && s.canCreate)
  const availableScopes = options.scopes.filter(s => s.canCreate)
  const filteredScopes = availableScopes.filter(s => {
    if (!scopeSearch.trim()) return true
    const q = scopeSearch.toLowerCase()
    return s.groupName.toLowerCase().includes(q) ||
      (s.careerName != null && s.careerName.toLowerCase().includes(q)) ||
      (s.institutionalUnit != null && s.institutionalUnit.toLowerCase().includes(q))
  })
  const report = candidates?.items.find(p => p.id === reportId)
  const executionBase = executionOptions?.find(p => p.id === executionBaseId)
  const dirty = kind === 'T1' ? !!scopeId || titleOverride !== null : kind === 'T2' ? !!reportId : !!executionBaseId
  useEffect(() => { onDirtyChange(dirty) }, [dirty, onDirtyChange])

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (comboboxRef.current && !comboboxRef.current.contains(e.target as Node)) {
        setComboboxOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])
  useEffect(() => {
    const controller = new AbortController()
    if (kind !== 'T2') return
    let active = true
    api<ReportOptions>('/reports/options?' + new URLSearchParams({ query: filter.query, page: String(filter.page), size: '20' }), 'GET', undefined, controller.signal).then(data => {
      if (active) { setCandidates(data); setLoading(false) }
    }).catch(e => { if (active) { setError(e.message); setLoading(false) } })
    return () => { active = false; controller.abort() }
  }, [kind, filter])
  useEffect(() => {
    if (kind !== 'T3') return
    let active = true
    api<ExecutionOption[]>('/execution-reports/options').then(data => { if (active) { setExecutionOptions(data); setLoading(false) } }).catch(e => { if (active) { setError(e.message); setLoading(false) } })
    return () => { active = false }
  }, [kind, executionReload])
  async function switchKind(next: typeof kind) {
    if (kind === next || busy) return
    if (dirty && !(await confirmAction('¿Descartar los datos de creación para cambiar de tipo documental?'))) return
    setScopeId(''); setTitleOverride(null); setScopeSearch(''); setComboboxOpen(false); setHighlightIndex(0); setReportId(''); setExecutionBaseId(''); setError(''); setRequestKey(crypto.randomUUID()); setKind(next)
    if (next === 'T2') { setCandidates(null); setLoading(true); setSearch(''); setFilter({ query: '', page: 0 }) }
    if (next === 'T3') { setExecutionOptions(null); setLoading(true) }
  }
  function searchPlans(page = 0, query = search) { setLoading(true); setReportId(''); setError(''); setFilter({ query, page }) }
  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); onBusyChange(true)
    try {
      if (kind === 'T1' && scope) onCreated(await api<WorkPlan>('/work-plans', 'POST', { scopeId: scope.id, groupId: scope.groupId, periodId: scope.periodId, title: titleOverride, requestKey }))
      else if (kind === 'T2' && report) onCreated(await api<WorkPlan>('/work-plans/' + report.id + '/report', 'POST'))
      else if (kind === 'T3' && executionBase) onCreated(await api<WorkPlan>('/work-plans/' + executionBase.id + '/execution-report', 'POST'))
    } catch (e) { setError(e instanceof TypeError ? 'No se pudo conectar con el servidor.' : e instanceof Error ? e.message : 'No se pudo crear el documento.') }
    finally { onBusyChange(false) }
  }
  return <section className="card animate-reveal document-creation">
    <div className="section-heading"><span className="section-icon"><Icon name="document" size={20} /></span><div><span className="section-kicker">NUEVO DOCUMENTO</span><h2>¿Qué documento desea crear?</h2></div></div>
    <p className="muted">Seleccione el formato y su ámbito institucional. El nombre se completa automáticamente.</p>
    <div className="document-kind-options" role="group" aria-label="Tipo de documento">
      {([['T1', 'Plan de Trabajo', 'Organice objetivos, actividades y responsables.', 'UTA-SGC-A-2-1-P7-T1'], ['T3', 'Informe de ejecución', 'Reporte el avance de un corte del Plan aprobado.', 'UTA-SGC-A-1-1-P4-T2'], ['T2', 'Informe final', 'Cierre el Plan aprobado con sus resultados.', 'UTA-SGC-A-2-1-P7-T2']] as const).map(([type, label, help, format]) => <label key={type} className={'document-kind-card ' + (kind === type ? 'selected' : '')}>
        <input type="radio" name="document-kind" value={type} checked={kind === type} disabled={busy || (type === 'T1' ? !options.scopes.some(scope => scope.canCreate) : !options.canCreateDerived)} onChange={() => switchKind(type)} />
        <span className="document-kind-icon"><Icon name={type === 'T1' ? 'calendar' : 'document'} size={20} /></span>
        <span><strong>{label}</strong><small>{help}</small><span className="document-kind-format">{format}</span></span>
      </label>)}
    </div>
    {error && <><Toast message={error} />{kind === 'T3' && <button type="button" className="secondary" disabled={busy || loading} onClick={() => { setError('');setLoading(true);setExecutionOptions(null);setExecutionReload(n => n + 1) }}>Reintentar carga de Planes</button>}</>}
    {kind === 'T3' ? loading ? <p role="status">Cargando Planes aprobados…</p> : executionOptions?.length ? <form onSubmit={create}><fieldset disabled={busy} className="document-create-fields">
        <label>Plan base del Informe de ejecución<select aria-label="Plan base del Informe de ejecución" required value={executionBaseId} onChange={e => setExecutionBaseId(e.target.value)}><option value="">Seleccione un Plan aprobado</option>{executionOptions.map(p => <option key={p.id} value={p.id}>{p.title} · {p.periodName} · {p.reports} informe(s) de ejecución{p.draftId ? ' · Borrador abierto' : ''}</option>)}</select></label>
        <label>Nombre del Informe<input readOnly placeholder="Se completa al seleccionar el Plan" value={executionBase ? 'Informe de ejecución ' + roman(executionBase.reports + (executionBase.draftId ? 0 : 1)) + ' — ' + executionBase.groupName : ''} /><small className="field-help">Cada Informe de ejecución reporta las actividades ejecutadas en un corte del Plan.{executionBase?.draftId && ' Hay un borrador en curso: se abrirá ese documento.'}</small></label>
        <button disabled={busy || !executionBase}><Icon name="document" size={17} />{busy ? 'Procesando…' : executionBase?.draftId ? 'Abrir borrador' : 'Crear Informe de ejecución'}</button>
      </fieldset></form> : executionOptions && <div className="empty-state"><Icon name="document" size={28} /><p>Aún no tiene un Plan aprobado disponible para reportar su ejecución.</p><small>Requiere un Plan vigente del que sea responsable principal, dentro de las fechas del período.</small></div> : kind === 'T1' ? !availableScopes.length ? <div className="empty-state"><Icon name="users" size={28} /><p>No tiene comisiones disponibles para crear un Plan.</p><small>Requiere designación como responsable principal, ventana abierta y un ámbito sin otro Plan activo.</small></div> :
      <form onSubmit={create}><fieldset disabled={busy} className="document-create-fields">
        <div className="scope-selection-col">
          <div className="scope-col-label">
            <label htmlFor="scope-combobox-input">Comisión o unidad asignada</label>
            <span className="scope-count-tag">{availableScopes.length} disponibles</span>
          </div>
          <div className="scope-combobox-wrapper" ref={comboboxRef}>
            <div className={'scope-combobox-control' + (comboboxOpen ? ' is-open' : '') + (scope ? ' is-selected' : '')}>
              <span className="scope-combobox-icon"><Icon name="search" size={16} /></span>
              <input
                id="scope-combobox-input"
                type="text"
                role="combobox"
                aria-expanded={comboboxOpen}
                aria-controls="scope-combobox-list"
                aria-autocomplete="list"
                aria-activedescendant={comboboxOpen && filteredScopes[highlightIndex] ? 'scope-option-' + filteredScopes[highlightIndex].id : undefined}
                placeholder={scope ? scope.groupName : 'Buscar o seleccionar comisión asignada…'}
                value={comboboxOpen ? scopeSearch : (scope ? scope.groupName : scopeSearch)}
                disabled={busy}
                onFocus={() => { setComboboxOpen(true); setHighlightIndex(0) }}
                onChange={e => {
                  setScopeSearch(e.target.value)
                  setScopeId('')
                  setTitleOverride(null)
                  setComboboxOpen(true)
                  setHighlightIndex(0)
                }}
                onKeyDown={e => {
                  if (!comboboxOpen) {
                    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                      e.preventDefault(); setComboboxOpen(true)
                    }
                    return
                  }
                  if (e.key === 'Escape') {
                    e.preventDefault(); setComboboxOpen(false)
                  } else if (e.key === 'ArrowDown') {
                    e.preventDefault()
                    setHighlightIndex(i => (i + 1) % Math.max(1, filteredScopes.length))
                  } else if (e.key === 'ArrowUp') {
                    e.preventDefault()
                    setHighlightIndex(i => (i - 1 + filteredScopes.length) % Math.max(1, filteredScopes.length))
                  } else if (e.key === 'Enter') {
                    e.preventDefault()
                    if (filteredScopes[highlightIndex]) {
                      const selected = filteredScopes[highlightIndex]
                      setScopeId(selected.id)
                      setTitleOverride(null)
                      setRequestKey(crypto.randomUUID())
                      setScopeSearch('')
                      setComboboxOpen(false)
                    }
                  }
                }}
              />
              {scope && !comboboxOpen ? (
                <button
                  type="button"
                  className="scope-combobox-btn scope-combobox-clear"
                  title="Cambiar comisión"
                  aria-label="Cambiar comisión"
                  onClick={() => {
                    setScopeId('')
                    setTitleOverride(null)
                    setScopeSearch('')
                    setComboboxOpen(true)
                  }}
                >
                  <Icon name="x" size={14} />
                </button>
              ) : (
                <button
                  type="button"
                  className={'scope-combobox-btn scope-combobox-chevron' + (comboboxOpen ? ' is-rotated' : '')}
                  title={comboboxOpen ? 'Cerrar opciones' : 'Abrir opciones'}
                  aria-label={comboboxOpen ? 'Cerrar opciones' : 'Abrir opciones'}
                  onClick={() => setComboboxOpen(o => !o)}
                >
                  <Icon name="chevronDown" size={16} />
                </button>
              )}
            </div>

            {comboboxOpen && (
              <ul id="scope-combobox-list" role="listbox" className="scope-combobox-dropdown">
                {filteredScopes.length === 0 ? (
                  <li className="scope-combobox-empty">No hay comisiones que coincidan con «{scopeSearch}»</li>
                ) : (
                  filteredScopes.map((s, idx) => (
                    <li
                      key={s.id}
                      id={'scope-option-' + s.id}
                      role="option"
                      aria-selected={s.id === scopeId}
                      className={'scope-combobox-item' + (s.id === scopeId ? ' is-selected' : '') + (idx === highlightIndex ? ' is-highlighted' : '')}
                      onMouseEnter={() => setHighlightIndex(idx)}
                      onClick={() => {
                        setScopeId(s.id)
                        setTitleOverride(null)
                        setRequestKey(crypto.randomUUID())
                        setScopeSearch('')
                        setComboboxOpen(false)
                      }}
                    >
                      <div className="scope-combobox-item-header">
                        <strong>{s.groupName}</strong>
                        {s.id === scopeId && <span className="scope-combobox-check"><Icon name="check" size={14} /></span>}
                      </div>
                      <small>{s.careerName ?? s.institutionalUnit ?? 'Facultad'} · {s.periodName}</small>
                    </li>
                  ))
                )}
              </ul>
            )}
          </div>
          {scope && (
            <div className="scope-locked-metadata">
              <div className="scope-metadata-badges-row">
                <span className="scope-metadata-pill">
                  <Icon name="users" size={13} />
                  <span><strong>Carrera:</strong> {scope.careerName ?? scope.institutionalUnit ?? 'Facultad'}</span>
                </span>
                <span className="scope-metadata-pill">
                  <Icon name="calendar" size={13} />
                  <span><strong>Período:</strong> {scope.periodName}</span>
                </span>
              </div>
              <small className="scope-metadata-note">Parámetros oficiales configurados por la administración institucional.</small>
            </div>
          )}
        </div>
        <div className="scope-title-col">
          <div className="scope-col-label">
            <label htmlFor="plan-title-input">Título del nuevo Plan</label>
          </div>
          <input
            id="plan-title-input"
            required
            maxLength={200}
            disabled={!scope || busy}
            placeholder="Se autocompleta al elegir la comisión"
            value={titleOverride ?? scope?.suggestedTitle ?? ''}
            onChange={e => { setTitleOverride(e.target.value); setRequestKey(crypto.randomUUID()) }}
          />
          <small className="field-help">Nombre oficial generado automáticamente según la comisión. Puede ajustarlo antes de crear el borrador.</small>
        </div>
        <button type="submit" className="document-create-submit-btn" disabled={busy || !scope || (titleOverride !== null && !titleOverride.trim())}>
          <Icon name="document" size={17} />
          {busy ? 'Creando…' : 'Crear borrador'}
        </button>
      </fieldset></form> : <>
      <form className="document-base-search" onSubmit={e => { e.preventDefault(); searchPlans() }}><label>Buscar Plan aprobado<input maxLength={200} value={search} disabled={busy} placeholder="Título, grupo o período" onChange={e => { const value = e.currentTarget.value; setSearch(value); setCandidates(null); setReportId(''); setLoading(true); setFilter({ query: value, page: 0 }) }} /></label><button type="submit" className="secondary" disabled={busy || loading}><Icon name="search" size={17} />Buscar Plan</button></form>
      {loading ? <p role="status">Cargando Planes aprobados…</p> : candidates?.items.length ? <form onSubmit={create}><fieldset disabled={busy} className="document-create-fields">
        <label>Plan base del Informe<select required value={reportId} onChange={e => setReportId(e.target.value)}><option value="">Seleccione un Plan aprobado</option>{candidates.items.map(p => <option key={p.id} value={p.id}>{p.title} · {p.periodName} · Versión {p.formalVersion}{p.reportId ? ' · Informe existente' : ''}</option>)}</select></label>
        <label>Nombre del Informe<input readOnly placeholder="Se completa al seleccionar el Plan" value={report?.suggestedTitle ?? ''} /><small className="field-help">Se mantienen la relación, actividades y datos del Plan fuente.{report?.reportId && ' Ya existe un Informe: se abrirá ese documento.'}</small></label>
        <button disabled={busy || !report}><Icon name="document" size={17} />{busy ? 'Procesando…' : report?.reportId ? 'Abrir Informe' : 'Crear Informe'}</button>
      </fieldset></form> : candidates && <div className="empty-state"><Icon name="document" size={28} /><p>{filter.query ? 'No hay Planes aprobados que coincidan con la búsqueda.' : 'Aún no tiene un Plan aprobado disponible para elaborar su Informe.'}</p><small>El Informe Final requiere un Plan vigente del que sea responsable principal.</small></div>}
      {!!candidates?.total && !loading && <div className="pagination"><span>{candidates.total} Planes · Página {candidates.page + 1}</span><button className="secondary" disabled={busy || candidates.page === 0} onClick={() => searchPlans(candidates.page - 1, filter.query)}>Anterior</button><button className="secondary" disabled={busy || (candidates.page + 1) * candidates.size >= candidates.total} onClick={() => searchPlans(candidates.page + 1, filter.query)}>Siguiente</button></div>}
    </>}
  </section>
}
