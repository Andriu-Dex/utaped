import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'
import { Icon } from '../shared/Icon'

export function DeadlineNotices() {
  const [config, setConfig] = useState<{ enabled: boolean; nearDays: number; repeatDays: number; timeZone: string } | null>(null), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  useEffect(() => { let active = true; api<typeof config>('/admin/deadline-notices').then(c => { if (active) setConfig(c) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [])
  async function scan() { setBusy(true); setError(''); setNotice(''); try { const data = await api<{ sent: number }>('/admin/deadline-notices/scan', 'POST'); setNotice(data.sent + ' avisos nuevos. Los envíos anteriores no se duplican.'); window.dispatchEvent(new Event('notifications-changed')) } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo comprobar.') } finally { setBusy(false) } }
  return <section className="card"><div className="section-heading"><span className="section-icon amber"><Icon name="calendar" /></span><div><span className="section-kicker">AVISOS INTERNOS</span><h2>Vencimientos y evidencias pendientes</h2></div></div>{error && <Toast message={error} onDismiss={() => setError('')} />}{notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}{config && <><p className="muted">Comprobación automática {config.enabled ? 'habilitada' : 'deshabilitada'}. Aviso próximo a vencer: {config.nearDays} días antes. Reiteración de pendientes vencidos: cada {config.repeatDays} día(s), en {config.timeZone}.</p><p className="field-help">Solo Planes aprobados en períodos vigentes, responsables activos con acceso y medios sin PDF. Se respeta cada prórroga personal. La frecuencia es configuración técnica ajustable.</p><button type="button" disabled={busy} onClick={scan}><Icon name="refresh" size={16} />Comprobar vencimientos ahora</button></>}</section>
}
