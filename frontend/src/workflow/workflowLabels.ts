export function displayStageLabel(label: string, action?: string): string {
  if (action === 'REVIEW' && label === 'Revisión de ejemplo') return 'Revisión documental'
  if (action === 'APPROVE' && label === 'Aprobación de ejemplo') return 'Aprobación del documento'
  if (!action) {
    if (label === 'Revisión de ejemplo') return 'Revisión documental'
    if (label === 'Aprobación de ejemplo') return 'Aprobación del documento'
  }
  return label
}

