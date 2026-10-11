import type { IconName } from '../shared/Icon'

export const adminPages = [
  { id: 'users', label: 'Usuarios', title: 'Gestión de usuarios', icon: 'users' },
  { id: 'groups', label: 'Grupos y pertenencias', title: 'Grupos y pertenencias', icon: 'users' },
  { id: 'periods', label: 'Períodos académicos', title: 'Períodos académicos', icon: 'calendar' },
  { id: 'activities', label: 'Catálogo de actividades', title: 'Catálogo de actividades', icon: 'document' },
  { id: 'resources', label: 'Recursos y medios', title: 'Recursos y medios', icon: 'document' },
  { id: 'holidays', label: 'Feriados y restricciones', title: 'Feriados y restricciones', icon: 'calendar' },
  { id: 'workflows', label: 'Flujos de aprobación', title: 'Flujos de aprobación', icon: 'settings' },
  { id: 'audit', label: 'Auditoría', title: 'Auditoría administrativa', icon: 'eye' },
  { id: 'certificates', label: 'Certificados', title: 'Vinculación de certificados', icon: 'lock' },
] as const satisfies readonly { id: string; label: string; title: string; icon: IconName }[]
export type AdminPage = typeof adminPages[number]['id']
export function isAdminPage(value: string): value is AdminPage { return adminPages.some(page => page.id === value) }
