import type { IconName } from '../shared/Icon'

export type AdminPage =
  | 'users'
  | 'groups'
  | 'organization'
  | 'positions'
  | 'periods'
  | 'templates'
  | 'activities'
  | 'resources'
  | 'holidays'
  | 'workflows'
  | 'reporting'
  | 'audit'
  | 'certificates'

export interface AdminNavItem {
  readonly id: AdminPage
  readonly label: string
  readonly title: string
  readonly icon: IconName
}

export interface AdminNavGroup {
  readonly title: string
  readonly items: readonly AdminNavItem[]
}

export const adminNavGroups: readonly AdminNavGroup[] = [
  {
    title: 'Organización y acceso',
    items: [
      { id: 'users', label: 'Usuarios', title: 'Gestión de usuarios', icon: 'users' },
      { id: 'groups', label: 'Grupos y pertenencias', title: 'Grupos y pertenencias', icon: 'users' },
      { id: 'organization', label: 'Carreras y ámbitos', title: 'Organización institucional', icon: 'users' },
      { id: 'positions', label: 'Cargos institucionales', title: 'Cargos institucionales', icon: 'users' },
    ],
  },
  {
    title: 'Planificación y reglas',
    items: [
      { id: 'periods', label: 'Períodos académicos', title: 'Períodos académicos', icon: 'calendar' },
      { id: 'templates', label: 'Plantillas', title: 'Plantillas institucionales', icon: 'document' },
      { id: 'activities', label: 'Catálogo de actividades', title: 'Catálogo de actividades', icon: 'document' },
      { id: 'resources', label: 'Recursos y medios', title: 'Recursos y medios', icon: 'document' },
      { id: 'holidays', label: 'Feriados y restricciones', title: 'Feriados y restricciones', icon: 'calendar' },
      { id: 'workflows', label: 'Flujos de aprobación', title: 'Flujos de aprobación', icon: 'settings' },
    ],
  },
  {
    title: 'Control y auditoría',
    items: [
      { id: 'reporting', label: 'Reportes de gestión', title: 'Reportes de gestión', icon: 'document' },
      { id: 'audit', label: 'Auditoría', title: 'Auditoría administrativa', icon: 'eye' },
      { id: 'certificates', label: 'Certificados', title: 'Historial de certificados', icon: 'lock' },
    ],
  },
]

export const adminPages: readonly AdminNavItem[] = adminNavGroups.flatMap(group => group.items)

export function isAdminPage(value: string): value is AdminPage {
  return adminPages.some(page => page.id === value)
}
