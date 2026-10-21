import { DocumentStructures } from './DocumentStructures'
import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../shared/api'
import { confirmAction } from '../shared/confirm-action'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'
import './template-admin.css'

export interface DocumentTemplate {
  documentType: string
  name: string
  description: string
  justification: string
  rowVersion: number
  updatedAt: string
  updatedBy: string | null
  updatedByName: string | null
}

export function TemplateAdministration({ onDirtyChange }: { onDirtyChange: (dirty: boolean) => void }) {
  const [templates, setTemplates] = useState<DocumentTemplate[]>([])
  const [editingType, setEditingType] = useState<string | null>(null)
  const [draftJustification, setDraftJustification] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const activeTemplate = templates.find(t => t.documentType === editingType)
  const [structureDirty, setStructureDirty] = useState(false)
  const textDirty = editingType !== null && activeTemplate !== undefined && draftJustification !== activeTemplate.justification

  const dirty = textDirty || structureDirty
  useEffect(() => {
    onDirtyChange(dirty)
    const prevent = (event: BeforeUnloadEvent) => {
      if (dirty) {
        event.preventDefault()
        event.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', prevent)
    return () => {
      onDirtyChange(false)
      window.removeEventListener('beforeunload', prevent)
    }
  }, [dirty, onDirtyChange])

  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true
    Promise.resolve().then(() => {
      if (active) {
        setBusy(true)
        setError('')
      }
    })
    api<DocumentTemplate[]>('/admin/document-templates')
      .then(items => {
        if (!active) return
        setTemplates(items)
      })
      .catch(e => {
        if (!active) return
        setError(e instanceof Error ? e.message : 'No se pudieron cargar las plantillas institucionales.')
      })
      .finally(() => {
        if (active) setBusy(false)
      })
    return () => {
      active = false
    }
  }, [reload])

  function startEditing(template: DocumentTemplate) {
    setEditingType(template.documentType)
    setDraftJustification(template.justification)
    setError('')
    setNotice('')
  }

  async function cancelEditing() {
    if (dirty && !(await confirmAction('Hay cambios sin guardar. ¿Desea descartarlos?'))) {
      return
    }
    setEditingType(null)
    setDraftJustification('')
    setError('')
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault()
    if (!activeTemplate || !dirty) return

    setBusy(true)
    setError('')
    setNotice('')
    try {
      const updated = await api<DocumentTemplate>('/admin/document-templates/' + activeTemplate.documentType, 'PUT', {
        rowVersion: activeTemplate.rowVersion,
        justification: draftJustification.trim(),
      })
      setTemplates(prev => prev.map(t => (t.documentType === updated.documentType ? updated : t)))
      setDraftJustification(updated.justification)
      setEditingType(null)
      setNotice('Plantilla institucional actualizada correctamente.')
      setReload(r => r + 1)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar la plantilla.')
    } finally {
      setBusy(false)
    }
  }

  async function handleReset(template: DocumentTemplate) {
    if (
      !(await confirmAction(
        '¿Desea restablecer esta plantilla al texto predeterminado institucional de fábrica? Se reemplazarán las personalizaciones actuales.',
      ))
    ) {
      return
    }

    setBusy(true)
    setError('')
    setNotice('')
    try {
      const reset = await api<DocumentTemplate>('/admin/document-templates/' + template.documentType + '/reset', 'POST', {
        rowVersion: template.rowVersion,
      })
      setTemplates(prev => prev.map(t => (t.documentType === reset.documentType ? reset : t)))
      if (editingType === template.documentType) {
        setDraftJustification(reset.justification)
      }
      setNotice('Plantilla restablecida al texto institucional de fábrica.')
      setReload(r => r + 1)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo restablecer la plantilla.')
    } finally {
      setBusy(false)
    }
  }

  const wordCount = (text: string) => (text.trim() ? text.trim().split(/\s+/).length : 0)

  return (
    <section className="card" aria-label="Plantillas institucionales de documentos">
      <div className="section-heading">
        <div className="section-heading-icon" aria-hidden="true">
          <Icon name="document" size={24} />
        </div>
        <div>
          <h2>Plantillas institucionales de documentos</h2>
          <p className="muted">
            Configure el texto inicial de la justificación del Plan de Trabajo T1.
            Se precarga solo en documentos nuevos; no modifica el formato A4 ni los documentos ya creados.
          </p>
        </div>
      </div>

      {error && <Toast message={error} />}
      {notice && <Toast tone="success" message={notice} />}

      <div className="template-admin-container">
        {templates.map(template => {
          const isEditing = editingType === template.documentType
          const currentWords = isEditing ? wordCount(draftJustification) : wordCount(template.justification)
          const currentChars = isEditing ? draftJustification.length : template.justification.length

          return (
            <article key={template.documentType} className="template-card">
              <header className="template-card-header">
                <div className="template-card-title-group">
                  <span className="template-badge">{template.documentType}</span>
                  <div>
                    <h3 style={{ margin: 0 }}>{template.name}</h3>
                    <p className="muted" style={{ margin: '4px 0 0', fontSize: '0.9rem' }}>
                      {template.description}
                    </p>
                  </div>
                </div>
                {!isEditing && (
                  <button
                    type="button"
                    className="secondary"
                    disabled={busy}
                    onClick={() => startEditing(template)}
                    style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
                  >
                    <Icon name="edit" size={16} />
                    Editar plantilla
                  </button>
                )}
              </header>

              <div className="template-meta-row">
                <div className="template-meta-stats">
                  <span>
                    <strong>{currentWords}</strong> palabras
                  </span>
                  <span>
                    <strong>{currentChars}</strong> caracteres
                  </span>
                </div>
                <div>
                  <span>
                    Última modificación:{' '}
                    <strong>
                      {new Date(template.updatedAt).toLocaleDateString('es-EC', {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </strong>{' '}
                    {template.updatedByName ? `por ${template.updatedByName}` : '(Configuración institucional inicial)'}
                  </span>
                </div>
              </div>

              {isEditing ? (
                <form onSubmit={handleSave}>
                  <label htmlFor={`template-justification-${template.documentType}`} style={{ fontWeight: 600, display: 'block', marginBottom: 8 }}>
                    Texto predeterminado de la Justificación:
                  </label>
                  <textarea
                    id={`template-justification-${template.documentType}`}
                    rows={8}
                    required
                    maxLength={50000}
                    value={draftJustification}
                    disabled={busy}
                    onChange={e => setDraftJustification(e.target.value)}
                    style={{ width: '100%', resize: 'vertical', lineHeight: 1.6 }}
                  />
                  <div className="template-form-actions">
                    <button
                      type="button"
                      className="secondary"
                      disabled={busy}
                      onClick={() => void handleReset(template)}
                      style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
                    >
                      <Icon name="refresh" size={15} />
                      Restaurar valor de fábrica
                    </button>
                    <div className="template-form-right-actions">
                      <button type="button" className="secondary" disabled={busy} onClick={() => void cancelEditing()}>
                        Cancelar
                      </button>
                      <button type="submit" disabled={busy || !dirty}>
                        Guardar cambios
                      </button>
                    </div>
                  </div>
                </form>
              ) : (
                <div className="template-preview-box">{template.justification}</div>
              )}
            </article>
          )
        })}
      </div>
      <DocumentStructures onDirtyChange={setStructureDirty} />
    </section>
  )
}
