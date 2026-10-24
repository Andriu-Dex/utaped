import { Toast } from '../shared/Toast'
import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Icon } from '../shared/Icon'

interface Preparation {
  targetDocId: string
  artifactId: string
  pdfHash: string
  artifactCurrent: boolean
  signingEnabled: boolean
  workflow: null | {
    revisionId: string
    revisionNumber: number
    name: string
    stages: {
      id: string
      label: string
      action: 'REVIEW' | 'VALIDATE' | 'APPROVE'
      recipientKind: string
      recipientLabel: string
      requiresSignature: boolean
      participants: { id: string; name: string; active: boolean }[]
    }[]
  }
  blockers: { section: string; message: string }[]
}

const actions: Record<string, string> = { REVIEW: 'Revisión', VALIDATE: 'Validación', APPROVE: 'Aprobación' }

export function SignaturePreparation({ targetDocId, artifactId }: { targetDocId: string; artifactId: string }) {
  const [preparation, setPreparation] = useState<Preparation | null>(null)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let active = true
    api<{ preparation: Preparation }>('/work-plans/' + targetDocId + '/artifacts/' + artifactId + '/signing')
      .then(result => { if (active) setPreparation(result.preparation) })
      .catch(e => { if (active) setError(e instanceof Error ? e.message : 'No se pudo consultar la preparación de firma.') })
    return () => { active = false }
  }, [targetDocId, artifactId, refresh])

  return (
    <section className="card signature-prep-card" aria-label="Preparación de firma">
      <div className="section-heading">
        <span className="section-icon"><Icon name="users" /></span>
        <div>
          <span className="section-kicker">CIRCUITO Y AUTORIZACIÓN</span>
          <h2>Firma y flujo del artefacto</h2>
        </div>
      </div>

      <div className="flow-explanation-banner">
        <Icon name="document" size={18} />
        <p>Las etapas se ejecutan en secuencia sobre el mismo PDF firmado. Solo la persona asignada a la tarea activa puede decidir; una etapa con firma obligatoria requiere su certificado personal.</p>
      </div>

      {error ? (
        <Toast message={error} />
      ) : !preparation ? (
        <p role="status" className="field-help">Consultando preparación de firma…</p>
      ) : (
        <>
          <div className="artifact-status-pill-row">
            <span className={'status-badge ' + (preparation.artifactCurrent ? 'status-approved' : 'status-in_review')}>
              <Icon name={preparation.artifactCurrent ? 'check' : 'refresh'} size={13} />
              <span>
                {preparation.artifactCurrent
                  ? 'Integridad comprobada · Coincide con la preparación actual'
                  : 'Artefacto conservado · Consulte los requisitos de firma para continuar con sus mismos bytes'}
              </span>
            </span>
            {preparation.workflow && (
              <span className="workflow-meta-badge">
                {preparation.workflow.name} · Rev. {preparation.workflow.revisionNumber}
              </span>
            )}
          </div>

          {preparation.workflow ? (
            <div className="workflow-pipeline-section">
              <div className="pipeline-header">
                <h3>{preparation.workflow.name} · revisión técnica {preparation.workflow.revisionNumber}</h3>
                <small className="muted">Configuración conservada al generar este artefacto. Consulte «Rondas y observaciones» para las asignaciones y decisiones ejecutadas.</small>
              </div>

              <ol className="workflow-pipeline">
                {preparation.workflow.stages.map((stage, idx) => (
                  <li key={stage.id} className="pipeline-stage-card">
                    <div className="stage-index-badge">{idx + 1}</div>
                    <div className="stage-card-body">
                      <div className="stage-header-row">
                        <strong className="stage-label">{stage.label}</strong>
                        <div className="stage-badges">
                          <span className={'action-tag action-' + stage.action.toLowerCase()}>
                            {actions[stage.action] ?? stage.action}
                          </span>
                          <span className={'sig-tag ' + (stage.requiresSignature ? 'sig-required' : 'sig-optional')}>
                            <Icon name={stage.requiresSignature ? 'lock' : 'eye'} size={12} />
                            <span>{stage.requiresSignature ? 'Firma obligatoria' : 'Sin firma personal'}</span>
                          </span>
                        </div>
                      </div>

                      {stage.recipientKind === 'COLLEGIATE' && (
                        <div className="stage-collegiate">
                          <Icon name="users" size={13} />
                          <span>Órgano colegiado: <strong>{stage.recipientLabel}</strong></span>
                        </div>
                      )}

                      <div className="stage-participants">
                        <Icon name="users" size={13} className="participant-icon" />
                        <span>
                          {stage.participants.length
                            ? stage.participants.map(p => p.name + (p.active ? '' : ' (inactivo al generar)')).join('; ')
                            : stage.recipientKind === 'COLLEGIATE'
                              ? 'Sin representantes individuales en esta configuración.'
                              : 'Sin participantes resueltos.'}
                        </span>
                      </div>
                    </div>
                  </li>
                ))}
              </ol>
            </div>
          ) : (
            <div className="empty-state">
              <Icon name="document" size={28} />
              <p>Este artefacto no contiene una revisión de flujo configurada.</p>
            </div>
          )}

          {preparation.blockers.length > 0 ? (
            <div className="signing-blockers-card">
              <div className="blockers-header">
                <span className="blockers-warning-icon" aria-hidden="true">⚠</span>
                <strong>Requisitos pendientes</strong>
              </div>
              <ul className="blockers-list">
                {preparation.blockers.map((blocker, i) => (
                  <li key={i}>
                    <span className="blocker-section-pill">{blocker.section}:</span>
                    <span className="blocker-message-text">{blocker.message}</span>
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <div className="signing-ready-card">
              <Icon name="check" size={16} />
              <span>Flujo preparado. La sección de firma comprueba también el certificado vinculado al elaborador.</span>
            </div>
          )}
        </>
      )}

      <div className="prep-footer-action">
        <button
          type="button"
          className="secondary"
          onClick={() => { setPreparation(null); setError(''); setRefresh(value => value + 1) }}
        >
          <Icon name="refresh" size={14} />
          <span>Actualizar preparación de firma</span>
        </button>
      </div>
    </section>
  )
}
