export interface ReportingRow {
  groupId: string; groupName: string; careerName: string; periodName: string
  plans: number; reports: number; approved: number; inReview: number; drafts: number
  activities: number; completedActivities: number; evidenceCoverage: number
  declaredExecution: number | null; declaredActivities: number
  pendingEvidence: number; dueSoonActivities: number; overdueActivities: number
}
export interface ReportingSummary {
  rows: ReportingRow[]; plans: number; reports: number; approved: number; inReview: number; drafts: number
  activities: number; completedActivities: number; pendingEvidence: number; dueSoonActivities: number; overdueActivities: number
  declaredExecution: number | null; declaredActivities: number; declarationCoverage: number; dueSoonDays: number
  upcoming: { documentId: string; activityId: string; documentTitle: string; activityTitle: string; groupName: string; periodName: string; deadline: string; missingEvidence: number }[]
}
export const formatPercent = (value: number) => new Intl.NumberFormat('es-EC', { maximumFractionDigits: 1 }).format(value) + ' %'
