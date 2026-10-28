import './document-read-only.css'

const messages: Record<string, string> = {
  ARCHIVED: 'El documento está archivado y conserva su contenido e historial.',
  PERIOD_CLOSED: 'El período está cerrado. La documentación se conserva para consulta.',
  NOT_PRINCIPAL: 'La edición corresponde al responsable principal del documento.',
  IN_REVIEW: 'El documento está en revisión. Podrá editarlo cuando sea devuelto para corrección, si el plazo de edición sigue vigente.',
  APPROVED: 'El documento está aprobado. Su contenido firmado se conserva sin modificaciones.',
  SIGNED: 'El contenido ya tiene una firma de elaboración y se conserva sin modificaciones.',
  PREPARATION_NOT_STARTED: 'El plazo de elaboración todavía no ha comenzado.',
  PREPARATION_ENDED: 'El plazo de elaboración ha finalizado, considerando las extensiones registradas.',
  PERIOD_NOT_STARTED: 'El período de elaboración del Informe todavía no ha comenzado.',
  PERIOD_ENDED: 'El período de elaboración del Informe ha finalizado.',
}

export function DocumentReadOnlyNotice({ document }: { document: { editable: boolean; readOnlyReason?: string | null } }) {
  if (document.editable) return null
  return <p className="notice document-read-only-notice" role="status"><strong>Solo lectura.</strong> {messages[document.readOnlyReason ?? ''] ?? 'La edición no está disponible con los permisos y el estado actuales del documento.'}</p>
}
