import { ActionForm } from '../shared/ActionForm'
import { useState, type FormEvent } from 'react'
import { api, download } from '../shared/api'
import { Icon } from '../shared/Icon'

export type Evidence = { id: string; version: number; originalName: string | null; uploadedBy: string; uploadedAt: string; evidenceKind: 'PDF' | 'URL'; externalUrl: string | null; referenceTitle: string | null }
export type Medium = { index: number; label: string; evidenceKind: 'PDF' | 'URL'; versions: Evidence[] }
export function EvidenceMedium({ path, activityId, medium, canUpload, onDirty, onSaved, onError }: { path: string; activityId: string; medium: Medium; canUpload: boolean; onDirty: (dirty: boolean) => void; onSaved: () => void; onError: (message: string) => void }) {
  const [busy, setBusy] = useState(false)
  const latest = medium.versions[0], external = medium.evidenceKind === 'URL'
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = event.currentTarget, data = new FormData(form); setBusy(true)
    try {
      const target = path + '/activities/' + activityId + '/media/' + medium.index
      if (external) await api(target + '/reference', 'POST', { expectedVersion: latest?.version ?? 0, title: data.get('title'), url: data.get('url') })
      else await api(target + '?expectedVersion=' + (latest?.version ?? 0), 'POST', data)
      form.reset(); onDirty(false); onSaved()
    } catch (e) { onError(e instanceof Error ? e.message : 'No se pudo guardar la evidencia.') } finally { setBusy(false) }
  }
  function action(e: Evidence) {
    return e.evidenceKind === 'URL' && e.externalUrl ? <a className="evidence-open" href={e.externalUrl} target="_blank" rel="noopener noreferrer" referrerPolicy="no-referrer" title="Abrir enlace externo en otra pestaña"><Icon name="arrow" size={16} />Abrir enlace</a> : <button type="button" className="link" onClick={() => download(path + '/evidence/' + e.id + '/content', e.originalName ?? 'evidencia.pdf').catch(error => onError(error.message))}>Descargar {e.originalName}</button>
  }
  return <section className={'evidence-medium evidence-panel ' + (latest ? 'is-covered' : '')} aria-label={'Evidencia de ' + medium.label}>
    <header className="evidence-heading"><span className="section-icon"><Icon name={external ? 'arrow' : 'document'} /></span><div><small className="section-kicker">{external ? 'REFERENCIA EXTERNA' : 'ARCHIVO PRIVADO'}</small><h3>{medium.label}</h3></div><span className={'status-badge ' + (latest ? 'active' : '')}>{latest ? 'v' + latest.version + '.0' : 'Pendiente'}</span></header>
    {latest ? <div className="evidence-current"><strong>{latest.referenceTitle ?? latest.originalName}</strong>{latest.externalUrl && <p className="evidence-url">{latest.externalUrl}</p>}<small>{latest.uploadedBy} · {new Date(latest.uploadedAt).toLocaleString('es-EC')}</small>{action(latest)}</div> : <div className="evidence-empty"><Icon name={external ? 'arrow' : 'document'} size={28} /><p>Sin evidencia registrada.</p><small>{external ? 'Registre un título y un enlace HTTPS verificable.' : 'Adjunte un PDF por este medio de verificación.'}</small></div>}
    {medium.versions.length > 0 && <details className="evidence-history"><summary>Ver historial · {medium.versions.length} {medium.versions.length === 1 ? 'versión' : 'versiones'}</summary><ol>{medium.versions.map(e => <li key={e.id}><span className="status-badge">v{e.version}.0</span><div><strong>{e.referenceTitle ?? e.originalName}</strong><small>{e.uploadedBy} · {new Date(e.uploadedAt).toLocaleString('es-EC')}</small>{e.externalUrl && <p className="evidence-url">{e.externalUrl}</p>}</div>{action(e)}</li>)}</ol></details>}
    {canUpload && <ActionForm label="Registrar o reemplazar evidencia" disabled={busy} onCancel={() => onDirty(false)}><form className="evidence-form" onChange={() => onDirty(true)} onSubmit={save}><fieldset disabled={busy}>{external ? <><div className="grid"><label>Título de referencia de {medium.label}<input name="title" required maxLength={200} placeholder="Nombre del documento o recurso" /></label><label>Enlace HTTPS de {medium.label}<input type="url" name="url" required pattern="https://.+" maxLength={2048} placeholder="https://…" /></label></div><p className="field-help">La referencia se conserva por versión. El sitio externo controla su contenido y acceso; evite enlaces con contraseñas o tokens privados.</p></> : <label>{latest ? 'Reemplazar PDF de ' : 'Cargar PDF de '}{medium.label}<input type="file" name="file" accept="application/pdf,.pdf" required /></label>}<button><Icon name="check" size={16} />{busy ? 'Guardando…' : latest ? 'Guardar nueva versión' : 'Guardar evidencia'}</button></fieldset></form></ActionForm>}
  </section>
}
