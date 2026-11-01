import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'
import '../planning/work-assistance.css'

interface Comparison { currentRound: number; previousRound: number | null; changes: { section: string; before: string; after: string }[] }
export function RoundComparison({ reviewId }: { reviewId: string }) {
  const [opened, setOpened] = useState(false), [data, setData] = useState<Comparison | null>(null), [error, setError] = useState(''), [reload, setReload] = useState(0)
  useEffect(() => {
    if (!opened) return
    let active = true
    api<Comparison>('/reviews/' + reviewId + '/comparison').then(value => { if (active) { setData(value); setError('') } }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [reviewId, opened, reload])
  return <section className="card round-comparison" aria-label="Comparación entre rondas">
    <header className="assistance-heading"><div className="section-heading"><span className="section-icon"><Icon name="columns" /></span><div><span className="section-kicker">TRAZABILIDAD</span><h2>Qué cambió entre rondas</h2></div></div><button type="button" className="secondary" aria-expanded={opened} aria-controls={'comparison-' + reviewId} onClick={() => setOpened(value => !value)}>{opened ? 'Cerrar comparación' : 'Comparar rondas'}</button></header>
    {opened && <div id={'comparison-' + reviewId}>
      {!data && !error && <p role="status">Comparando contenido conservado…</p>}
      {error && <><p role="alert">{error}</p><button type="button" className="secondary" onClick={() => { setError(''); setReload(value => value + 1) }}>Reintentar comparación</button></>}
      {data && (data.previousRound === null ? <p className="comparison-intro">Esta es la primera ronda; todavía no hay una anterior para comparar.</p> : <><p className="comparison-intro">Ronda {data.previousRound} → Ronda {data.currentRound} · {data.changes.length} {data.changes.length === 1 ? 'apartado modificado' : 'apartados modificados'}. Se compara el contenido conservado de ambas rondas; las firmas y fechas de preparación se consultan en sus PDF.</p>
        {data.changes.length === 0 && <p>No hay cambios en el contenido comparado.</p>}
        {data.changes.map(change => <details key={change.section}><summary>{change.section}<span className="sr-only">: apartado modificado</span></summary><div className="comparison-columns"><div><h4>Antes · Ronda {data.previousRound}</h4><div className="comparison-text">{change.before || 'Sin contenido'}</div></div><div><h4>Ahora · Ronda {data.currentRound}</h4><div className="comparison-text">{change.after || 'Sin contenido'}</div></div></div>{change.before === change.after && <p className="field-help">Cambió la configuración o el archivo asociado. Consulte el detalle en los PDF de cada ronda.</p>}</details>)}
      </>)}
    </div>}
  </section>
}
