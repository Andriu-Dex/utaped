import { useEffect, useRef, useState, type InputHTMLAttributes, type Ref } from 'react'
import { Icon } from './Icon'
import './file-input.css'

type FileInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> & { ref?: Ref<HTMLInputElement> }

/**
 * Styled file picker. The native input stays in the form (name, required, accept, labels and tests keep working)
 * but is visually hidden, because its built-in text follows the browser language ("Choose File").
 */
export function FileInput({ ref, className, ...props }: FileInputProps) {
  const input = useRef<HTMLInputElement | null>(null)
  const [names, setNames] = useState<string[]>([])
  useEffect(() => {
    const element = input.current
    if (!element) return
    const sync = () => setNames(Array.from(element.files ?? [], file => file.name))
    // Form resets and programmatic clears followed by a change event must also clear the shown name.
    const reset = () => setNames([])
    element.addEventListener('change', sync)
    element.form?.addEventListener('reset', reset)
    return () => { element.removeEventListener('change', sync); element.form?.removeEventListener('reset', reset) }
  }, [])
  function assign(element: HTMLInputElement | null) {
    input.current = element
    if (typeof ref === 'function') ref(element)
    else if (ref) ref.current = element
  }
  return <span className={'file-input' + (props.disabled ? ' is-disabled' : '') + (className ? ' ' + className : '')}>
    <input {...props} ref={assign} type="file" className="file-input-native" />
    {/* Visible texts come from CSS content so they never become part of the surrounding label's text. */}
    <span className="file-input-button" aria-hidden="true"><Icon name="document" size={16} /></span>
    <span className={'file-input-name' + (names.length ? '' : ' is-empty')} aria-hidden="true" data-label={names.length ? names.join(', ') : 'Ningún archivo seleccionado'} />
  </span>
}
