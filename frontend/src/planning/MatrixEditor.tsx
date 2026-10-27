import { StepPagination } from './StepPagination'
import { usePlanStepGuard } from './PlanStepNavigation'
import { confirmAction } from '../shared/confirm-action'
import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, ApiError } from '../shared/api'
import { categoryLabels, type Activity, type Matrix } from './matrixTypes'
import { Icon } from '../shared/Icon'
import { containDialogFocus } from '../shared/dialog-focus'

function selection(matrix: Matrix) { return { source: matrix.source, activities: matrix.activities } }
export function MatrixEditor({ targetDocId, onBack, onPrevious, onNext, onDirtyChange, onSaved }: { onSaved?: () => Promise<void>; onPrevious?: () => Promise<void>; targetDocId: string; onBack: () => Promise<void>; onNext: () => Promise<void>; onDirtyChange: (dirty: boolean) => void }) {
  const [saved, setSaved] = useState<Matrix | null>(null); const [draft, setDraft] = useState<ReturnType<typeof selection> | null>(null)
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const [conflict, setConflict] = useState(false); const [latest, setLatest] = useState<Matrix | null>(null)
  const [catalogId, setCatalogId] = useState(''); const [otherTitle, setOtherTitle] = useState('')
  const [editingActivityId, setEditingActivityId] = useState<string | null>(null)
  const [addKey, setAddKey] = useState(0)
  const [attempted, setAttempted] = useState(false)
  const formRef = useRef<HTMLFormElement>(null)
  const activityDialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const dialog = activityDialog.current
    if (!editingActivityId || !dialog) return
    const origin = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const form = formRef.current
    const overflow = document.body.style.overflow
    dialog.showModal(); document.body.style.overflow = 'hidden'
    dialog.querySelector<HTMLElement>('[id^="activity-"]')?.focus()
    const release = containDialogFocus(dialog)
    return () => {
      release(); dialog.close(); document.body.style.overflow = overflow
      requestAnimationFrame(() => {
        const target = origin?.isConnected ? origin : form?.querySelector<HTMLElement>('button[aria-expanded]')
        target?.focus()
      })
    }
  }, [editingActivityId])
  const dirty = !!catalogId || !!otherTitle || !!saved && !!draft && JSON.stringify(selection(saved)) !== JSON.stringify(draft)
  useEffect(() => { onDirtyChange(dirty) }, [dirty, onDirtyChange])
  useEffect(() => () => onDirtyChange(false), [onDirtyChange])
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => { if (dirty) { e.preventDefault(); e.returnValue = '' } }
    window.addEventListener('beforeunload', handler); return () => window.removeEventListener('beforeunload', handler)
  }, [dirty])
  useEffect(() => {
    let cancelled = false
    api<Matrix>('/work-plans/' + targetDocId + '/matrix').then(m => { if (!cancelled) { setSaved(m); setDraft(selection(m)) } }).catch(e => { if (!cancelled) setError(e.message) })
    return () => { cancelled = true }
  }, [targetDocId])
  function update(id: string, patch: Partial<Activity>) { setDraft(d => d && ({ ...d, activities: d.activities.map(a => a.id === id ? { ...a, ...patch } : a) })); setNotice('') }
  function selectResponsible(row: Activity, responsibleIds: string[]) {
    update(row.id, { responsibleIds, collective: !!saved?.collectiveLabel && !!saved.members.length && saved.members.every(member => responsibleIds.includes(member.id)) && responsibleIds.length === saved.members.length })
  }
  function add() {
    if (!saved || !draft) return
    const definition = saved.definitions.find(d => d.id === catalogId)
    if (!definition && !otherTitle.trim()) { setError('Escriba el título de la actividad Otra.'); return }
    const activity: Activity = { id: crypto.randomUUID(), catalogId: definition?.id ?? null, title: definition?.title ?? otherTitle.trim(), category: definition?.category ?? 'OTHER', mandatory: definition?.mandatory ?? false, startsOn: null, endsOn: null, responsibleIds: [], collective: false, resources: [], means: [] }
    setDraft({ ...draft, activities: [...draft.activities, activity] })
    setCatalogId('')
    setOtherTitle('')
    setAddKey(k => k + 1)
    setEditingActivityId(activity.id)
    setError('')
    setNotice('')
    requestAnimationFrame(() => {
      document.getElementById('activity-' + activity.id)?.focus()
    })
  }
  function toggleChoice(row: Activity, key: 'resources' | 'means', id: string | null, checked: boolean) {
    const values = row[key].filter(c => c.catalogId !== id)
    if (checked) values.push({ catalogId: id, other: id === null ? '' : null, label: null })
    update(row.id, { [key]: values })
  }
  async function persist(): Promise<boolean> {
    if (!saved || !draft) return false
    if (catalogId || otherTitle) { setError('Agregue la actividad seleccionada o cancele su creación antes de guardar y continuar.'); return false }
    setAttempted(true); setError(''); setNotice('')
    const missingDates = draft.activities.find(a => !a.startsOn || !a.endsOn || a.startsOn > a.endsOn)
    if (missingDates) { setError('Complete fechas válidas antes de guardar la matriz.'); showActivity(missingDates.id); return false }
    const emptyOther = formRef.current?.querySelector<HTMLInputElement>('input[data-other="true"][aria-invalid="true"]')
    if (emptyOther) { setError('Complete la descripción de Otro.'); emptyOther.focus(); return false }
    if (draft.activities.some(a => !a.responsibleIds.length || !a.resources.length || !a.means.length)) { setError('Cada actividad requiere al menos un responsable, un recurso y un medio de verificación.'); return false }
    setBusy(true)
    try {
      const m = await api<Matrix>('/work-plans/' + targetDocId + '/matrix', 'PUT', { ...draft, rowVersion: saved.rowVersion })
      setSaved(m); setDraft(selection(m)); await onSaved?.(); setNotice('Matriz guardada en el servidor.'); setAttempted(false); setEditingActivityId(null); return true
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.'); if (e instanceof ApiError && e.status === 409) setConflict(true); return false }
    finally { setBusy(false) }
  }
  async function save(e: FormEvent) { e.preventDefault(); await persist() }
  async function canContinue(): Promise<boolean> {
    if (!saved || !draft || busy || conflict) return false
    if (!saved.editable) return true
    setError(''); setNotice('')
    if (!draft.activities.length) {
      setError('Agregue al menos una actividad para continuar.')
      formRef.current?.querySelector<HTMLButtonElement>('button[aria-expanded]')?.focus()
      return false
    }
    setAttempted(true)
    const incomplete = draft.activities.find(a => !a.title.trim() || !a.startsOn || !a.endsOn ||
      a.startsOn > a.endsOn || a.startsOn < saved.periodStartsOn || a.endsOn > saved.periodEndsOn ||
      !a.responsibleIds.length || a.responsibleIds.some(id => !saved.members.some(member => member.id === id)) ||
      !a.resources.length || !a.means.length || [...a.resources, ...a.means].some(c => c.catalogId === null && !c.other?.trim()) ||
      saved.restrictHolidayEndpoints && saved.holidays.some(h => h.date === a.startsOn || h.date === a.endsOn))
    if (incomplete) {
      setError('Complete fechas válidas, responsables, recursos y medios de verificación de la actividad ' + (draft.activities.indexOf(incomplete) + 1) + ' para continuar.')
      showActivity(incomplete.id)
      return false
    }
    if (formRef.current && !formRef.current.checkValidity()) {
      setError('Revise los campos obligatorios de la matriz antes de continuar.')
      formRef.current.querySelector<HTMLElement>(':invalid')?.focus()
      return false
    }
    if (dirty && !(await persist())) return false
    return true
  }
  const embedded = usePlanStepGuard(async target => {
    if (busy) return false
    if (!saved || !draft) return ['general', 'content'].includes(target)
    if (['attachments', 'preparation', 'signing'].includes(target)) return canContinue()
    return !dirty || await confirmAction('Hay cambios sin guardar en la matriz. ¿Cambiar de paso y descartarlos?')
  }, busy || (!saved && !error))
  async function next() {
    if (!(await canContinue())) return
    setBusy(true)
    try { await onNext() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo abrir Anexos.'); setBusy(false) }
  }
  function showActivity(id: string) {
    setEditingActivityId(id)
    requestAnimationFrame(() => {
      document.getElementById('activity-' + id)?.focus()
    })
  }
  async function back() {
    if (dirty && !(await confirmAction('Hay cambios sin guardar en la matriz. ¿Salir y descartarlos?'))) return
    setBusy(true)
    try { await onBack() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo volver.'); setBusy(false) }
  }
  async function previous() {
    if (busy || (dirty && !(await confirmAction('Hay cambios sin guardar en la matriz. ¿Volver al contenido y descartarlos?')))) return
    setBusy(true)
    try { await (onPrevious ?? onBack)() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo cambiar de paso.'); setBusy(false) }
  }
  async function compare() { try { setLatest(await api<Matrix>('/work-plans/' + targetDocId + '/matrix')) } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo consultar.') } }
  async function reload() {
    if (!latest || !(await confirmAction('¿Descartar cambios locales y cargar la matriz actual del servidor?'))) return
    setEditingActivityId(null); setSaved(latest); setDraft(selection(latest)); setLatest(null); setConflict(false); setAttempted(false); setError(''); setNotice('Versión del servidor cargada.')
  }
  if (!saved || !draft) return <section>{!embedded && <button className="secondary" onClick={back}>Volver al Plan</button>}{error ? <Toast message={error} /> : <p role="status">Cargando matriz…</p>}</section>
  function choices(row: Activity, key: 'resources' | 'means', title: string) {
    if (!saved) return null
    const available = saved.catalogs.filter(c => c.kind === (key === 'resources' ? 'RESOURCE' : 'MEANS') && (c.active || row[key].some(v => v.catalogId === c.id)))
    const other = row[key].find(c => c.catalogId === null)
    const count = row[key].filter(c => c.catalogId !== null || c.other?.trim()).length

    return (
      <fieldset className="choice-group" aria-label={title}>
        <div className="choice-group-header">
          <span className="choice-group-title">{title}</span>
          <span className={'choice-count-badge' + (count > 0 ? ' has-selected' : '')}>
            {count} {count === 1 ? 'seleccionado' : 'seleccionados'}
          </span>
        </div>
        <div className="choice-chip-grid">
          {available.map(c => {
            const isSelected = row[key].some(v => v.catalogId === c.id)
            const resolvedLabel = row[key].find(v => v.catalogId === c.id)?.label || c.label
            const evidenceKind = (row[key].find(v => v.catalogId === c.id)?.evidenceKind ?? c.evidenceKind)
            return (
              <label
                className={'choice-chip' + (isSelected ? ' is-selected' : '')}
                key={c.id}
                title={resolvedLabel}
              >
                <input
                  type="checkbox"
                  aria-label={resolvedLabel}
                  checked={isSelected}
                  onChange={e => toggleChoice(row, key, c.id, e.target.checked)}
                />
                <span className="chip-label">{resolvedLabel}</span>
                {key === 'means' && (
                  <small className={'status-badge doc-type-pill' + (evidenceKind === 'URL' ? ' is-url' : '')}>
                    {evidenceKind === 'URL' ? 'Enlace HTTPS' : 'PDF'}
                  </small>
                )}
                {!c.active ? ' (inactivo)' : ''}
              </label>
            )
          })}
          <label className={'choice-chip choice-chip-other' + (other ? ' is-selected' : '')}>
            <input
              type="checkbox"
              aria-label={'Otro (' + title + ')'}
              checked={!!other}
              onChange={e => toggleChoice(row, key, null, e.target.checked)}
            />
            <span className="chip-label">+ Otro</span>
          </label>
        </div>
        {other && (
          <div className="choice-other-field animate-reveal">
            <label>
              <span>Descripción de Otro ({title}):</span>
              <input
                data-other="true"
                aria-invalid={!other.other?.trim()}
                aria-describedby={attempted && !other.other?.trim() ? `${row.id}-${key}-error` : undefined}
                maxLength={500}
                placeholder={'Describa el ' + (key === 'resources' ? 'recurso' : 'medio de verificación') + ' específico…'}
                value={other.other ?? ''}
                onChange={e => update(row.id, { [key]: row[key].map(c => c.catalogId === null ? { ...c, other: e.target.value, label: null } : c) })}
              />
            </label>
            {attempted && !other.other?.trim() && (
              <span className="error" id={`${row.id}-${key}-error`}>Escriba una descripción.</span>
            )}
          </div>
        )}
      </fieldset>
    )
  }

  const activeActivity = draft.activities.find(a => a.id === editingActivityId)
  const activeIndex = activeActivity ? draft.activities.indexOf(activeActivity) : -1

  return (
    <section className={'matrix-workspace' + (embedded ? ' plan-step-panel' : '')}>
      <div className="matrix-topbar">
        {!embedded && <button type="button" className="secondary matrix-back-btn" disabled={busy} onClick={back}>
          <Icon name="arrow" size={14} style={{ transform: 'rotate(180deg)' }} />
          <span>Volver al Plan</span>
        </button>}
        <div className="matrix-topbar-meta">
          <span className="section-kicker review-kicker">PASO 3 DE 6 · PLAN OPERATIVO</span>
          <span role="status" className={'document-dirty-indicator ' + (dirty ? 'is-dirty' : 'is-clean')}>
            {dirty ? 'Tiene cambios sin guardar.' : 'Sin cambios pendientes.'}
          </span>
        </div>
      </div>

      <header className="matrix-main-header">
        <div className="matrix-title-group">
          {embedded ? <h2 className="plan-step-title">Actividades y matriz T1</h2> : <h1>Actividades y matriz T1</h1>}
          <p className="matrix-subtitle">
            Planificación operativa y articulación institucional: configure actividades, cronograma, integrantes responsables, recursos y medios de verificación.
          </p>
        </div>
        <div className="matrix-summary-pill">
          <Icon name="document" size={15} />
          <span><strong>{draft.activities.length}</strong> {draft.activities.length === 1 ? 'actividad' : 'actividades'}</span>
          <span className="dot-sep">·</span>
          <span className="matrix-author-pill">Elaborado por: <strong>{saved.elaboratedBy}</strong></span>
        </div>
      </header>

      {!saved.editable && <Toast tone="warning" message="Solo lectura: ventana de elaboración cerrada." />}

    {!!draft.activities.length && <section className="card matrix-summary-card">
      <div className="section-heading">
        <span className="section-icon"><Icon name="document" /></span>
        <div>
          <span className="section-kicker">CONSOLIDADO T1</span>
          <h2>Resumen de la matriz</h2>
          <p className="field-help">Revise la planificación completa y abra el detalle de una actividad para ajustarla.</p>
        </div>
      </div>
      <p className="matrix-summary-state" role="status">{dirty ? 'Vista de sus cambios locales · Pendiente de guardar' : 'Información guardada en el servidor'}</p>
      <div className="table-scroll matrix-summary-table" role="region" aria-label="Resumen de actividades" tabIndex={0}>
        <table>
          <caption className="matrix-table-caption">Planificación de actividades, plazo, responsables y estado de configuración</caption>
          <thead>
            <tr>
              <th scope="col">Actividad</th>
              <th scope="col" className="matrix-col-center">Plazo</th>
              <th scope="col">Responsables</th>
              <th scope="col" className="matrix-col-center">Configuración</th>
              <th scope="col" className="matrix-col-action">Acción</th>
            </tr>
          </thead>
          <tbody>
            {draft.activities.map((a, index) => {
              const resourcesCount = a.resources.filter(c => c.catalogId !== null || c.other?.trim()).length
              const meansCount = a.means.filter(c => c.catalogId !== null || c.other?.trim()).length
              const hasConfig = resourcesCount > 0 && meansCount > 0
              const dateRange = (a.startsOn && a.endsOn)
                ? `${a.startsOn.split('-').reverse().join('/')} — ${a.endsOn.split('-').reverse().join('/')}`
                : a.startsOn
                  ? `Desde ${a.startsOn.split('-').reverse().join('/')}`
                  : a.endsOn
                    ? `Hasta ${a.endsOn.split('-').reverse().join('/')}`
                    : null

              return (
                <tr key={a.id} className={editingActivityId === a.id ? 'is-selected-row' : ''}>
                  <th scope="row">
                    <div className="matrix-activity-title-cell">
                      <span className="matrix-row-number">#{index + 1}</span>
                      <div className="matrix-activity-title-content">
                        <strong>{a.title}</strong>
                        <div className="matrix-table-tags">
                          <small className="status-badge doc-type-pill">{categoryLabels[a.category]}</small>
                          {a.mandatory && <small className="status-badge is-mandatory">Obligatoria</small>}
                        </div>
                      </div>
                    </div>
                  </th>
                  <td className="matrix-col-center">
                    {dateRange ? (
                      <span className="matrix-date-range-badge">
                        <Icon name="calendar" size={12} />
                        <span>{dateRange}</span>
                      </span>
                    ) : (
                      <span className="matrix-pending-badge">Pendiente</span>
                    )}
                  </td>
                  <td>
                    {a.collective ? (
                      <div className="matrix-responsibles-cell"><span className="matrix-collective-badge" title={a.responsibleIds.length + ' integrantes · Todos seleccionados: ' + a.responsibleIds.map(id => saved.members.find(m => m.id === id)?.name ?? 'Integrante no disponible').join(', ')}>{saved.collectiveLabel}</span></div>
                    ) : a.responsibleIds.length > 0 ? (
                      <div className="matrix-responsibles-cell">
                        <span className="matrix-collective-badge">{a.responsibleIds.map(id => saved.members.find(m => m.id === id)?.name ?? 'Integrante no disponible').join(', ')}</span>
                      </div>
                    ) : (
                      <span className="matrix-pending-badge">Pendiente</span>
                    )}
                  </td>
                  <td className="matrix-col-center">
                    {hasConfig ? (
                      <span className="matrix-coverage-badge">
                        {resourcesCount} {resourcesCount === 1 ? 'recurso' : 'recursos'} · {meansCount} {meansCount === 1 ? 'medio' : 'medios'}
                      </span>
                    ) : (
                      <span className="matrix-pending-badge">Pendiente</span>
                    )}
                  </td>
                  <td className="matrix-action-cell">
                    <div className="matrix-row-actions">
                      <button
                        type="button"
                        className="secondary matrix-icon-btn matrix-edit-btn"
                        onClick={() => showActivity(a.id)}
                        title={saved.editable ? 'Configurar actividad ' + (index + 1) : 'Ver detalle de la actividad ' + (index + 1)}
                        aria-label={'Ver detalle de la actividad ' + (index + 1)}
                      >
                        <Icon name={saved.editable ? 'edit' : 'eye'} size={15} />
                      </button>
                      {saved.editable && !a.mandatory && (
                        <button
                          type="button"
                          className="secondary danger-btn matrix-icon-btn matrix-delete-btn"
                          title={'Quitar actividad ' + (index + 1)}
                          aria-label={'Quitar ' + a.title}
                          onClick={async () => {
                            if (await confirmAction(`¿Quitar la actividad «${a.title}» del borrador?`)) {
                              if (editingActivityId === a.id) setEditingActivityId(null)
                              setDraft({ ...draft, activities: draft.activities.filter(item => item.id !== a.id) })
                              setNotice('Actividad quitada del borrador.')
                            }
                          }}
                        >
                          <Icon name="trash" size={15} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>}

      <form ref={formRef} onSubmit={save}>
        <fieldset disabled={busy || !saved.editable}>
          <div className="card matrix-source-card">
            <label htmlFor="matrix-source" className="matrix-source-label">
              <span className="matrix-field-label">Fuente de la matriz</span>
              <p className="field-help">Indique de dónde procede la información: puede ser esta misma comisión o un documento de referencia institucional.</p>
              <input
                id="matrix-source"
                className="matrix-source-input"
                maxLength={500}
                placeholder="Ej.: nombre de la comisión o documento de referencia…"
                value={draft.source}
                onChange={e => { setDraft({ ...draft, source: e.target.value }); setNotice('') }}
              />
            </label>
          </div>

          <div className="matrix-activities-section-header">
            <div className="section-heading">
              <span className="section-icon"><Icon name="document" /></span>
              <div>
                <span className="section-kicker">PLANIFICACIÓN OPERATIVA</span>
                <h2>Actividades del Plan ({draft.activities.length})</h2>
              </div>
            </div>

            <ActionForm
              modal
              key={addKey}
              label="Agregar actividad"
              disabled={busy || !saved.editable}
              dirty={!!catalogId || !!otherTitle}
              onCancel={() => { setCatalogId(''); setOtherTitle('') }}
            >
              <div className="matrix-modal-content">
                <p className="matrix-modal-intro">
                  Seleccione una actividad del catálogo institucional o elija «Otra actividad» para especificar una actividad complementaria.
                </p>
                <label>
                  Actividad del catálogo
                  <select value={catalogId} onChange={e => setCatalogId(e.target.value)}>
                    <option value="">Seleccione una actividad</option>
                    {saved.definitions.filter(d => d.active && !draft.activities.some(a => a.catalogId === d.id)).map(d => (
                      <option key={d.id} value={d.id}>
                        {categoryLabels[d.category]} · {d.title}
                      </option>
                    ))}
                    <option value="__OTHER__">Otra actividad (especifique)</option>
                  </select>
                </label>
                {catalogId === '__OTHER__' && (
                  <label>
                    Título de Otra actividad
                    <input
                      maxLength={500}
                      placeholder="Ingrese la denominación de la actividad…"
                      value={otherTitle}
                      onChange={e => setOtherTitle(e.target.value)}
                    />
                  </label>
                )}
                <div className="action-form-actions">
                  <button type="button" disabled={draft.activities.length >= 100 || !catalogId || (catalogId === '__OTHER__' && !otherTitle.trim())} onClick={add}>
                    <Icon name="plus" size={15} />
                    <span>Agregar actividad</span>
                  </button>
                </div>
              </div>
            </ActionForm>
          </div>

          {draft.activities.length === 0 && (
            <div className="empty-state-notice">
              <Icon name="document" size={24} />
              <p>No hay actividades seleccionadas. Use el botón «Agregar actividad» para incorporar actividades al Plan.</p>
            </div>
          )}
        </fieldset>
          {activeActivity && (
            <dialog
              ref={activityDialog}
              aria-labelledby={`activity-${activeActivity.id}-title`}
              className="activity-modal-overlay animate-fade-in"
              onCancel={e => { e.preventDefault(); if (!busy) setEditingActivityId(null) }}
              onClick={e => {
                if (!busy && e.target === e.currentTarget) setEditingActivityId(null)
              }}
            >
              <section
                id={'activity-' + activeActivity.id}
                tabIndex={-1}
                className="card activity-modal-dialog animate-scale-up"
                role="region"
                aria-label={`Actividad ${activeIndex + 1}`}
                onClick={e => e.stopPropagation()}
              >
                <header className="activity-modal-header">
                  <div className="activity-modal-title-group">
                    <span className="activity-index-pill">#{activeIndex + 1}</span>
                    <div className="activity-modal-header-text">
                      <h2 id={`activity-${activeActivity.id}-title`}>{activeIndex + 1}. {activeActivity.title}</h2>
                      <div className="activity-tags">
                        <span className="status-badge doc-type-pill">{categoryLabels[activeActivity.category]}</span>
                        {activeActivity.mandatory && <span className="status-badge is-mandatory">Obligatoria</span>}
                        <span className="status-badge is-active-badge">En configuración</span>
                      </div>
                    </div>
                  </div>
                  <div className="activity-modal-header-actions">
                    <button
                      type="button"
                      className="secondary matrix-icon-btn modal-close-btn"
                      title="Cerrar modal"
                      aria-label="Cerrar modal"
                      disabled={busy}
                      onClick={() => setEditingActivityId(null)}
                    >
                      <Icon name="x" size={15} />
                    </button>
                  </div>
                </header>

                <div className="activity-modal-scrollable"><fieldset disabled={busy || !saved.editable} className="activity-modal-fields">
                  {!activeActivity.catalogId && (
                    <div className="activity-custom-title-field">
                      <label>
                        Título de la actividad
                        <input
                          required
                          maxLength={500}
                          value={activeActivity.title}
                          onChange={e => update(activeActivity.id, { title: e.target.value })}
                        />
                      </label>
                    </div>
                  )}

                  <div className="activity-dates-block">
                    <div className="activity-block-title">
                      <Icon name="calendar" size={15} />
                      <span>Plazo de ejecución</span>
                      <small>(Período académico: {saved.periodStartsOn?.split('-').reverse().join('/')} — {saved.periodEndsOn?.split('-').reverse().join('/')})</small>
                    </div>
                    <div className="activity-dates-inputs">
                      <label>
                        <span>Desde</span>
                        <input
                          type="date"
                          required
                          min={saved.periodStartsOn}
                          max={saved.periodEndsOn}
                          value={activeActivity.startsOn ?? ''}
                          onChange={e => update(activeActivity.id, { startsOn: e.target.value })}
                        />
                      </label>
                      <label>
                        <span>Hasta</span>
                        <input
                          type="date"
                          required
                          min={activeActivity.startsOn || saved.periodStartsOn}
                          max={saved.periodEndsOn}
                          value={activeActivity.endsOn ?? ''}
                          onChange={e => update(activeActivity.id, { endsOn: e.target.value })}
                        />
                      </label>
                    </div>
                  </div>

                  <fieldset className="choice-group" aria-label="Responsables">
                    <div className="choice-group-header">
                      <span className="choice-group-title">Responsables</span>
                      <div className="choice-group-header-right">
                        <span className="choice-count-badge">
                          {activeActivity.responsibleIds.length} {activeActivity.responsibleIds.length === 1 ? 'responsable' : 'responsables'} seleccionados
                        </span>
                        <button
                          type="button"
                          className="secondary select-all-btn"
                          onClick={() => selectResponsible(activeActivity, saved.members.map(m => m.id))}
                        >
                          <Icon name="users" size={13} />
                          <span>Seleccionar todos</span>
                        </button>
                      </div>
                    </div>

                    <div className="choice-chip-grid">
                      {saved.members.map(member => (
                        <label className={'choice-chip' + (activeActivity.responsibleIds.includes(member.id) ? ' is-selected' : '')} key={member.id}>
                          <input
                            type="checkbox"
                            checked={activeActivity.responsibleIds.includes(member.id)}
                            onChange={e => selectResponsible(activeActivity, e.target.checked
                              ? [...activeActivity.responsibleIds, member.id]
                              : activeActivity.responsibleIds.filter(id => id !== member.id))}
                          />
                          <span className="chip-label">{member.name}</span>
                        </label>
                      ))}
                    </div>

                    {activeActivity.responsibleIds.some(id => !saved.members.some(m => m.id === id)) && (
                      <p className="error">Hay responsables que ya no están disponibles. Vuelva a seleccionarlos.</p>
                    )}

                    <p className="field-help" role="status">{activeActivity.collective
                      ? 'En el documento aparecerá: ' + saved.collectiveLabel + '. Se conservan los integrantes seleccionados para trazabilidad.'
                      : !saved.collectiveLabel && activeActivity.responsibleIds.length === saved.members.length && saved.members.length > 0
                        ? 'Todos seleccionados. Administración debe configurar la denominación colectiva; mientras tanto se mostrarán los nombres.'
                        : 'Al seleccionar a todos se mostrará automáticamente la denominación colectiva configurada.'}</p>
                  </fieldset>

                  <div className="activity-catalogs-grid">
                    {choices(activeActivity, 'resources', 'Recursos')}
                    {choices(activeActivity, 'means', 'Medios de verificación')}
                  </div>
                </fieldset></div>

                <footer className="activity-modal-footer">
                  <div className="activity-modal-footer-left">
                    <span className="field-help">Los cambios se guardan en el borrador local.</span>
                  </div>
                  <div className="activity-modal-footer-actions">
                    <button
                      type="button"
                      className="secondary"
                      onClick={() => setEditingActivityId(null)}
                    >
                      Listo
                    </button>
                    <button
                      type="button"
                      disabled={busy || conflict || !saved.editable}
                      className="primary-save-btn"
                      onClick={async () => {
                        await persist()
                      }}
                    >
                      <Icon name="check" size={15} />
                      <span>{busy ? 'Guardando…' : 'Guardar matriz'}</span>
                    </button>
                  </div>
                </footer>
              </section>
            </dialog>
          )}
          {!activeActivity && (
            <div className="matrix-footer-actions">
              <div className="matrix-footer-status">
                <span role="status" className={'step-save-status' + (dirty ? ' is-dirty' : '')}>
                  {dirty ? 'Hay cambios sin guardar en la matriz' : 'Matriz sincronizada con el servidor'}
                </span>
              </div>
              <div className="matrix-footer-buttons">
                <button
                  disabled={busy || conflict || !saved.editable}
                  className={dirty ? 'primary-save-btn' : 'secondary'}
                >
                  <Icon name="check" size={15} />
                  <span>{busy ? 'Guardando…' : 'Guardar matriz'}</span>
                </button>

              </div>
            </div>
          )}
      </form>
    <StepPagination previous={{ label: 'Contenido', action: () => void previous() }} next={{ label: 'Anexos', action: () => void next() }} disabled={busy} nextDisabled={conflict} />
    {error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} />}
    {conflict && <section className="conflict"><p>Los cambios locales se conservan. Revise la matriz actual antes de descartar sus cambios.</p><button className="secondary" onClick={compare}>Consultar matriz actual</button>{latest && <><h2>Matriz actual del servidor</h2><p>Fuente: {latest.source || 'Sin fuente'}</p>{latest.activities.map(a => <section key={a.id}><h3>{a.title}</h3><dl><dt>Fechas</dt><dd>{a.startsOn?.split('-').reverse().join('/') || 'Sin inicio'} — {a.endsOn?.split('-').reverse().join('/') || 'Sin fin'}</dd><dt>Responsables</dt><dd>{a.collective ? latest.collectiveLabel : a.responsibleIds.map(id => latest.members.find(m => m.id === id)?.name ?? 'Integrante no disponible').join(', ')}</dd><dt>Recursos</dt><dd>{a.resources.map(c => c.label).join(', ')}</dd><dt>Medios</dt><dd>{a.means.map(c => c.label).join(', ')}</dd></dl></section>)}<button className="secondary" onClick={reload}>Descartar cambios y cargar matriz actual</button></>}</section>}
  </section>
  )
}
