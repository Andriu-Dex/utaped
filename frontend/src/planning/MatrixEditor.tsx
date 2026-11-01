import { DocumentReadOnlyNotice } from './DocumentReadOnlyNotice'
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

type RecoveryContent = { draft: ReturnType<typeof selection>; catalogId: string; otherTitle: string; selectedDefinitions: string[] }
type Recovery = { baseVersion: number; revision: number; content: RecoveryContent }
function selection(matrix: Matrix) { return { source: matrix.source, activities: matrix.activities, note: matrix.note } }
export function MatrixEditor({ targetDocId, onBack, onPrevious, onNext, onDirtyChange, onSaved, isSplitView, onToggleSplit }: { onSaved?: () => Promise<void>; onPrevious?: () => Promise<void>; targetDocId: string; onBack: () => Promise<void>; onNext: () => Promise<void>; onDirtyChange: (dirty: boolean) => void; isSplitView?: boolean; onToggleSplit?: () => void }) {
  const [saved, setSaved] = useState<Matrix | null>(null); const [draft, setDraft] = useState<ReturnType<typeof selection> | null>(null)
  const [loadVersion, setLoadVersion] = useState(0)
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [busy, setBusy] = useState(false)
  const [autosaving, setAutosaving] = useState(false), [autosaveFailed, setAutosaveFailed] = useState('')
  const writeLock = useRef(false), mounted = useRef(false)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false } }, [])
  const [recoveryRevision, setRecoveryRevision] = useState<number | null>(null), [recoverySaved, setRecoverySaved] = useState(''), [staleRecovery, setStaleRecovery] = useState<Recovery | null>(null)
  const [conflict, setConflict] = useState(false); const [latest, setLatest] = useState<Matrix | null>(null)
  const [catalogId, setCatalogId] = useState(''); const [otherTitle, setOtherTitle] = useState('')
  const [selectedDefinitions, setSelectedDefinitions] = useState<string[]>([])
  const [activityOrigin, setActivityOrigin] = useState(''), [activityQuery, setActivityQuery] = useState('')
  const [strategy, setStrategy] = useState('')
  const [objective, setObjective] = useState(''), [criterion, setCriterion] = useState(''), [indicator, setIndicator] = useState('')
  const availableDefinitions = saved?.definitions.filter(d => d.active && !draft?.activities.some(a => a.catalogId === d.id) && (!activityOrigin || d.category === activityOrigin)) ?? []
  const objectives = [...new Set(availableDefinitions.map(d => d.context?.objective).filter((value): value is string => !!value))]
  const strategies = [...new Set(availableDefinitions.filter(d => !objective || d.context?.objective === objective).map(d => d.context?.strategy).filter((value): value is string => !!value))]
  const criteria = [...new Set(availableDefinitions.filter(d => !objective || d.context?.objective === objective).map(d => d.context?.criterion).filter((value): value is string => !!value))]
  const indicators = [...new Set(availableDefinitions.filter(d => (!objective || d.context?.objective === objective) && (!criterion || d.context?.criterion === criterion)).map(d => d.context?.indicator).filter((value): value is string => !!value))]
  const filteredDefinitions = availableDefinitions.filter(d => (!strategy || d.context?.strategy === strategy) && (!objective || d.context?.objective === objective) && (!criterion || d.context?.criterion === criterion) && (!indicator || d.context?.indicator === indicator) && [d.title, d.context?.objective, d.context?.criterion, d.context?.indicator, d.context?.strategy].join(' ').toLocaleLowerCase('es').includes(activityQuery.toLocaleLowerCase('es')))
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
  const recoveryContent = draft ? JSON.stringify({ draft, catalogId, otherTitle, selectedDefinitions }) : ''
  const dirty = autosaving || !!selectedDefinitions.length || !!catalogId || !!otherTitle || !!saved && !!draft && JSON.stringify(selection(saved)) !== JSON.stringify(draft)
  const unprotected = dirty ? (autosaving || recoveryRevision === null || recoverySaved !== recoveryContent) : recoveryRevision !== null
  useEffect(() => { onDirtyChange(unprotected) }, [unprotected, onDirtyChange])
  useEffect(() => () => onDirtyChange(false), [onDirtyChange])
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => { if (unprotected) { e.preventDefault(); e.returnValue = '' } }
    window.addEventListener('beforeunload', handler); return () => window.removeEventListener('beforeunload', handler)
  }, [unprotected])
  useEffect(() => {
    let cancelled = false
    Promise.all([api<Matrix>('/work-plans/' + targetDocId + '/matrix'), api<{ recovery: Recovery | null }>('/work-plans/' + targetDocId + '/matrix/recovery')]).then(([m, response]) => { if (!cancelled) {
      setSaved(m); setDraft(selection(m)); const recovery = response.recovery
      if (recovery && m.editable) {
        setRecoveryRevision(recovery.revision)
        if (recovery.baseVersion !== m.rowVersion) { setStaleRecovery(recovery); setConflict(true); setError('La matriz cambió desde el último borrador. Revise sus cambios antes de recuperarlos.') }
        else { setDraft(recovery.content.draft); setCatalogId(recovery.content.catalogId); setOtherTitle(recovery.content.otherTitle); setSelectedDefinitions(recovery.content.selectedDefinitions); setRecoverySaved(JSON.stringify(recovery.content)); setNotice('Se recuperó el borrador de la matriz.') }
      }
    } }).catch(e => { if (!cancelled) setError(e.message) })
    return () => { cancelled = true }
  }, [targetDocId, loadVersion])
  function update(id: string, patch: Partial<Activity>) { setDraft(d => d && ({ ...d, activities: d.activities.map(a => a.id === id ? { ...a, ...patch } : a) })); setNotice('') }
  function selectResponsible(row: Activity, responsibleIds: string[]) {
    update(row.id, { responsibleIds, collective: !!saved?.collectiveLabel && !!saved.members.length && saved.members.every(member => responsibleIds.includes(member.id)) && responsibleIds.length === saved.members.length })
  }
  function add() {
    if (!saved || !draft) return
    if (selectedDefinitions.length) {
      const entries = saved.definitions.filter(d => selectedDefinitions.includes(d.id) && !draft.activities.some(a => a.catalogId === d.id))
      if (draft.activities.length + entries.length > 100) { setError('La matriz admite hasta 100 actividades.'); return }
      const additions: Activity[] = entries.map(d => ({ id: crypto.randomUUID(), catalogId: d.id, title: d.title, category: d.category, mandatory: d.mandatory, context: d.context, startsOn: null, endsOn: null, responsibleIds: [], collective: false, resources: [], means: [] }))
      setDraft({ ...draft, activities: [...draft.activities, ...additions] }); setSelectedDefinitions([]); setCatalogId(''); setAddKey(k => k + 1); setNotice(`${additions.length} actividades incorporadas. Configure su planificación.`); setError(''); return
    }
    const definition = saved.definitions.find(d => d.id === catalogId)
    if (!definition && !otherTitle.trim()) { setError('Escriba el título de la actividad Otra.'); return }
    const activity: Activity = { id: crypto.randomUUID(), catalogId: definition?.id ?? null, title: definition?.title ?? otherTitle.trim(), category: definition?.category ?? 'OTHER', mandatory: definition?.mandatory ?? false, context: definition?.context, startsOn: null, endsOn: null, responsibleIds: [], collective: false, resources: [], means: [] }
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
  useEffect(() => {
    if (!saved || !saved.editable || dirty || recoveryRevision === null || busy || autosaving || conflict || autosaveFailed === 'discard') return
    const timer = window.setTimeout(() => {
      if (writeLock.current) return
      writeLock.current = true; setAutosaving(true)
      api('/work-plans/' + targetDocId + '/matrix/recovery?revision=' + recoveryRevision, 'DELETE').then(() => {
        if (mounted.current) { setRecoveryRevision(null); setRecoverySaved(''); setAutosaveFailed('') }
      }).catch(e => { if (mounted.current) { setAutosaveFailed('discard'); setError(e.message || 'No se pudo actualizar el borrador.'); if (e instanceof ApiError && e.status === 409) setConflict(true) } }).finally(() => { writeLock.current = false; if (mounted.current) setAutosaving(false) })
    }, 700)
    return () => window.clearTimeout(timer)
  }, [saved, dirty, recoveryRevision, busy, autosaving, conflict, autosaveFailed, targetDocId])
  useEffect(() => {
    if (!saved || !draft || !saved.editable || busy || autosaving || conflict || !dirty || recoveryContent === recoverySaved || recoveryContent === autosaveFailed) return
    const timer = window.setTimeout(() => {
      if (writeLock.current) return
      writeLock.current = true; setAutosaving(true)
      api<{ recovery: Recovery }>('/work-plans/' + targetDocId + '/matrix/recovery', 'PUT', { baseVersion: saved.rowVersion, revision: recoveryRevision, content: JSON.parse(recoveryContent) }).then(value => {
        if (!mounted.current) return
        setRecoveryRevision(value.recovery.revision); setRecoverySaved(recoveryContent); setAutosaveFailed('')
      }).catch(e => { if (mounted.current) { setAutosaveFailed(recoveryContent); setError(e.message || 'No se pudo guardar el borrador de la matriz.'); if (e instanceof ApiError && e.status === 409) setConflict(true) } }).finally(() => { writeLock.current = false; if (mounted.current) setAutosaving(false) })
    }, 700)
    return () => window.clearTimeout(timer)
  }, [saved, draft, busy, autosaving, conflict, dirty, recoveryContent, recoverySaved, recoveryRevision, autosaveFailed, targetDocId])
  useEffect(() => {
    if (!saved || !draft || !saved.editable || busy || autosaving || conflict || catalogId || otherTitle || selectedDefinitions.length) return
    const serialized = JSON.stringify(draft)
    if (serialized === JSON.stringify(selection(saved)) || serialized === autosaveFailed || !draft.note.trim()) return
    if (draft.activities.some(a => !a.title.trim() || !a.startsOn || !a.endsOn || a.startsOn > a.endsOn || a.startsOn < saved.periodStartsOn || a.endsOn > saved.periodEndsOn || !a.responsibleIds.length || a.responsibleIds.some(id => !saved.members.some(member => member.id === id)) || !a.resources.length || !a.means.length || [...a.resources, ...a.means].some(c => c.catalogId === null && !c.other?.trim()) || saved.restrictHolidayEndpoints && saved.holidays.some(h => h.date === a.startsOn || h.date === a.endsOn))) return
    const timer = window.setTimeout(() => {
      if (writeLock.current) return
      writeLock.current = true; setAutosaving(true)
      api<Matrix>('/work-plans/' + targetDocId + '/matrix', 'PUT', { ...draft, rowVersion: saved.rowVersion, recoveryRevision }).then(async value => {
        if (!mounted.current) return
        setSaved(value); setDraft(current => JSON.stringify(current) === serialized ? selection(value) : current); setRecoveryRevision(null); setRecoverySaved(''); setAutosaveFailed(''); await onSaved?.()
      }).catch(e => { if (mounted.current) { setAutosaveFailed(serialized); setError(e.message || 'No se pudo autoguardar la matriz.'); if (e instanceof ApiError && e.status === 409) setConflict(true) } }).finally(() => { writeLock.current = false; if (mounted.current) setAutosaving(false) })
    }, 1200)
    return () => window.clearTimeout(timer)
  }, [saved, draft, busy, autosaving, conflict, catalogId, otherTitle, selectedDefinitions.length, autosaveFailed, targetDocId, onSaved, recoveryRevision])
  async function persist(): Promise<boolean> {
    if (!saved || !draft || writeLock.current) return false
    if (catalogId || otherTitle || selectedDefinitions.length) { setError('Agregue la actividad seleccionada o cancele su creación antes de guardar y continuar.'); return false }
    setAttempted(true); setError(''); setNotice('')
    const missingDates = draft.activities.find(a => !a.startsOn || !a.endsOn || a.startsOn > a.endsOn)
    if (missingDates) { setError('Complete fechas válidas antes de guardar la matriz.'); showActivity(missingDates.id); return false }
    const emptyOther = formRef.current?.querySelector<HTMLInputElement>('input[data-other="true"][aria-invalid="true"]')
    if (emptyOther) { setError('Complete la descripción de Otro.'); emptyOther.focus(); return false }
    if (draft.activities.some(a => !a.responsibleIds.length || !a.resources.length || !a.means.length)) { setError('Cada actividad requiere al menos un responsable, un recurso y un medio de verificación.'); return false }
    setBusy(true)
    try {
      const m = await api<Matrix>('/work-plans/' + targetDocId + '/matrix', 'PUT', { ...draft, rowVersion: saved.rowVersion, recoveryRevision })
      setSaved(m); setDraft(selection(m)); setRecoveryRevision(null); setRecoverySaved(''); await onSaved?.(); setNotice('Matriz guardada en el servidor.'); setAttempted(false); setEditingActivityId(null); return true
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar.'); if (e instanceof ApiError && e.status === 409) setConflict(true); return false }
    finally { setBusy(false) }
  }
  async function save(e: FormEvent) { e.preventDefault(); await persist() }
  async function canContinue(): Promise<boolean> {
    if (!saved || !draft || busy || writeLock.current || conflict) return false
    if (!saved.editable) return true
    setError(''); setNotice('')
    if (!draft.activities.length) {
      setError('Agregue al menos una actividad para continuar.')
      formRef.current?.querySelector<HTMLButtonElement>('button[aria-expanded]')?.focus()
      return false
    }
    setAttempted(true)
    if (saved.definitions.some(d => d.active && d.category === 'IMPROVEMENT_ACTION') && !draft.activities.some(a => a.category === 'IMPROVEMENT_ACTION')) { setError('Seleccione al menos una acción de mejora aplicable a su comisión.'); return false }
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
    return !unprotected || await confirmAction('Hay cambios pendientes de guardar en la matriz. ¿Cambiar de paso?')
  }, busy || autosaving || (!saved && !error))
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
    if (writeLock.current) return
    if (unprotected && !(await confirmAction('Hay cambios pendientes de guardar en la matriz. ¿Salir?'))) return
    setBusy(true)
    try { await onBack() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo volver.'); setBusy(false) }
  }
  async function previous() {
    if (busy || writeLock.current || (unprotected && !(await confirmAction('Hay cambios pendientes de guardar en la matriz. ¿Volver al contenido?')))) return
    setBusy(true)
    try { await (onPrevious ?? onBack)() } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo cambiar de paso.'); setBusy(false) }
  }
  async function compare() { try { setLatest(await api<Matrix>('/work-plans/' + targetDocId + '/matrix')) } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo consultar.') } }
  async function reload() {
    if (!latest || !(await confirmAction('¿Descartar cambios locales y cargar la matriz actual del servidor?'))) return
    if (recoveryRevision !== null) { try { await api('/work-plans/' + targetDocId + '/matrix/recovery?revision=' + recoveryRevision, 'DELETE'); setRecoveryRevision(null); setRecoverySaved('') } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo descartar el borrador.'); return } }
    setEditingActivityId(null); setSaved(latest); setDraft(selection(latest)); setLatest(null); setConflict(false); setAttempted(false); setError(''); setNotice('Versión del servidor cargada.')
  }
  if (!saved || !draft) return (
    <section>
      {!embedded && (
        <button
          type="button"
          className="plan-back-btn"
          onClick={back}
          title="Volver al Plan"
          aria-label="Volver al Plan"
        >
          <Icon name="arrowLeft" size={20} />
          <span className="sr-only">Volver al Plan</span>
        </button>
      )}
      {error ? <><Toast message={error} /><button type="button" className="secondary" onClick={() => { setError(''); setLoadVersion(value => value + 1) }}>Reintentar carga de la matriz</button></> : <p role="status">Cargando matriz…</p>}
    </section>
  )
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
        {!embedded && (
          <button
            type="button"
            className="plan-back-btn"
            disabled={busy}
            onClick={back}
            title="Volver al Plan"
            aria-label="Volver al Plan"
          >
            <Icon name="arrowLeft" size={20} />
            <span className="sr-only">Volver al Plan</span>
          </button>
        )}
        <div className="matrix-topbar-meta">
          <span className="section-kicker review-kicker">PASO 3 DE 5 · PLAN OPERATIVO</span>
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

      <DocumentReadOnlyNotice document={saved} />

    {!!draft.activities.length && <section className="card matrix-summary-card">
      <div className="section-heading matrix-summary-heading">
        <span className="section-icon"><Icon name="document" /></span>
        <div>
          <span className="section-kicker">CONSOLIDADO T1</span>
          <h2>Resumen de la matriz</h2>
          <p className="field-help">Revise la planificación completa y abra el detalle de una actividad para ajustarla.</p>
        </div>
        {onToggleSplit && (
          <div className="matrix-heading-actions">
            <button
              type="button"
              className="secondary matrix-toggle-split-btn"
              onClick={onToggleSplit}
              title={isSplitView ? 'Ver matriz a ancho completo' : 'Volver a vista dividida con previsualización'}
              aria-label={isSplitView ? 'Ver matriz a ancho completo' : 'Volver a vista dividida'}
            >
              <Icon name={isSplitView ? 'maximize' : 'columns'} size={14} />
              <span>{isSplitView ? 'Ver matriz completa' : 'Vista dividida'}</span>
            </button>
          </div>
        )}
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
                  <td className="matrix-col-center matrix-cell-dates">
                    <span className="matrix-card-field-label" aria-hidden="true">Plazo:</span>
                    <div className="matrix-cell-content">
                      {dateRange ? (
                        <span className="matrix-date-range-badge">
                          <Icon name="calendar" size={12} />
                          <span>{dateRange}</span>
                        </span>
                      ) : (
                        <span className="matrix-pending-badge">Pendiente</span>
                      )}
                    </div>
                  </td>
                  <td className="matrix-cell-responsibles">
                    <span className="matrix-card-field-label" aria-hidden="true">Responsables:</span>
                    <div className="matrix-cell-content">
                      {a.collective ? (
                        <div className="matrix-responsibles-cell"><span className="matrix-collective-badge" title={a.responsibleIds.length + ' integrantes · Todos seleccionados: ' + a.responsibleIds.map(id => saved.members.find(m => m.id === id)?.name ?? 'Integrante no disponible').join(', ')}>{saved.collectiveLabel}</span></div>
                      ) : a.responsibleIds.length > 0 ? (
                        <div className="matrix-responsibles-cell">
                          <span className="matrix-collective-badge">{a.responsibleIds.map(id => saved.members.find(m => m.id === id)?.name ?? 'Integrante no disponible').join(', ')}</span>
                        </div>
                      ) : (
                        <span className="matrix-pending-badge">Pendiente</span>
                      )}
                    </div>
                  </td>
                  <td className="matrix-col-center matrix-cell-coverage">
                    <span className="matrix-card-field-label" aria-hidden="true">Configuración:</span>
                    <div className="matrix-cell-content">
                      {hasConfig ? (
                        <span className="matrix-coverage-badge">
                          {resourcesCount} {resourcesCount === 1 ? 'recurso' : 'recursos'} · {meansCount} {meansCount === 1 ? 'medio' : 'medios'}
                        </span>
                      ) : (
                        <span className="matrix-pending-badge">Pendiente</span>
                      )}
                    </div>
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

          <div className="card matrix-source-card"><label htmlFor="matrix-note">Nota de la matriz</label><textarea id="matrix-note" required maxLength={2000} value={draft.note} onChange={e => { setDraft({ ...draft, note: e.currentTarget.value }); setNotice('') }} /></div>
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
              dirty={!!catalogId || !!otherTitle || !!selectedDefinitions.length}
              onCancel={() => { setCatalogId(''); setOtherTitle(''); setSelectedDefinitions([]) }}
            >
              <div className="matrix-modal-content">
                <p className="matrix-modal-intro">
                  Seleccione las actividades del catálogo institucional o elija «Otra actividad» para especificar una actividad complementaria.
                </p>
                <div className="grid">
                  <label>Origen de actividades<select value={activityOrigin} onChange={e => { setActivityOrigin(e.target.value); setObjective(''); setStrategy(''); setCriterion(''); setIndicator(''); setCatalogId(e.target.value === 'OTHER' ? '__OTHER__' : '') }}><option value="">Todos los orígenes</option>{Object.entries(categoryLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                  <label>Buscar actividad<input type="search" value={activityQuery} onChange={e => { setActivityQuery(e.target.value); setCatalogId('') }} /></label>
                </div>
                {!!objectives.length && <label>Objetivo estratégico<select value={objective} onChange={e => { setObjective(e.target.value); setStrategy(''); setCriterion(''); setIndicator(''); setCatalogId('') }}><option value="">Todos los objetivos</option>{objectives.map(value => <option key={value} value={value}>{value}</option>)}</select></label>}
                {!!strategies.length && <label>Estrategia<select value={strategy} onChange={e => { setStrategy(e.currentTarget.value); setCatalogId('') }}><option value="">Todas las estrategias</option>{strategies.map(value => <option key={value} value={value}>{value}</option>)}</select></label>}
                {!!criteria.length && <label>Criterio<select value={criterion} onChange={e => { setCriterion(e.target.value); setIndicator(''); setCatalogId('') }}><option value="">Todos los criterios</option>{criteria.map(value => <option key={value} value={value}>{value}</option>)}</select></label>}
                {!!indicators.length && <label>Indicador<select value={indicator} onChange={e => { setIndicator(e.target.value); setCatalogId('') }}><option value="">Todos los indicadores</option>{indicators.map(value => <option key={value} value={value}>{value}</option>)}</select></label>}
                <p role="status">{filteredDefinitions.length} actividades disponibles · {selectedDefinitions.length} seleccionadas</p>
                <div className="catalog-reference-list" aria-label="Actividades del catálogo">
                  {filteredDefinitions.map(d => <label key={d.id} className="catalog-reference-option"><input type="checkbox" checked={selectedDefinitions.includes(d.id)} onChange={e => { setCatalogId(''); setOtherTitle(''); setSelectedDefinitions(ids => e.target.checked ? [...ids, d.id] : ids.filter(id => id !== d.id)) }} /><span><strong>{d.title}</strong><small>{[categoryLabels[d.category], d.context?.indicator, d.context?.strategy].filter(Boolean).join(' · ')}</small></span></label>)}
                  {!filteredDefinitions.length && <p>No hay actividades con estos filtros.</p>}
                </div>
                <button type="button" className="secondary" onClick={() => { setSelectedDefinitions([]); setCatalogId('__OTHER__') }}>Otra actividad (especifique)</button>
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
                  <button type="button" disabled={draft.activities.length >= 100 || (!selectedDefinitions.length && (!catalogId || (catalogId === '__OTHER__' && !otherTitle.trim())))} onClick={add}>
                    <Icon name="plus" size={15} />
                    <span>{selectedDefinitions.length ? 'Incorporar selección (' + selectedDefinitions.length + ')' : 'Agregar actividad'}</span>
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
                    <span className="field-help" role="status">{autosaving ? 'Guardando borrador…' : autosaveFailed ? 'No se pudo guardar el borrador.' : dirty && recoverySaved === recoveryContent ? 'Borrador guardado en el servidor.' : dirty ? 'Cambios pendientes de guardar.' : 'Matriz sincronizada con el servidor.'}</span>
                    {!!autosaveFailed && !conflict && <button type="button" className="secondary" onClick={() => { setAutosaveFailed(''); setError('') }}>Reintentar guardado</button>}
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
                      disabled={busy || autosaving || conflict || !saved.editable}
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
                  {autosaving ? 'Guardando borrador…' : dirty && recoverySaved === recoveryContent ? 'Borrador guardado · faltan datos por completar' : dirty ? 'Hay cambios pendientes de guardar' : 'Matriz sincronizada con el servidor'}
                </span>
              </div>
              <div className="matrix-footer-buttons">
                <button
                  disabled={busy || autosaving || conflict || !saved.editable}
                  className={dirty ? 'primary-save-btn' : 'secondary'}
                >
                  <Icon name="check" size={15} />
                  <span>{busy ? 'Guardando…' : 'Guardar matriz'}</span>
                </button>

              </div>
            </div>
          )}
      </form>
    <StepPagination previous={{ label: 'Contenido', action: () => void previous() }} next={{ label: 'Anexos', action: () => void next() }} disabled={busy || autosaving} nextDisabled={conflict} />
    {error && <Toast message={error} onDismiss={() => setError('')} />}{!!autosaveFailed && !conflict && <button type="button" className="secondary" onClick={() => { setAutosaveFailed(''); setError('') }}>Reintentar guardado</button>}{notice && <Toast tone="success" message={notice} />}
    {staleRecovery && <section className="conflict"><p>Existe un borrador guardado sobre una versión anterior de la matriz. Puede recuperar sus cambios y revisarlos antes de guardar.</p><button type="button" className="secondary" onClick={async () => { if (!(await confirmAction('La matriz cambió después de guardar este borrador. ¿Recuperar sus cambios para revisarlos?'))) return; setDraft(staleRecovery.content.draft); setCatalogId(staleRecovery.content.catalogId); setOtherTitle(staleRecovery.content.otherTitle); setSelectedDefinitions(staleRecovery.content.selectedDefinitions); setRecoverySaved(''); setStaleRecovery(null); setConflict(false); setError('') }}>Recuperar borrador anterior</button><button type="button" className="secondary" onClick={async () => { if (!(await confirmAction('¿Descartar el borrador anterior y conservar la matriz actual?'))) return; try { await api('/work-plans/' + targetDocId + '/matrix/recovery?revision=' + staleRecovery.revision, 'DELETE'); setRecoveryRevision(null); setRecoverySaved(''); setStaleRecovery(null); setConflict(false); setError('') } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo descartar el borrador.') } }}>Descartar borrador anterior</button></section>}
    {conflict && !staleRecovery && <section className="conflict"><p>Los cambios locales se conservan. Revise la matriz actual antes de descartar sus cambios.</p><button className="secondary" onClick={compare}>Consultar matriz actual</button>{latest && <><h2>Matriz actual del servidor</h2><p>Fuente: {latest.source || 'Sin fuente'}</p>{latest.activities.map(a => <section key={a.id}><h3>{a.title}</h3><dl><dt>Fechas</dt><dd>{a.startsOn?.split('-').reverse().join('/') || 'Sin inicio'} — {a.endsOn?.split('-').reverse().join('/') || 'Sin fin'}</dd><dt>Responsables</dt><dd>{a.collective ? latest.collectiveLabel : a.responsibleIds.map(id => latest.members.find(m => m.id === id)?.name ?? 'Integrante no disponible').join(', ')}</dd><dt>Recursos</dt><dd>{a.resources.map(c => c.label).join(', ')}</dd><dt>Medios</dt><dd>{a.means.map(c => c.label).join(', ')}</dd></dl></section>)}<button className="secondary" onClick={reload}>Descartar cambios y cargar matriz actual</button></>}</section>}
  </section>
  )
}
