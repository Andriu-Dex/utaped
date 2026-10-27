import type { KeyboardEvent } from 'react'

/** Local to the focused viewer: never install a document-wide keyboard listener. */
export function navigatePdfWithArrow(event: KeyboardEvent<HTMLElement>, page: number, pageCount: number, disabled: boolean, go: (page: number) => void) {
  if (disabled || event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey || !['ArrowLeft', 'ArrowRight'].includes(event.key)) return
  if (!(event.target instanceof HTMLElement) || event.target.closest('input, textarea, select, button, a, [contenteditable]:not([contenteditable="false"]), [role="textbox"], [role="combobox"], [role="slider"], [role="spinbutton"], dialog, [aria-modal="true"]')) return
  event.preventDefault()
  const target = page + (event.key === 'ArrowRight' ? 1 : -1)
  if (target >= 0 && target < pageCount) go(target)
}
