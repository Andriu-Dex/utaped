export interface WorkPlan {
  id: string; teacherId: string; teacherName: string; groupId: string; groupName: string; periodId: string; periodName: string
  title: string; institutionalUnit: string; career: string; justification: string; objective: string
  documentState: 'DRAFT' | 'IN_REVIEW' | 'APPROVED'; formalVersion: string; rowVersion: number; preparationDate: string
  scopeId: string | null; createdBy: string; reviewRound: number; archivedAt: string | null
  documentType: 'T1' | 'T2'; basePlanId: string | null
  createdAt: string; updatedAt: string; editable: boolean; canManage: boolean
}
export interface PlanOptions {
  groups: { id: string; name: string }[]
  periods: { id: string; name: string; preparationStartsOn: string; preparationEndsOn: string; editable: boolean; status: 'ACTIVE' | 'CLOSED' }[]
  singlePlanPerScope: boolean; enforcePreparationWindow: boolean
  scopes: { id: string; groupId: string; periodId: string; groupName: string; periodName: string; careerName: string | null; canCreate: boolean }[]
}
export interface PlanPage { items: Pick<WorkPlan, 'id' | 'title' | 'groupName' | 'periodName' | 'documentState' | 'formalVersion' | 'updatedAt' | 'editable' | 'documentType' | 'archivedAt'>[]; total: number; page: number; size: number }
