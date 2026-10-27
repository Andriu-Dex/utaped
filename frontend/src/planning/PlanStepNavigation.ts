import { createContext, useContext, useEffect, useLayoutEffect, useRef, useCallback } from 'react'

export type PlanStep = 'general' | 'content' | 'matrix' | 'attachments' | 'preparation' | 'signing'
export type PlanStepGuard = (target: PlanStep) => Promise<boolean>
export const planSteps: { key: PlanStep; label: string }[] = [
  { key: 'general', label: 'Información general' }, { key: 'content', label: 'Contenido' },
  { key: 'matrix', label: 'Actividades y matriz' }, { key: 'attachments', label: 'Anexos y opciones' },
  { key: 'preparation', label: 'Revisión final' }, { key: 'signing', label: 'Firma y Finalización' },
]
export const PlanStepNavigationContext = createContext<null | ((guard: PlanStepGuard, busy: boolean) => () => void)>(null)
export function usePlanStepGuard(guard: PlanStepGuard, busy: boolean) {
  const register = useContext(PlanStepNavigationContext)
  const latest = useRef(guard)
  useLayoutEffect(() => { latest.current = guard }, [guard])
  const stableGuard = useCallback((target: PlanStep) => latest.current(target), [])
  useEffect(() => register?.(stableGuard, busy), [register, stableGuard, busy])
  return register !== null
}
