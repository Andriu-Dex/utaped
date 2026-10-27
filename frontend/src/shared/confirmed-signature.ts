import { signPdf } from './api'
import { confirmAction } from './confirm-action'
interface Certificate { holder: string; subject: string; issuer: string; fingerprint: string; validUntil: string }
export type SigningPhase = 'checking' | 'confirming' | 'signing'
export async function confirmAndSign(path: string, file: File, password: string, isActive: () => boolean = () => true, onPhase?: (phase: SigningPhase) => void): Promise<boolean> {
  onPhase?.('checking')
  const certificate = await signPdf<Certificate>('/signing/certificate-preview', file, password)
  if (!isActive()) return false
  onPhase?.('confirming')
  if (!(await confirmAction('Titular del certificado: ' + certificate.holder + '\nEmisor: ' + certificate.issuer + '\nVigente hasta: ' + new Date(certificate.validUntil).toLocaleDateString('es-EC') + '\n¿Confirmar la firma de este documento?'))) return false
  if (!isActive()) return false
  onPhase?.('signing')
  await signPdf(path + (path.includes('?') ? '&' : '?') + 'certificateFingerprint=' + encodeURIComponent(certificate.fingerprint), file, password)
  return true
}
