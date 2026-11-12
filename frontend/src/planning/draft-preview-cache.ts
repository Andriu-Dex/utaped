import type { PreviewArtifact } from './PlanPreview'

export type PreviewFocus = 'COVER' | 'JUSTIFICATION' | 'OBJECTIVE' | 'MATRIX' | 'ANNEXES' | 'SIGNATURES'
export interface DraftView { sourceRowVersion: number; pages: PreviewArtifact['pages']; sections: Partial<Record<PreviewFocus, number>>; pageIndex: number; image: string }
export const draftViewCache = new Map<string, DraftView>()
export function clearDraftPreviewCache() { draftViewCache.clear() }
