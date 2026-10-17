import { useEffect } from 'react'

/** Keep the existing toast portal visible and interactive above the active modal. */
export function ToastHost() {
  useEffect(() => {
    const host = document.getElementById('toast-root')
    if (!host) return
    const place = () => {
      const target = [...document.querySelectorAll<HTMLDialogElement>('dialog[open]')].at(-1) ?? document.body
      if (host.parentElement !== target) target.appendChild(host)
    }
    place()
    const observer = new MutationObserver(place)
    observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['open'] })
    return () => { observer.disconnect(); document.body.appendChild(host) }
  }, [])
  return null
}
