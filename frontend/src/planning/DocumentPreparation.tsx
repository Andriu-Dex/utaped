import { Toast } from '../shared/Toast'
import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { SignaturePreparation } from './SignaturePreparation'
import { AuthorSigning } from './AuthorSigning'
import { Icon } from '../shared/Icon'
import { DocumentPageNavigator } from '../shared/DocumentPageNavigator'
import type { WorkPlan } from './types'
interface Readiness { rowVersion: number; ready: boolean; blockers: { section: string; message: string }[] }
interface Artifact { id: string; pageCount: number; sourceRowVersion: number; createdAt: string; current: boolean; pages: { number: number; kind: string; width: number; height: number }[]; signatureSlots: { label: string; pageNumber: number }[] }
export function DocumentPreparation({ targetDocId, onBack, documentType = 'T1', mode = 'combined' }: { targetDocId: string; onBack: () => void; documentType?: 'T1' | 'T2'; mode?: 'preview' | 'sign' | 'combined' }) {
  const path = '/work-plans/' + targetDocId + '/artifacts'
  const [ready, setReady] = useState<Readiness | null>(null); const [artifacts, setArtifacts] = useState<Artifact[]>([])
  const [selected, setSelected] = useState<Artifact | null>(null); const [page, setPage] = useState(0); const [plan, setPlan] = useState<WorkPlan | null>(null)
  const [error, setError] = useState(''); const [busy, setBusy] = useState(false); const [imageError, setImageError] = useState(false); const [reload, setReload] = useState(0)
  const [loadedImage, setLoadedImage] = useState('')
  const label = documentType === 'T2' ? 'Informe' : 'Plan'
  useEffect(() => { let active = true; Promise.all([api<Readiness>(path + '/readiness'), api<Artifact[]>(path), api<WorkPlan>('/work-plans/' + targetDocId)]).then(([r, a, p]) => { if (active) { setReady(r); setArtifacts(a); setPlan(p); setSelected(a[0] ?? null); setPage(0) } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [path, targetDocId, reload])
  async function generate() {
    if (!ready) return; setBusy(true); setError('')
    try { const artifact = await api<Artifact>(path, 'POST', { rowVersion: ready.rowVersion }); setSelected(artifact); setPage(0); setImageError(false); setArtifacts(await api<Artifact[]>(path)) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo generar el PDF.') }
    finally { setBusy(false) }
  }
  return <section><button className="secondary" disabled={busy} onClick={onBack}>Volver al {label}</button><span className="section-kicker review-kicker">DOCUMENTO INSTITUCIONAL · {documentType}</span><h1>{mode === 'sign' ? 'Firma y envío' : 'Preparación y previsualización ' + documentType}</h1><p className="muted">Formato institucional en A4. Los anexos y evidencias conservan sus páginas originales. Cada artefacto se guarda completo y no se reconstruye después de firmarlo.</p>
    {error && <Toast message={error} onDismiss={() => setError('')} />}
    {!ready ? <p role="status">Consultando preparación…</p> : <div className="card"><div className="section-heading"><span className="section-icon"><Icon name="document" /></span><div><span className="section-kicker">PREPARACIÓN DOCUMENTAL</span><h2>Revisión del contenido</h2></div></div>{ready.ready ? <p role="status">Contenido completo para previsualizar.</p> : <ul>{ready.blockers.map((b, i) => <li key={i}><strong>{b.section}:</strong> {b.message}</li>)}</ul>}{plan?.editable ? <button disabled={busy || !ready.ready} onClick={() => void generate()}>{busy ? 'Generando PDF…' : 'Generar previsualización'}</button> : <p className="field-help">Documento de solo lectura. Consulte su artefacto conservado y las rondas de revisión; la descarga final se habilita al aprobarse.</p>}</div>}
    {artifacts.length > 0 && <label>Previsualizaciones guardadas<select value={selected?.id ?? ''} disabled={busy} onChange={e => { setSelected(artifacts.find(a => a.id === e.target.value) ?? null); setPage(0); setImageError(false) }}>{artifacts.map(a => <option value={a.id} key={a.id}>{new Date(a.createdAt).toLocaleString('es-EC')} · {a.pageCount} páginas · {a.current ? 'Contenido actual' : 'Artefacto conservado'}</option>)}</select></label>}
    {selected && <div className="card"><p>{selected.current ? 'Coincide con el contenido actual.' : 'Artefacto conservado de una preparación anterior. Consulte las rondas para ver los bytes firmados.'}</p><p>Espacios de firma: {selected.signatureSlots.map(s => s.label + ', página ' + s.pageNumber).join('; ')}.</p>
      {mode !== 'preview' && <SignaturePreparation key={targetDocId + selected.id} targetDocId={targetDocId} artifactId={selected.id} />}
      {mode !== 'preview' && <AuthorSigning key={'sign-' + targetDocId + selected.id} targetDocId={targetDocId} artifactId={selected.id} onSigned={() => { setReload(n => n + 1) }} />}
      <DocumentPageNavigator key={selected.id} page={page} pageCount={selected.pageCount} disabled={busy} onChange={target => { setPage(target); setImageError(false) }} references={[...selected.signatureSlots.map(slot => ({ page: slot.pageNumber - 1, label: slot.label })), ...selected.pages.filter((item, index) => item.kind === 'ANNEX' && selected.pages[index - 1]?.kind !== 'ANNEX').map(item => ({ page: item.number - 1, label: 'Anexos y evidencias' }))]} />
      {imageError && <Toast message="No se pudo cargar la página. Vuelva a seleccionar la previsualización para reintentar." />}
      {!imageError && loadedImage !== selected.id + '-' + page && <p role="status" className="field-help">Cargando página {page + 1}…</p>}
      <img className="document-page" key={selected.id + '-' + page} width={Math.round(selected.pages[page].width * 1.5)} height={Math.round(selected.pages[page].height * 1.5)} src={'/api' + path + '/' + selected.id + '/pages/' + page} alt={'Página ' + (page + 1) + ' del ' + label + ' ' + documentType} onLoad={() => setLoadedImage(selected.id + '-' + page)} onError={() => setImageError(true)} />
    </div>}
  </section>
}
