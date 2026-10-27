import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'

export function SignaturePlacement({ targetDocId, artifactId, slots, onEditAuthors, disabled, current }: {
  targetDocId: string; artifactId: string; current: boolean
  slots: { actorId?: string; label: string; pageNumber: number }[]
  onEditAuthors?: () => void; disabled: boolean
}) {
  const [authors, setAuthors] = useState<{ id: string; name: string; position: string }[] | null>(null)
  const [error, setError] = useState(''), [retry, setRetry] = useState(0)
  useEffect(() => {
    if (!current) return
    let active = true
    api<{ authors: NonNullable<typeof authors> }>('/work-plans/' + targetDocId + '/authors')
      .then(value => { if (active) { setAuthors(value.authors); setError('') } })
      .catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [targetDocId, artifactId, retry, current])
  return <section className="card" aria-label="Firmantes y ubicación">
    <h3>Elaboradores y ubicación de las firmas</h3>
    <p className="field-help">Los espacios proceden del documento institucional. Compruebe los nombres, cargos y páginas antes de la revisión final. Aquí todavía no se aplica ninguna firma.</p>
    {error && <><Toast message={error} /><button type="button" className="secondary" onClick={() => setRetry(value => value + 1)}>Reintentar elaboradores</button></>}
    {current && !authors && !error && <p role="status">Consultando elaboradores…</p>}
    {current && authors && <ol className="signature-placement-list">{authors.map(person => {
      const locations = slots.filter(slot => slot.actorId === person.id)
      return <li key={person.id}><strong>{person.name}</strong><span>{person.position || 'Cargo pendiente de configurar'}</span><span>{locations.length ? locations.map(slot => slot.label + ' · página ' + slot.pageNumber).join('; ') : 'Consulte los espacios previstos en el visor.'}</span></li>
    })}</ol>}
    {!current && <><p className="field-help">Este artefacto pertenece a una preparación anterior. Compruebe sus nombres y cargos directamente en el documento conservado.</p><ul>{slots.map((slot, index) => <li key={index}>{slot.label} · página {slot.pageNumber}</li>)}</ul></>}
    {onEditAuthors && <button type="button" className="secondary" disabled={disabled} onClick={onEditAuthors}>Revisar elaboradores en información general</button>}
  </section>
}
