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
    <button type="button" className="secondary document-follow-up-trigger" ref={trigger} aria-expanded={open} aria-controls={id}
      onClick={() => setOpen(value => !value)}
      onKeyDown={event => {
        if (event.key === 'ArrowDown') {
          event.preventDefault(); setOpen(true)
          requestAnimationFrame(() => root.current?.querySelector<HTMLButtonElement>('.document-follow-up-panel button:not(:disabled)')?.focus())
        }
      }}><Icon name="clock" size={17} />Seguimiento del documento<Icon name="chevronRight" className={'follow-up-chevron' + (open ? ' is-open' : '')} size={15} /></button>
    {open && <section className="document-follow-up-panel" id={id} aria-label="Opciones de seguimiento">
      <p>Consulta y seguimiento del documento</p>
      {options.map(option => <button key={option.key} type="button" className="secondary" aria-label={option.label} disabled={disabled} onClick={() => { setOpen(false); onOpen(option.key) }}>
        <Icon name={option.icon} size={18} /><span><strong>{option.label}</strong><small>{option.description}</small></span>
      </button>)}
      {disabled && <small role="status" className="follow-up-pending">{pendingChanges ? 'Guarde los cambios pendientes antes de abrir el seguimiento.' : 'Espere a que termine la operación en curso.'}</small>}
    </section>}
  </div>
}
