import { useRef, useState } from 'react'

interface WorkspaceResizerProps {
  value: number
  onChange: (value: number) => void
  onResizingChange?: (isResizing: boolean) => void
  min?: number
  max?: number
  defaultValue?: number
}

export function WorkspaceResizer({
  value,
  onChange,
  onResizingChange,
  min = 38,
  max = 58,
  defaultValue = 46,
}: WorkspaceResizerProps) {
  const [isDragging, setIsDragging] = useState(false)
  const resizerRef = useRef<HTMLDivElement>(null)

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (e.button !== 0) return
    e.preventDefault()
    setIsDragging(true)
    onResizingChange?.(true)
    try {
      e.currentTarget.setPointerCapture(e.pointerId)
    } catch {
      // Ignore if pointer capture fails
    }
  }

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDragging) return
    const workspace = resizerRef.current?.closest('.plan-persistent-workspace') as HTMLElement | null
    if (!workspace) return
    const rect = workspace.getBoundingClientRect()
    if (rect.width <= 0) return
    const relativeX = e.clientX - rect.left
    const percent = Math.round((relativeX / rect.width) * 100)
    const clamped = Math.max(min, Math.min(max, percent))
    onChange(clamped)
  }

  const stopDragging = (e: React.PointerEvent<HTMLDivElement>) => {
    if (isDragging) {
      setIsDragging(false)
      onResizingChange?.(false)
      try {
        e.currentTarget.releasePointerCapture(e.pointerId)
      } catch {
        // Ignore if pointer capture was already released
      }
    }
  }

  const handleDoubleClick = () => {
    onChange(defaultValue)
  }

  return (
    <div
      ref={resizerRef}
      className={'plan-workspace-resizer' + (isDragging ? ' is-dragging' : '')}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={stopDragging}
      onPointerCancel={stopDragging}
      onDoubleClick={handleDoubleClick}
      title="Arrastre para ajustar el ancho · Doble clic para restablecer (46%)"
      role="separator"
      aria-orientation="vertical"
      aria-valuenow={value}
      aria-valuemin={min}
      aria-valuemax={max}
      aria-label="Separador de paneles"
    >
      <input
        type="range"
        className="plan-resizer-input"
        aria-label="Ancho del formulario"
        min={min}
        max={max}
        value={value}
        onChange={e => onChange(Number(e.target.value))}
      />
      <div className="plan-resizer-line" />
      <div className="plan-resizer-badge" aria-hidden="true">
        {value}%
      </div>
    </div>
  )
}

