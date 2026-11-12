import { FileInput } from '../shared/FileInput'
import { useEffect, useState, type FormEvent } from 'react'
import { api, download } from '../shared/api'
import { ActionForm } from '../shared/ActionForm'
import { Toast } from '../shared/Toast'
import { confirmAction } from '../shared/confirm-action'
type Section = { key: string; label: string; required: boolean; initialText: string }
type Transfer = { formatVersion: number; documentType: string; sections: Section[] }
type Structure = { revision: number; sections: Section[] }
export function DocumentStructures({ onDirtyChange }: { onDirtyChange: (dirty: boolean) => void }) {
  const [type, setType] = useState('T1'), [saved, setSaved] = useState<Structure | null>(null), [rows, setRows] = useState<Section[]>([]), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  const [imported, setImported] = useState<Transfer | null>(null)
  const [reload, setReload] = useState(0)
  const dirty = !!imported || !!saved && JSON.stringify(saved.sections) !== JSON.stringify(rows)
  useEffect(() => { onDirtyChange(dirty); return () => onDirtyChange(false) }, [dirty, onDirtyChange])
  useEffect(() => { let active = true; api<Structure>('/admin/document-structures/' + type).then(value => { if (active) { setSaved(value); setRows(value.sections); setError('') } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [type, reload])
  async function save(e: FormEvent) { e.preventDefault(); if (!saved) return; setBusy(true); setError(''); try { const value = await api<Structure>('/admin/document-structures/' + type, 'PUT', { revision: saved.revision, sections: rows }); setSaved(value); setRows(value.sections); setNotice('Estructura publicada. Se aplicará solo a documentos nuevos.') } catch(e) { setError(e instanceof Error ? e.message : 'No se pudo publicar; los cambios se conservan.') } finally { setBusy(false) } }
  async function readStructure(file?: File) {
    if (!file) return
    setError('')
    try {
      if (file.size > 350000) throw new Error('El archivo de estructura supera 350 KB.')
      const value: unknown = JSON.parse(await file.text())
      if (!value || typeof value !== 'object' || !('formatVersion' in value) || value.formatVersion !== 1 || !('documentType' in value) || value.documentType !== type || !('sections' in value) || !Array.isArray(value.sections) || !value.sections.length || value.sections.length > 30) throw new Error('Seleccione un JSON de estructura compatible con este formato documental.')
      if (!value.sections.every(section => section && typeof section.key === 'string' && typeof section.label === 'string' && typeof section.required === 'boolean' && typeof section.initialText === 'string')) throw new Error('El archivo contiene secciones no válidas.')
      setImported(value as Transfer)
    } catch (e) { setImported(null); setError(e instanceof Error ? e.message : 'No se pudo leer la estructura.') }
  }
  async function publishImported() {
    if (!saved || !imported) return
    setBusy(true); setError('')
    try {
      const value = await api<Structure>('/admin/document-structures/' + type + '/import', 'POST', { revision: saved.revision, structure: imported })
      setSaved(value); setRows(value.sections); setImported(null); setNotice('Estructura importada y publicada para documentos nuevos.')
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo importar. Revise el archivo.') }
    finally { setBusy(false) }
  }
  function move(index: number, delta: number) { setRows(current => { const next = [...current]; [next[index], next[index + delta]] = [next[index + delta], next[index]]; return next }) }
  return <section className="card" aria-label="Estructura de documentos"><span className="section-kicker">ESTRUCTURA VERSIONADA</span><h2>Secciones y orden del documento</h2><p className="muted">Configure el orden y los apartados adicionales. El encabezado, las tablas oficiales y las firmas conservan su formato. Cada documento guarda una copia de su estructura.</p><label>Formato documental<select aria-label="Formato documental" value={type} disabled={busy} onChange={async e => { const next = e.currentTarget.value; if (!dirty || await confirmAction('¿Descartar los cambios de estructura para cambiar de formato?')) { setImported(null); setSaved(null); setType(next); setNotice('') } }}><option value="T1">Plan de Trabajo</option><option value="T2">Informe final</option><option value="T3">Informe de ejecución</option></select></label>
    {error && <><Toast message={error} onDismiss={() => setError('')} /><button type="button" className="secondary" disabled={busy || dirty} onClick={() => setReload(value => value + 1)}>Reintentar estructura documental</button></>}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    {saved && <><div className="review-actions"><button type="button" className="secondary" disabled={busy} onClick={() => void download('/admin/document-structures/' + type + '/export', 'estructura-' + type + '.json').catch(e => setError(e.message))}>Exportar estructura JSON</button><ActionForm key={'import-' + type + saved.revision} label="Importar estructura JSON" disabled={busy || JSON.stringify(saved.sections) !== JSON.stringify(rows)} dirty={!!imported} onCancel={() => setImported(null)}><label>Archivo de estructura<FileInput accept=".json,application/json" disabled={busy} onChange={e => void readStructure(e.currentTarget.files?.[0])} /></label>{imported && <><p>{imported.sections.length} secciones para {type}. Se aplicará únicamente a documentos nuevos.</p><ol>{imported.sections.map(section => <li key={section.key}>{section.label}</li>)}</ol><button type="button" disabled={busy} onClick={() => void publishImported()}>Publicar estructura importada</button></>}</ActionForm></div><p>Versión de estructura {saved.revision} · {rows.length} secciones</p><ActionForm key={type + saved.revision} label="Editar estructura" dirty={dirty} disabled={busy} onCancel={() => setRows(saved.sections)}><form onSubmit={save}><fieldset disabled={busy}>{rows.map((section, index) => { const custom = section.key.startsWith('CUSTOM_'), fixed = index >= rows.length - 2; return <section className="card" key={section.key}><div className="section-heading"><strong>{index + 1}. {section.label}</strong><span className="status-badge">{custom ? 'Adicional' : 'Institucional'}</span></div>{custom && <><label>Título de la sección<input value={section.label} maxLength={160} required onChange={e => setRows(rows.map(s => s.key === section.key ? { ...s, label: e.target.value } : s))} /></label><label className="check"><input type="checkbox" checked={section.required} onChange={e => setRows(rows.map(s => s.key === section.key ? { ...s, required: e.target.checked } : s))} />Obligatoria para preparar el documento</label><label>Texto inicial<textarea rows={3} maxLength={10000} value={section.initialText} onChange={e => setRows(rows.map(s => s.key === section.key ? { ...s, initialText: e.target.value } : s))} /></label></>}<div className="review-actions"><button type="button" className="secondary" aria-label={'Subir ' + section.label} disabled={fixed || index === 0} onClick={() => move(index, -1)}>↑ Subir</button><button type="button" className="secondary" aria-label={'Bajar ' + section.label} disabled={fixed || index >= rows.length - 3} onClick={() => move(index, 1)}>↓ Bajar</button>{custom && <button type="button" className="secondary" onClick={async () => { if (await confirmAction('¿Quitar esta sección de la siguiente estructura? Los documentos existentes la conservarán.')) setRows(current => current.filter(s => s.key !== section.key)) }}>Quitar sección</button>}</div></section> })}<div className="action-form-actions"><button type="button" className="secondary" disabled={rows.filter(section => section.key.startsWith('CUSTOM_')).length >= 20} onClick={() => setRows([...rows.slice(0, -2), { key: 'CUSTOM_' + crypto.randomUUID(), label: 'Nueva sección', required: false, initialText: '' }, ...rows.slice(-2)])}>Agregar sección</button><button disabled={!dirty}>Publicar estructura</button></div></fieldset></form></ActionForm></>}
  </section>
}
