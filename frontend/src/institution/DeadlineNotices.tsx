import { useEffect, useState } from 'react'
import { api } from '../shared/api'
import { Toast } from '../shared/Toast'
import { Icon } from '../shared/Icon'

export function DeadlineNotices() {
  const [config, setConfig] = useState<{ enabled: boolean; nearDays: number; repeatDays: number; timeZone: string } | null>(null), [busy, setBusy] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState('')
  const [reload, setReload] = useState(0)
  useEffect(() => { let active = true; api<typeof config>('/admin/deadline-notices').then(c => { if (active) { setConfig(c); setError('') } }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [reload])
  async function scan() { setBusy(true); setError(''); setNotice(''); try { const data = await api<{ sent: number }>('/admin/deadline-notices/scan', 'POST'); setNotice(data.sent + ' avisos nuevos. Los envíos anteriores no se duplican.'); window.dispatchEvent(new Event('notifications-changed')) } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo comprobar.') } finally { setBusy(false) } }
  return <section className="card deadline-notices-card" style={{ marginTop: 24 }}>
    <div className="deadline-notices-heading">
      <div className="section-heading" style={{ marginBottom: 0 }}>
        <span className="section-icon amber"><Icon name="calendar" /></span>
        <div>
          <span className="section-kicker">AVISOS INTERNOS</span>
          <h2>Vencimientos y evidencias pendientes</h2>
          {config && <p className="muted" style={{ margin: '4px 0 0', fontSize: '0.88rem' }}>Comprobación automática {config.enabled ? 'habilitada' : 'deshabilitada'} · Aviso próximo a vencer: {config.nearDays} días antes · Reiteración: cada {config.repeatDays} día(s) en {config.timeZone}.</p>}
        </div>
      </div>
      {config && <button type="button" className="secondary" disabled={busy} onClick={scan} style={{ whiteSpace: 'nowrap' }}><Icon name="refresh" size={16} />Comprobar vencimientos ahora</button>}
    </div>
    {error && <><Toast message={error} onDismiss={() => setError('')} /><button type="button" className="secondary" disabled={busy} onClick={() => setReload(value => value + 1)}>Reintentar configuración de avisos</button></>}
    {notice && <Toast tone="success" message={notice} onDismiss={() => setNotice('')} />}
    <p className="field-help" style={{ margin: '12px 0 0' }}>Solo Planes aprobados en períodos vigentes, responsables activos con acceso y medios sin PDF. Se respeta cada prórroga personal. La frecuencia es configuración técnica ajustable.</p>
  </section>
}
