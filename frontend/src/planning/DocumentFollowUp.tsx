import { useEffect, useId, useRef, useState } from 'react'
import { Icon, type IconName } from '../shared/Icon'
import './document-follow-up.css'

export type FollowUpSection = 'rounds' | 'execution' | 'history'

export function DocumentFollowUp({ approved, disabled, pendingChanges, onOpen }: {
  approved: boolean; disabled: boolean; pendingChanges: boolean; onOpen: (section: FollowUpSection) => void
}) {
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null), trigger = useRef<HTMLButtonElement>(null), id = useId()
  useEffect(() => {
    if (!open) return
    const outside = (event: PointerEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false) }
    document.addEventListener('pointerdown', outside)
    return () => document.removeEventListener('pointerdown', outside)
  }, [open])
  const options: { key: FollowUpSection; icon: IconName; label: string; description: string }[] = [
    { key: 'rounds', icon: 'eye', label: 'Rondas y observaciones', description: 'Revisiones, devoluciones y comentarios.' },
    ...(approved ? [{ key: 'execution' as const, icon: 'document' as const, label: 'Ejecución y evidencias', description: 'Seguimiento de las actividades del Plan aprobado.' }] : []),
    { key: 'history', icon: 'clock', label: 'Historial del documento', description: 'Registro de cambios y acciones realizadas.' },
  ]
  return <div className="document-follow-up" ref={root}
    onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false) }}
    onKeyDown={event => {
      if (event.key === 'Escape' && open) { event.preventDefault(); event.stopPropagation(); setOpen(false); trigger.current?.focus() }
    }}>
    <button
      type="button"
      className={'secondary document-follow-up-trigger' + (open ? ' is-open' : '')}
      ref={trigger}
      aria-expanded={open}
      aria-controls={id}
      onClick={() => setOpen(value => !value)}
      onKeyDown={event => {
        if (event.key === 'ArrowDown') {
          event.preventDefault(); setOpen(true)
          requestAnimationFrame(() => root.current?.querySelector<HTMLButtonElement>('.document-follow-up-panel button:not(:disabled)')?.focus())
        }
      }}
    >
      <Icon name="clock" size={17} />
      <span>Seguimiento del documento</span>
      <Icon name="chevronRight" className={'follow-up-chevron' + (open ? ' is-open' : '')} size={15} />
    </button>
    {open && <section className="document-follow-up-panel" id={id} aria-label="Opciones de seguimiento">
      <div className="document-follow-up-header">
        <span className="follow-up-kicker">TRAZABILIDAD</span>
        <p className="follow-up-header-title">Consulta y seguimiento del documento</p>
      </div>
      <div className="document-follow-up-list">
        {options.map(option => (
          <button
            key={option.key}
            type="button"
            className="secondary document-follow-up-item"
            aria-label={option.label}
            disabled={disabled}
            onClick={() => { setOpen(false); onOpen(option.key) }}
          >
            <span className="follow-up-icon-badge" aria-hidden="true">
              <Icon name={option.icon} size={18} />
            </span>
            <span className="follow-up-item-content">
              <strong>{option.label}</strong>
              <small>{option.description}</small>
            </span>
            <Icon name="chevronRight" size={15} className="follow-up-item-arrow" aria-hidden="true" />
          </button>
        ))}
      </div>
      {disabled && (
        <div role="status" className="follow-up-pending">
          <Icon name="lock" size={14} aria-hidden="true" />
          <span>{pendingChanges ? 'Guarde los cambios pendientes antes de abrir el seguimiento.' : 'Espere a que termine la operación en curso.'}</span>
        </div>
      )}
    </section>}
  </div>
}
