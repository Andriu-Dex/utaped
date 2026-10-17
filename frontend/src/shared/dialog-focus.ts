/** Native capture also covers controls rendered into a dialog through React portals. */
export function containDialogFocus(dialog: HTMLDialogElement) {
  const trap = (event: KeyboardEvent) => {
    if (event.key !== 'Tab') return
    const controls = [...dialog.querySelectorAll<HTMLElement>('button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href],[tabindex="0"]')].filter(el => el.getClientRects().length > 0)
    const first = controls[0], last = controls.at(-1)
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
  }
  dialog.addEventListener('keydown', trap, true)
  return () => dialog.removeEventListener('keydown', trap, true)
}
