export interface WorkPlan {
  id: string; teacherId: string; teacherName: string; groupId: string; groupName: string; periodId: string; periodName: string
  title: string; institutionalUnit: string; career: string; justification: string; objective: string
  documentState: 'DRAFT'; formalVersion: string; rowVersion: number; preparationDate: string
  createdAt: string; updatedAt: string; editable: boolean
}
export interface PlanOptions {
  groups: { id: string; name: string }[]
  periods: { id: string; name: string; preparationStartsOn: string; preparationEndsOn: string; editable: boolean }[]
  singlePlanPerScope: boolean; enforcePreparationWindow: boolean
}
export interface PlanPage { items: Pick<WorkPlan, 'id' | 'title' | 'groupName' | 'periodName' | 'documentState' | 'formalVersion' | 'updatedAt' | 'editable'>[]; total: number; page: number; size: number }
