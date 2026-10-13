import { useEffect, useRef, useState, type RefObject } from 'react'
import { Icon } from '../shared/Icon'
import { Toast } from '../shared/Toast'

type Challenge = { id: string; image: string }
export function CaptchaField({ nonce, busy, answer, onReady, onRenew }: {
  nonce: string; busy: boolean; answer: RefObject<HTMLInputElement | null>
  onReady: (ready: boolean) => void; onRenew: () => void
}) {
  const [challenge, setChallenge] = useState<Challenge | null>(null)
  const [error, setError] = useState('')
  const [imageFailed, setImageFailed] = useState(false)
  const [audioShown, setAudioShown] = useState(false)
  const audio = useRef<HTMLAudioElement>(null)
  useEffect(() => {
    const controller = new AbortController(); let image = ''
    // Deferring one microtask prevents the discarded StrictMode effect issuing a challenge.
    void Promise.resolve().then(async () => {
      if (controller.signal.aborted) return
      try {
        const response = await fetch('/api/auth/captcha?v=' + nonce, { signal: controller.signal, cache: 'no-store', credentials: 'same-origin' })
        const id = response.headers.get('X-Captcha-Id')
        if (!response.ok || !id || !/^[a-f0-9-]{36}$/i.test(id)) throw new Error()
        const blob = await response.blob()
        if (controller.signal.aborted) return
        image = URL.createObjectURL(blob); setChallenge({ id, image }); onReady(true)
      } catch { if (!controller.signal.aborted) setError('No se pudo cargar el código. Cambia el código de verificación para reintentar.') }
    })
    return () => { controller.abort(); if (image) URL.revokeObjectURL(image) }
  }, [nonce, onReady])
  function showAudio() { setAudioShown(true); setError(''); requestAnimationFrame(() => audio.current?.focus()) }
  return <div className="captcha-field">
    <div className="captcha-heading"><label htmlFor="captcha-answer">Código de verificación</label>
      <button type="button" className="icon-button" title="Cambiar código de verificación" aria-label="Cambiar código de verificación" disabled={busy || (!challenge && !error)} onClick={onRenew}><Icon name="refresh" /></button>
    </div>
    {challenge && !imageFailed && <img className="captcha-image" src={challenge.image} alt="Código de seis caracteres. También puedes escucharlo con Escuchar código." onError={() => setImageFailed(true)} />}
    {challenge && <div className="captcha-audio-panel">
      <button type="button" className="captcha-listen" aria-expanded={audioShown} aria-controls="captcha-audio" onClick={showAudio} disabled={busy}><Icon name="volume" size={18} />Escuchar código</button>
      {audioShown && <audio id="captcha-audio" ref={audio} tabIndex={0} controls preload="none" aria-label="Audio del código de verificación" aria-describedby="captcha-help" src={'/api/auth/captcha/audio?id=' + challenge.id}
        onError={() => setError('No se pudo reproducir el audio o el código expiró. Cambia el código de verificación para reintentar.')} />}
    </div>}
    {!challenge && !error && <p className="field-help" role="status">Cargando código…</p>}
    {imageFailed && <p className="field-help">La imagen no está disponible. Puedes escuchar el código.</p>}
    {error && <Toast message={error} />}
    <input ref={answer} id="captcha-answer" name="captcha" required minLength={6} maxLength={6} aria-describedby="captcha-help" autoComplete="off" autoCapitalize="characters" spellCheck={false} placeholder="Escribe el código" />
    <small id="captcha-help" className="field-help">Seis letras o números, sin espacios. No distingue mayúsculas. Válido por 3 minutos.{audioShown && ' Pulsa reproducir para escucharlo; puedes pausarlo y repetirlo.'}</small>
  </div>
}
