import { Icon } from '../shared/Icon'
import './work-assistance.css'

export interface ChecklistBlocker { section: string; message: string }
type Section = 'general' | 'content' | 'matrix' | 'attachments'
const destinations: Record<string, Section> = { 'Información general': 'general', Contenido: 'content', 'Actividades y matriz': 'matrix', Anexos: 'attachments' }
export function SignatureChecklist({ blockers, onResolve, onResolveRequirement, disabled = false, signing = false, documentType = 'T1' }: { blockers: ChecklistBlocker[]; onResolve?: (section: Section) => void; onResolveRequirement?: (section: string) => void; disabled?: boolean; signing?: boolean; documentType?: 'T1' | 'T2' | 'T3' }) {
  const content = documentType === 'T1' ? ['Información general', 'Contenido', 'Actividades y matriz', 'Anexos'] : documentType === 'T2' ? ['Antecedentes', 'Resultados', 'Conclusiones', 'Oportunidades de mejora', 'Evidencias', 'Anexos'] : ['Resultados', 'Análisis de resultados', 'Análisis FODA', 'Conclusiones', 'Recomendaciones', 'Anexos']
  const extra = blockers.filter(blocker => !content.includes(blocker.section))
  const sections = [...content, ...new Set(extra.map(blocker => blocker.section))]
  if (signing && extra.length === 0) sections.push('Flujo y firma')
  const groups = sections.map(section => ({ section, items: blockers.filter(blocker => blocker.section === section) }))
  return <section className="card signature-checklist" aria-label="Comprobación antes de firmar">
    <header className="assistance-heading"><div className="section-heading"><span className="section-icon"><Icon name="check" /></span><div><span className="section-kicker">ANTES DE FIRMAR</span><h2>Comprobación del documento</h2></div></div><span className={'status-badge ' + (blockers.length ? 'checklist-pending' : 'active')}>{blockers.length ? blockers.length + (blockers.length === 1 ? ' pendiente' : ' pendientes') : 'Comprobaciones completas'}</span></header>
    <ul>{groups.map(group => <li key={group.section} data-pending={group.items.length > 0}><Icon name={group.items.length ? 'document' : 'check'} size={18} /><div><strong>{group.section}</strong>{group.items.length ? group.items.map((blocker, index) => <p key={index}>{blocker.message}</p>) : <p>Sin requisitos pendientes.</p>}</div>{group.items.length > 0 && ((onResolve && destinations[group.section]) || (onResolveRequirement && content.includes(group.section))) && <button type="button" className="link" disabled={disabled} onClick={() => { if (onResolveRequirement) onResolveRequirement(group.section); else onResolve?.(destinations[group.section]) }}>Revisar<span className="sr-only"> {group.section.toLowerCase()}</span><Icon name="arrow" size={16} /></button>}</li>)}</ul>
    {signing && <p className="field-help">El certificado se comprueba al firmar. Estas comprobaciones no sustituyen la revisión del PDF.</p>}
  </section>
}
