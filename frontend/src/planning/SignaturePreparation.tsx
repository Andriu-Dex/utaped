import { useEffect, useState } from 'react'
import { api } from '../shared/api'

interface Preparation {
  targetDocId: string; artifactId: string; pdfHash: string; artifactCurrent: boolean; signingEnabled: boolean
  workflow: null | { revisionId: string; revisionNumber: number; name: string; stages: {
    id: string; label: string; action: 'REVIEW' | 'VALIDATE' | 'APPROVE'; recipientKind: string
    recipientLabel: string; requiresSignature: boolean; participants: { id: string; name: string; active: boolean }[]
  }[] }
  blockers: { section: string; message: string }[]
}
const actions = { REVIEW: 'Revisión', VALIDATE: 'Validación', APPROVE: 'Aprobación' }

export function SignaturePreparation({ targetDocId, artifactId }: { targetDocId: string; artifactId: string }) {
  const [preparation, setPreparation] = useState<Preparation | null>(null)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)
  useEffect(() => {
    let active = true
    api<Preparation>('/work-plans/' + targetDocId + '/artifacts/' + artifactId + '/signature-preparation')
      .then(result => { if (active) setPreparation(result) })
      .catch(e => { if (active) setError(e instanceof Error ? e.message : 'No se pudo consultar la preparación de firma.') })
    return () => { active = false }
  }, [targetDocId, artifactId, refresh])
  return <section aria-label="Preparación de firma"><h2>Firma y flujo del artefacto</h2>
    <p>Mecanismo previsto: archivo .p12/.pfx con contraseña. La firma, el envío y la aprobación permanecen deshabilitados hasta completar la validación institucional y la política de certificados.</p>
    {error ? <p role="alert" className="error">{error}</p> : !preparation ? <p role="status">Consultando preparación de firma…</p> : <>
      <p>Integridad del PDF almacenado comprobada. {preparation.artifactCurrent ? 'El artefacto coincide con la preparación actual.' : 'Artefacto anterior: vuelva a generar la previsualización.'}</p>
      {preparation.workflow ? <><h3>{preparation.workflow.name} · revisión técnica {preparation.workflow.revisionNumber}</h3>
        <p>Configuración conservada al generar este artefacto; no representa asignaciones ni decisiones ejecutadas.</p>
        <ol>{preparation.workflow.stages.map(stage => <li key={stage.id}><strong>{stage.label}</strong> · {actions[stage.action]} · {stage.requiresSignature ? 'Requiere firma según configuración' : 'Sin firma personal según configuración'}
          {stage.recipientKind === 'COLLEGIATE' && <p>Órgano colegiado: {stage.recipientLabel}</p>}
          <p>{stage.participants.length ? stage.participants.map(p => p.name + (p.active ? '' : ' (inactivo al generar)')).join('; ') : stage.recipientKind === 'COLLEGIATE' ? 'Sin representantes individuales en esta configuración.' : 'Sin participantes resueltos.'}</p>
        </li>)}</ol></> : <p>Este artefacto no contiene una revisión de flujo configurada.</p>}
      <h3>Requisitos pendientes</h3><ul>{preparation.blockers.map((blocker, i) => <li key={i}><strong>{blocker.section}:</strong> {blocker.message}</li>)}</ul>
    </>}
    <button className="secondary" onClick={() => { setPreparation(null); setError(''); setRefresh(value => value + 1) }}>Actualizar preparación de firma</button>
  </section>
}
