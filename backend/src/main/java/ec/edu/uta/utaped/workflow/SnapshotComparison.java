package ec.edu.uta.utaped.workflow;

import ec.edu.uta.utaped.documents.ArtifactModels.*;
import java.util.*;
import java.util.stream.Collectors;
import tools.jackson.databind.json.JsonMapper;

/** Compares conserved source snapshots, never editable data or regenerated PDFs. */
public final class SnapshotComparison {
    public record Change(String section,String before,String after) {}
    private static final JsonMapper JSON=JsonMapper.builder().build();
    static List<Change> compare(Snapshot before,Snapshot after) {
        var result=new ArrayList<Change>();
        add(result,"Información general",general(before),general(after),general(before),general(after));
        add(result,"Justificación",before.plan().justification(),after.plan().justification(),before.plan().justification(),after.plan().justification());
        add(result,"Objetivo",before.plan().objective(),after.plan().objective(),before.plan().objective(),after.plan().objective());
        add(result,"Actividades y matriz",Arrays.asList(before.matrixSource(),before.elaboratedBy(),before.activities()),Arrays.asList(after.matrixSource(),after.elaboratedBy(),after.activities()),matrix(before),matrix(after));
        add(result,"Anexos",before.attachments(),after.attachments(),attachments(before),attachments(after));
        add(result,"Elaboradores",before.resolvedAuthors(),after.resolvedAuthors(),authors(before),authors(after));
        add(result,"Flujo de revisión",before.workflow(),after.workflow(),workflow(before),workflow(after));
        add(result,"Nota de protección de datos",before.privacyNoticeEnabled(),after.privacyNoticeEnabled(),before.privacyNoticeEnabled()?"Incluida":"No incluida",after.privacyNoticeEnabled()?"Incluida":"No incluida");
        add(result,"Informe final",before.report(),after.report(),report(before),report(after));
        add(result,"Informe de ejecución",before.execution(),after.execution(),execution(before),execution(after));
        add(result,"Referencias de evidencia",before.externalReferences(),after.externalReferences(),references(before),references(after));
        add(result,"Secciones adicionales",before.structure(),after.structure(),structure(before),structure(after));
        return List.copyOf(result);
    }
    private static void add(List<Change> changes,String section,Object before,Object after,String oldText,String newText) {
        if(!JSON.writeValueAsString(before).equals(JSON.writeValueAsString(after))) changes.add(new Change(section,oldText,newText));
    }
    private static String general(Snapshot s) { var p=s.plan();return p.title()+"\n"+("ADMINISTRATIVE".equals(p.institutionalUnitType())?"Unidad administrativa: ":"Unidad académica: ")+p.institutionalUnit()+"\n"+p.career()+"\nVersión formal: "+p.formalVersion(); }
    private static String matrix(Snapshot s) {
        return "Fuente: "+s.matrixSource()+"\nElaborado por: "+s.elaboratedBy()+"\n\n"+s.activities().stream().map(a->a.title()+"\n"+a.startsOn()+" — "+a.endsOn()+"\nResponsables: "+a.responsibleDisplay()+"\nRecursos: "+String.join("; ",a.resources())+"\nMedios: "+String.join("; ",a.means())).collect(Collectors.joining("\n\n"));
    }
    private static String attachments(Snapshot s) { return s.attachments().stream().map(a->a.title()+" · "+a.originalName()+" · "+a.pageCount()+" páginas\n"+a.description()).collect(Collectors.joining("\n\n")); }
    private static String authors(Snapshot s) { return s.resolvedAuthors().stream().map(a->a.name()+" · "+a.position()).collect(Collectors.joining("\n")); }
    private static String workflow(Snapshot s) { return s.workflow()==null?"Sin flujo":s.workflow().name()+"\n"+s.workflow().stages().stream().map(stage->stage.label()+": "+stage.participants().stream().map(p->p.name()).collect(Collectors.joining(", "))).collect(Collectors.joining("\n")); }
    private static String report(Snapshot s) { var r=s.report();return r==null?"Sin informe final":r.background()+"\n\nConclusiones\n"+r.conclusions()+"\n\nMejoras\n"+r.improvements()+"\n\nResultados\n"+r.results().stream().map(SnapshotComparison::result).collect(Collectors.joining("\n"))+"\n\nDelegaciones\n"+r.delegations().stream().map(d->d.purpose()+" · "+d.location()+" · "+d.institution()+"\n"+d.contact()+" · "+d.contactDetails()+"\n"+d.topic()+"\n"+d.agreement()).collect(Collectors.joining("\n\n")); }
    private static String result(ec.edu.uta.utaped.planning.ReportService.Result r) { return (r.percentage()==null?"Sin porcentaje":r.percentage()+"%")+" · "+r.observations(); }
    private static String execution(Snapshot s) { var r=s.execution();return r==null?"Sin informe de ejecución":"Análisis\n"+r.analysis()+"\n\nFortalezas\n"+r.strengths()+"\n\nDebilidades\n"+r.weaknesses()+"\n\nOportunidades\n"+r.opportunities()+"\n\nAmenazas\n"+r.threats()+"\n\nConclusiones\n"+r.conclusions()+"\n\nRecomendaciones\n"+r.recommendations()+"\n\nAcciones de mejora\n"+r.improvementActions().stream().map(a->a.situation()+" → "+a.action()).collect(Collectors.joining("\n"))+"\n\nResultados\n"+r.results().stream().map(x->(x.percentage()==null?"Sin porcentaje":x.percentage()+"%")+" · "+x.observations()).collect(Collectors.joining("\n"))+"\n\nReferencia de evidencias\n"+r.evidenceLink(); }
    private static String structure(Snapshot s) { return s.structure()==null?"Sin secciones adicionales":s.structure().sections().stream().map(section->section.label()+"\n"+s.structure().contents().getOrDefault(section.key(),section.initialText())).collect(Collectors.joining("\n\n")); }
    private static String references(Snapshot s) { return s.externalReferences()==null?"":s.externalReferences().stream().map(r->r.activity()+" · "+r.medium()+"\n"+r.title()+"\n"+r.url()).collect(Collectors.joining("\n\n")); }
}
