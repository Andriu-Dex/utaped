import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { SignaturePreparation } from './SignaturePreparation'
interface Readiness { rowVersion: number; ready: boolean; blockers: { section: string; message: string }[] }
interface Artifact { id: string; pageCount: number; sourceRowVersion: number; createdAt: string; current: boolean; pages: { number: number; kind: string; width: number; height: number }[]; signatureSlots: { label: string; pageNumber: number }[] }
export function DocumentPreparation({ targetDocId, onBack }: { targetDocId: string; onBack: () => void }) {
  const path = '/work-plans/' + targetDocId + '/artifacts'
  const [ready, setReady] = useState<Readiness | null>(null); const [artifacts, setArtifacts] = useState<Artifact[]>([])
  const [selected, setSelected] = useState<Artifact | null>(null); const [page, setPage] = useState(0)
  const [error, setError] = useState(''); const [busy, setBusy] = useState(false); const [imageError, setImageError] = useState(false)
  useEffect(() => { let active = true; Promise.all([api<Readiness>(path + '/readiness'), api<Artifact[]>(path)]).then(([r, a]) => { if (active) { setReady(r); setArtifacts(a); setSelected(a[0] ?? null) } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [path])
  async function generate() {
    if (!ready) return; setBusy(true); setError('')
    try { const artifact = await api<Artifact>(path, 'POST', { rowVersion: ready.rowVersion }); setSelected(artifact); setPage(0); setImageError(false); setArtifacts(await api<Artifact[]>(path)) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo generar el PDF.') }
    finally { setBusy(false) }
  }
  return <section><button className="secondary" disabled={busy} onClick={onBack}>Volver al Plan</button><h1>Preparación y previsualización T1</h1><p>PDF basado en la plantilla institucional, con páginas T1 en A4 y matriz horizontal. Los anexos conservan su formato original. Esta previsualización conserva sus bytes y contenido; el Plan continúa en borrador.</p>
    <p>Previsualización sin firma. Revise el contenido antes de continuar con la elaboración del Plan.</p>
    {error && <p role="alert" className="error">{error}</p>}
    {ready && <div className="card"><h2>Revisión de preparación</h2>{ready.ready ? <p role="status">Listo para generar la previsualización.</p> : <ul>{ready.blockers.map((b, i) => <li key={i}><strong>{b.section}:</strong> {b.message}</li>)}</ul>}<button disabled={busy || !ready.ready} onClick={() => void generate()}>{busy ? 'Generando PDF…' : 'Generar previsualización'}</button></div>}
    {artifacts.length > 0 && <label>Previsualizaciones guardadas<select value={selected?.id ?? ''} disabled={busy} onChange={e => { setSelected(artifacts.find(a => a.id === e.target.value) ?? null); setPage(0); setImageError(false) }}>{artifacts.map(a => <option key={a.id} value={a.id}>{new Date(a.createdAt).toLocaleString('es-EC')} · {a.pageCount} páginas · {a.current ? 'Contenido actual' : 'Contenido anterior'}</option>)}</select></label>}
    {selected && <div className="card"><p>{selected.current ? 'Coincide con el contenido actual.' : 'Previsualización anterior: el contenido del borrador o la configuración ha cambiado.'}</p><a href={'/api' + path + '/' + selected.id + '/content'}>Descargar PDF T1</a><p>Espacio de elaboración: {selected.signatureSlots.map(s => s.label + ', página ' + s.pageNumber).join('; ')}. Sin firma aplicada.</p>
      <SignaturePreparation key={targetDocId + selected.id} targetDocId={targetDocId} artifactId={selected.id} />
      <nav aria-label="Páginas del PDF"><button className="secondary" disabled={page === 0} onClick={() => { setPage(page - 1); setImageError(false) }}>Página anterior</button><span role="status"> Página {page + 1} de {selected.pageCount} </span><button className="secondary" disabled={page + 1 === selected.pageCount} onClick={() => { setPage(page + 1); setImageError(false) }}>Página siguiente</button></nav>
      {imageError && <p role="alert" className="error">No se pudo cargar la página. Descargue el PDF o vuelva a seleccionar la previsualización.</p>}
      <img className="document-page" key={selected.id + '-' + page} width={Math.round(selected.pages[page].width * 1.5)} height={Math.round(selected.pages[page].height * 1.5)} src={'/api' + path + '/' + selected.id + '/pages/' + page} alt={'Página ' + (page + 1) + ' del Plan T1'} onError={() => setImageError(true)} />
    </div>}
  </section>
}
