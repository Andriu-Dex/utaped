type Request = { message: string; resolve: (accepted: boolean) => void; origin: HTMLElement | null }
let pending: Request | null = null
const listeners = new Set<() => void>()
export function subscribeConfirmation(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener) } }
export function currentConfirmation() { return pending }
export function confirmAction(message: string): Promise<boolean> {
  // Ignore repeated attempts while a decision is already pending; never queue duplicate mutations.
  if (pending) return Promise.resolve(false)
  return new Promise(resolve => {
    pending = { message, resolve, origin: document.activeElement instanceof HTMLElement ? document.activeElement : null }
    listeners.forEach(listener => listener())
  })
}
export function finishConfirmation(accepted: boolean) {
  const request = pending
  if (!request) return
  pending = null; listeners.forEach(listener => listener())
  request.resolve(accepted)
  requestAnimationFrame(() => { if (request.origin?.isConnected) request.origin.focus() })
}
