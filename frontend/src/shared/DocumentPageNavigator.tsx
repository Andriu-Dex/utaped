import { useState } from 'react'
import { Icon } from './Icon'
import './document-page-navigator.css'

export function DocumentPageNavigator({ page, pageCount, onChange, disabled = false, references = [] }: {
  page: number; pageCount: number; onChange: (page: number) => void; disabled?: boolean; references?: { page: number; label: string }[]
}) {
  const [input, setInput] = useState<string | null>(null)
  const candidate = Number(input ?? page + 1)
  const valid = Number.isInteger(candidate) && candidate >= 1 && candidate <= pageCount
  const shortcuts = references.filter(reference => reference.page >= 0 && reference.page < pageCount)
  function go(target: number) { if (!disabled && target >= 0 && target < pageCount) { onChange(target); setInput(null) } }
  return <nav className="document-page-navigator" aria-label="Páginas del PDF">
    <div className="document-navigator-heading"><span className="section-icon"><Icon name="document" size={18} /></span><div><strong>Explorar el documento</strong><span role="status">Página {page + 1} de {pageCount}</span></div><div className="document-page-progress" aria-hidden="true"><span style={{ width: ((page + 1) / pageCount * 100) + '%' }} /></div></div>
    <div className="document-navigator-controls"><button type="button" className="secondary" disabled={disabled || page === 0} onClick={() => go(page - 1)}>Página anterior</button><label>Ir a página<div><input type="number" min={1} max={pageCount} step={1} value={input ?? page + 1} disabled={disabled} aria-invalid={!valid} onChange={event => setInput(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') { event.preventDefault(); if (valid) go(candidate - 1) } }} /><button type="button" className="secondary" disabled={disabled || !valid} aria-label="Ir a la página indicada" title="Ir a la página indicada" onClick={() => go(candidate - 1)}><Icon name="arrow" size={16} /></button></div></label><button type="button" className="secondary" disabled={disabled || page + 1 === pageCount} onClick={() => go(page + 1)}>Página siguiente</button>{shortcuts.length > 0 && <label>Referencias del documento<select value="" disabled={disabled} onChange={event => { if (event.target.value !== '') go(Number(event.target.value)) }}><option value="">Selecciona una referencia</option>{shortcuts.map((reference, i) => <option key={i} value={reference.page}>{reference.label} · pág. {reference.page + 1}</option>)}</select></label>}</div>
    {!valid && <p className="field-help">Introduce una página entre 1 y {pageCount}.</p>}
  </nav>
}
