import { Icon } from '../shared/Icon'

export function StepPagination({ previous, next, disabled = false, nextDisabled = false }: {
  previous?: { label: string; action: () => void }; next?: { label: string; action: () => void }
  disabled?: boolean; nextDisabled?: boolean
}) {
  return <nav className="plan-step-pagination" aria-label="Navegación entre pasos del Plan">
    {previous ? <button type="button" className="secondary" disabled={disabled} onClick={previous.action}><Icon name="chevronLeft" size={17} /><span>Anterior: {previous.label}</span></button> : <span />}
    {next && <button type="button" className="plan-step-next" disabled={disabled || nextDisabled} onClick={next.action}><span>Siguiente: {next.label}</span><Icon name="arrow" size={17} /></button>}
  </nav>
}
