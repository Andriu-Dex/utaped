import { confirmAction } from '../shared/confirm-action'
import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, ApiError } from '../shared/api'
import { categoryLabels, type Activity, type Matrix } from './matrixTypes'
import { Icon } from '../shared/Icon'

function selection(matrix: Matrix) { return { source: matrix.source, activities: matrix.activities } }
export function MatrixEditor({ targetDocId, onBack, onNext, onDirtyChange }: { targetDocId: string; onBack: () => Promise<void>; onNext: () => Promise<void>; onDirtyChange: (dirty: boolean) => void }) {
  const [saved, setSaved] = useState<Matrix | null>(null); const [draft, setDraft] = useState<ReturnType<typeof selection> | null>(null)
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const [conflict, setConflict] = useState(false); const [latest, setLatest] = useState<Matrix | null>(null)
  const [catalogId, setCatalogId] = useState(''); const [otherTitle, setOtherTitle] = useState('')
  const [attempted, setAttempted] = useState(false)
  const formRef = useRef<HTMLFormElement>(null)
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
    setDraft({ ...draft, activities: [...draft.activities, activity] }); setCatalogId(''); setOtherTitle(''); setError(''); setNotice('')
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
    const emptyOther = formRef.current?.querySelector<HTMLInputElement>('input[data-other="true"][aria-invalid="true"]')
    if (emptyOther) { setError('Complete la descripción de Otro.'); emptyOther.focus(); return false }
    if (draft.activities.some(a => !a.responsibleIds.length || !a.resources.length || !a.means.length)) { setError('Cada actividad requiere al menos un responsable, un recurso y un medio de verificación.'); return false }
    setBusy(true)
    try {
      const m = await api<Matrix>('/work-plans/' + targetDocId + '/matrix', 'PUT', { ...draft, rowVersion: saved.rowVersion })
      setSaved(m); setDraft(selection(m)); setNotice('Matriz guardada en el servidor.'); setAttempted(false); return true
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.'); if (e instanceof ApiError && e.status === 409) setConflict(true); return false }
    finally { setBusy(false) }
  }
  async function save(e: FormEvent) { e.preventDefault(); await persist() }
  async function next() {
    if (!saved || !draft || busy || conflict) return
    setError(''); setNotice('')
    if (!draft.activities.length) {
      setError('Agregue al menos una actividad para continuar.')
      formRef.current?.querySelector<HTMLButtonElement>('button[aria-expanded]')?.focus()
      return
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
      return
    }
    if (formRef.current && !formRef.current.checkValidity()) {
      setError('Revise los campos obligatorios de la matriz antes de continuar.')
      formRef.current.querySelector<HTMLElement>(':invalid')?.focus()
      return
    }
    if (dirty && !(await persist())) return
    setBusy(true)
    try { await onNext() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo abrir Anexos.'); setBusy(false) }
  }
  function showActivity(id: string) {
    const card = document.getElementById('activity-' + id)
    card?.focus({ preventScroll: true }); card?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'start' })
  }
  async function back() {
    if (dirty && !(await confirmAction('Hay cambios sin guardar en la matriz. ¿Salir y descartarlos?'))) return
    setBusy(true)
    try { await onBack() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo volver.'); setBusy(false) }
  }
  async function compare() { try { setLatest(await api<Matrix>('/work-plans/' + targetDocId + '/matrix')) } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo consultar.') } }
  async function reload() {
    if (!latest || !(await confirmAction('¿Descartar cambios locales y cargar la matriz actual del servidor?'))) return
    setSaved(latest); setDraft(selection(latest)); setLatest(null); setConflict(false); setAttempted(false); setError(''); setNotice('Versión del servidor cargada.')
  }
  if (!saved || !draft) return <section><button className="secondary" onClick={back}>Volver al Plan</button>{error ? <Toast message={error} /> : <p role="status">Cargando matriz…</p>}</section>
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

  return (
    <section className="matrix-workspace">
      <div className="matrix-topbar">
        <button type="button" className="secondary matrix-back-btn" disabled={busy} onClick={back}>
          <Icon name="arrow" size={14} style={{ transform: 'rotate(180deg)' }} />
          <span>Volver al Plan</span>
        </button>
        <div className="matrix-topbar-meta">
          <span className="section-kicker review-kicker">PASO 3 DE 6 · PLAN OPERATIVO</span>
          <span role="status" className={'document-dirty-indicator ' + (dirty ? 'is-dirty' : 'is-clean')}>
            {dirty ? '• Tiene cambios sin guardar.' : '• Sin cambios pendientes.'}
          </span>
        </div>
      </div>

      <header className="matrix-main-header">
        <div className="matrix-title-group">
          <h1>Actividades y matriz T1</h1>
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
          <caption className="matrix-table-caption">Planificación de actividades, fechas, responsables, recursos y medios de verificación</caption>
          <thead>
            <tr>
              <th scope="col">Actividad</th>
              <th scope="col">Desde</th>
              <th scope="col">Hasta</th>
              <th scope="col">Responsables</th>
              <th scope="col">Recursos</th>
              <th scope="col">Medios de verificación</th>
              <th scope="col">Detalle</th>
            </tr>
          </thead>
          <tbody>
            {draft.activities.map((a, index) => <tr key={a.id}>
              <th scope="row"><span className="matrix-row-number">{index + 1}</span><strong>{a.title}</strong><small>{categoryLabels[a.category]}{a.mandatory ? ' · Obligatoria' : ''}</small></th>
              <td><span className="matrix-date-cell">{a.startsOn?.split('-').reverse().join('/') || 'Pendiente'}</span></td>
              <td><span className="matrix-date-cell">{a.endsOn?.split('-').reverse().join('/') || 'Pendiente'}</span></td>
              <td>{a.collective ? <span className="status-badge">{saved.collectiveLabel}</span> : a.responsibleIds.map(id => saved.members.find(m => m.id === id)?.name ?? 'Integrante no disponible').join(', ') || <span className="matrix-pending">Pendiente</span>}</td>
              <td>{a.resources.map(c => c.label || c.other || saved.catalogs.find(v => v.id === c.catalogId)?.label).filter(Boolean).join(', ') || <span className="matrix-pending">Pendiente</span>}</td>
              <td>{a.means.map(c => c.label || c.other || saved.catalogs.find(v => v.id === c.catalogId)?.label).filter(Boolean).join(', ') || <span className="matrix-pending">Pendiente</span>}</td>
            <td><button type="button" className="secondary matrix-detail-button" onClick={() => showActivity(a.id)} title={'Ver detalle de la actividad ' + (index + 1)} aria-label={'Ver detalle de la actividad ' + (index + 1)}><Icon name="eye" size={16} /></button></td>
            </tr>)}
          </tbody>
        </table>
      </div>
    </section>}

      <form ref={formRef} onSubmit={save}>
        <fieldset disabled={busy || !saved.editable}>
          <div className="matrix-source-card">
            <div className="matrix-source-header">
              <label htmlFor="matrix-source">
                <span className="matrix-field-label">Fuente de la matriz</span>
                <span className="matrix-field-desc">Indique de dónde procede la información: puede ser esta misma comisión o un documento de referencia. Este dato es editable.</span>
              </label>
            </div>
            <input
              id="matrix-source"
              className="matrix-source-input"
              maxLength={500}
              placeholder="Ej.: nombre de la comisión o documento de referencia…"
              value={draft.source}
              onChange={e => { setDraft({ ...draft, source: e.target.value }); setNotice('') }}
            />
          </div>

          <div className="matrix-add-section">
            <ActionForm
              label="Agregar actividad"
              disabled={busy || !saved.editable}
              dirty={!!catalogId || !!otherTitle}
              onCancel={() => { setCatalogId(''); setOtherTitle('') }}
            >
              <div className="card matrix-add-card">
                <div className="section-heading">
                  <span className="section-icon"><Icon name="plus" /></span>
                  <div>
                    <span className="section-kicker">CATÁLOGO INSTITUCIONAL</span>
                    <h2>Seleccionar actividades</h2>
                  </div>
                </div>
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
                <button type="button" disabled={draft.activities.length >= 100 || !catalogId || (catalogId === '__OTHER__' && !otherTitle.trim())} onClick={add}>
                  <Icon name="plus" size={15} />
                  <span>Agregar actividad</span>
                </button>
              </div>
            </ActionForm>
          </div>

          {draft.activities.length === 0 && (
            <div className="empty-state-notice">
              <Icon name="document" size={24} />
              <p>No hay actividades seleccionadas. Use el botón «Agregar actividad» para incorporar actividades al Plan.</p>
            </div>
          )}

          {draft.activities.map((row, index) => (
            <section id={'activity-' + row.id} tabIndex={-1} className="card activity-card" key={row.id} aria-label={`Actividad ${index + 1}`}>
              <header className="activity-card-header">
                <div className="activity-header-meta">
                  <span className="activity-index-pill">#{index + 1}</span>
                  <div>
                    <h2>{index + 1}. {row.title}</h2>
                    <div className="activity-tags">
                      <span className="status-badge doc-type-pill">{categoryLabels[row.category]}</span>
                      {row.mandatory && <span className="status-badge is-mandatory">Obligatoria</span>}
                    </div>
                  </div>
                </div>
                {!row.mandatory && (
                  <button
                    type="button"
                    className="secondary danger-btn"
                    title="Quitar actividad"
                    aria-label={'Quitar ' + row.title}
                    onClick={() => {
                      setDraft({ ...draft, activities: draft.activities.filter(a => a.id !== row.id) })
                      setNotice('')
                    }}
                  >
                    <Icon name="trash" size={14} />
                    <span>Quitar actividad</span>
                  </button>
                )}
              </header>

              {!row.catalogId && (
                <div className="activity-custom-title-field">
                  <label>
                    Título de la actividad
                    <input required maxLength={500} value={row.title} onChange={e => update(row.id, { title: e.target.value })} />
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
                      value={row.startsOn ?? ''}
                      onChange={e => update(row.id, { startsOn: e.target.value })}
                    />
                  </label>
                  <label>
                    <span>Hasta</span>
                    <input
                      type="date"
                      required
                      min={row.startsOn || saved.periodStartsOn}
                      max={saved.periodEndsOn}
                      value={row.endsOn ?? ''}
                      onChange={e => update(row.id, { endsOn: e.target.value })}
                    />
                  </label>
                </div>
              </div>

              <fieldset className="choice-group" aria-label="Responsables">
                <div className="choice-group-header">
                  <span className="choice-group-title">Responsables</span>
                  <div className="choice-group-header-right">
                    <span className="choice-count-badge">
                      {row.responsibleIds.length} {row.responsibleIds.length === 1 ? 'responsable' : 'responsables'} seleccionados
                    </span>
                    <button
                      type="button"
                      className="secondary select-all-btn"
                      onClick={() => selectResponsible(row, saved.members.map(m => m.id))}
                    >
                      <Icon name="users" size={13} />
                      <span>Seleccionar todos</span>
                    </button>
                  </div>
                </div>

                <div className="choice-chip-grid">
                  {saved.members.map(member => (
                    <label className={'choice-chip' + (row.responsibleIds.includes(member.id) ? ' is-selected' : '')} key={member.id}>
                      <input
                        type="checkbox"
                        checked={row.responsibleIds.includes(member.id)}
                        onChange={e => selectResponsible(row, e.target.checked
                          ? [...row.responsibleIds, member.id]
                          : row.responsibleIds.filter(id => id !== member.id))}
                      />
                      <span className="chip-label">{member.name}</span>
                    </label>
                  ))}
                </div>

                {row.responsibleIds.some(id => !saved.members.some(m => m.id === id)) && (
                  <p className="error">Hay responsables que ya no están disponibles. Vuelva a seleccionarlos.</p>
                )}

                <p className="field-help" role="status">{row.collective
                  ? 'En el documento aparecerá: ' + saved.collectiveLabel + '. Se conservan los integrantes seleccionados para trazabilidad.'
                  : !saved.collectiveLabel && row.responsibleIds.length === saved.members.length && saved.members.length > 0
                    ? 'Todos seleccionados. Administración debe configurar la denominación colectiva; mientras tanto se mostrarán los nombres.'
                    : 'Al seleccionar a todos se mostrará automáticamente la denominación colectiva configurada.'}</p>
              </fieldset>

              <div className="activity-catalogs-grid">
                {choices(row, 'resources', 'Recursos')}
                {choices(row, 'means', 'Medios de verificación')}
              </div>
            </section>
          ))}

        </fieldset>
          <div className="matrix-footer-actions">
            <div className="matrix-footer-status">
              <span role="status" className={'step-save-status' + (dirty ? ' is-dirty' : '')}>
                {dirty ? '• Hay cambios sin guardar en la matriz' : '• Matriz sincronizada con el servidor'}
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
              <button type="button" disabled={busy || conflict} onClick={next} className="matrix-next-button">
                <span>{busy ? 'Procesando…' : 'Siguiente: Anexos'}</span><Icon name="arrow" size={16} />
              </button>
            </div>
          </div>
      </form>
    {error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} />}
    {conflict && <section className="conflict"><p>Los cambios locales se conservan. Revise la matriz actual antes de descartar sus cambios.</p><button className="secondary" onClick={compare}>Consultar matriz actual</button>{latest && <><h2>Matriz actual del servidor</h2><p>Fuente: {latest.source || 'Sin fuente'}</p>{latest.activities.map(a => <section key={a.id}><h3>{a.title}</h3><dl><dt>Fechas</dt><dd>{a.startsOn?.split('-').reverse().join('/') || 'Sin inicio'} — {a.endsOn?.split('-').reverse().join('/') || 'Sin fin'}</dd><dt>Responsables</dt><dd>{a.collective ? latest.collectiveLabel : a.responsibleIds.map(id => latest.members.find(m => m.id === id)?.name ?? 'Integrante no disponible').join(', ')}</dd><dt>Recursos</dt><dd>{a.resources.map(c => c.label).join(', ')}</dd><dt>Medios</dt><dd>{a.means.map(c => c.label).join(', ')}</dd></dl></section>)}<button className="secondary" onClick={reload}>Descartar cambios y cargar matriz actual</button></>}</section>}
  </section>
  )
}
