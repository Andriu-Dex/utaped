export type Category = 'POA' | 'IMPROVEMENT_PLAN' | 'IMPROVEMENT_ACTION' | 'OTHER'
export const categoryLabels: Record<Category, string> = { POA: 'POA', IMPROVEMENT_PLAN: 'Plan de Mejoras', IMPROVEMENT_ACTION: 'Acción de Mejora', OTHER: 'Otra' }
export interface Catalog { id: string; kind: 'RESOURCE' | 'MEANS'; label: string; active: boolean }
export interface Definition { id: string; groupId: string; title: string; category: Category; mandatory: boolean; active: boolean }
export interface Choice { catalogId: string | null; other: string | null; label: string | null }
export interface Activity {
  id: string; catalogId: string | null; title: string; category: Category; mandatory: boolean
  startsOn: string | null; endsOn: string | null; responsibleIds: string[]; collective: boolean; resources: Choice[]; means: Choice[]
}
export interface Matrix {
  rowVersion: number; source: string; elaboratedBy: string; collectiveLabel: string; editable: boolean
  periodStartsOn: string; periodEndsOn: string; restrictHolidayEndpoints: boolean
  activities: Activity[]; definitions: Definition[]; catalogs: Catalog[]; members: { id: string; name: string }[]; holidays: { date: string; label: string }[]
}
