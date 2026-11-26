package ec.edu.uta.utaped.documents;

import static ec.edu.uta.utaped.documents.T2Template.*;
import ec.edu.uta.utaped.documents.ArtifactModels.Snapshot;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

/** Versioned binding to the two owner-confirmed official event DOCX packages. */
@Component
public class EventTemplate {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private final byte[] planning,execution;
    public EventTemplate() throws IOException {
        try(var in=new ClassPathResource("document-templates/event-plan-reference.docx").getInputStream()) { planning=in.readAllBytes(); }
        try(var in=new ClassPathResource("document-templates/event-report-reference.docx").getInputStream()) { execution=in.readAllBytes(); }
    }
    public String version(String type) { return "EVENT-"+type+"-"+StoredFiles.hash(type.equals("T1")?planning:execution).substring(0,16)+"-A4-engine2"; }
    public List<String> sections(Snapshot s) {
        var result=new ArrayList<String>();if(s.plan().documentType().equals("T1"))result.addAll(List.of("JUSTIFICACIÓN","OBJETIVO"));
        result.addAll(List.of("DATOS INFORMATIVOS DEL EVENTO","DESARROLLO DEL EVENTO"));
        if(s.plan().documentType().equals("T2"))result.add("CONCLUSIONES Y/O RECOMENDACIONES");
        result.addAll(List.of("ANEXOS","FIRMAS DE RESPONSABILIDAD","CONTROL DE HISTORIAL DE CAMBIOS","ÍNDICE DE CONTENIDO","ÍNDICE DE TABLAS"));return result;
    }
    byte[] compose(Snapshot s,Map<String,Integer> numbers,CompositionMarkers markers) {
        try(var in=new ZipInputStream(new ByteArrayInputStream(s.plan().documentType().equals("T1")?planning:execution));var buffer=new ByteArrayOutputStream();var out=new ZipOutputStream(buffer)) {
            for(ZipEntry entry;(entry=in.getNextEntry())!=null;) {
                byte[] bytes=in.readAllBytes();
                if(entry.getName().equals("word/document.xml")||entry.getName().matches("word/(header|footer)\\d+\\.xml")) {
                    var f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
                    var doc=f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
                    if(entry.getName().equals("word/document.xml"))body(doc,s,numbers,markers);
                    else if(entry.getName().startsWith("word/header"))InstitutionalHeader.apply(doc,s);
                    else for(var p:all(doc.getDocumentElement(),"p"))if(all(p,"instrText").stream().anyMatch(t->t.getTextContent().contains("PAGE"))) {text(p,markers.page());for(var sz:all(p,"sz"))sz.setAttributeNS(W,"w:val","4");}
                    var tf=TransformerFactory.newDefaultInstance();tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");var xml=new ByteArrayOutputStream();tf.newTransformer().transform(new DOMSource(doc),new StreamResult(xml));bytes=xml.toByteArray();
                }
                out.putNextEntry(new ZipEntry(entry.getName()));out.write(bytes);out.closeEntry();
            }
            out.finish();return buffer.toByteArray();
        }catch(Exception e) { throw new IllegalStateException("No se pudo componer el formato institucional de evento.",e); }
    }
    private String plain(Element e) { return String.join("",all(e,"t").stream().map(Element::getTextContent).toList()).strip(); }
    private Element find(List<Element> source,String label) { return source.stream().filter(e->e.getLocalName().equals("p")&&plain(e).equals(label)).findFirst().orElseThrow(()->new IllegalStateException("Vinculación de plantilla no disponible: "+label)); }
    private void body(Document doc,Snapshot s,Map<String,Integer> numbers,CompositionMarkers markers) {
        boolean plan=s.plan().documentType().equals("T1");var data=s.eventData();var body=(Element)doc.getElementsByTagNameNS(W,"body").item(0);var source=children(body,null);var tables=source.stream().filter(n->n.getLocalName().equals("tbl")).toList();
        var out=new ArrayList<Element>();var headings=new LinkedHashMap<String,Element>();
        var heading=find(source,"DATOS INFORMATIVOS DEL EVENTO");var paragraph=plan?source.get(41):source.get(45);
        int index=source.indexOf(find(source,"ÍNDICE DE CONTENIDO"));
        for(int i=0;i<index;i++) {
            var n=(Element)source.get(i).cloneNode(true);String value=plain(n);
            if(value.startsWith("UNIDAD ACADÉMICA"))text(n,("ADMINISTRATIVE".equals(s.plan().institutionalUnitType())?"UNIDAD ADMINISTRATIVA: ":"UNIDAD ACADÉMICA: ")+s.plan().institutionalUnit());
            else if(value.startsWith("CARRERA:")) { if(s.plan().career().isBlank())continue;text(n,"CARRERA: "+s.plan().career()); }
            else if(value.startsWith("PLAN DE TRABAJO DE:"))text(n,"PLAN DE TRABAJO DE: "+data.topic());
            else if(value.startsWith("NOMBRE DEL EVENTO:"))text(n,"NOMBRE DEL EVENTO: "+data.topic());
            else if(value.startsWith("PERÍODO:")) { text(n,"PERÍODO: "+s.plan().periodName());out.add(n);break; }
            for(var br:all(n,"br"))if("page".equals(br.getAttributeNS(W,"type")))br.getParentNode().removeChild(br);
            out.add(n);
        }
        heading(out,headings,find(source,"ÍNDICE DE CONTENIDO"),"ÍNDICE DE CONTENIDO");property(out.getLast(),"pageBreakBefore");
        for(String label:sections(s))if(!label.startsWith("ÍNDICE"))out.add(InstitutionalIndex.entry(paragraph,label,label,numbers));
        heading(out,headings,find(source,"ÍNDICE DE TABLAS"),"ÍNDICE DE TABLAS");
        out.add(InstitutionalIndex.entry(paragraph,"Tabla 1.- Datos informativos del evento","DATOS INFORMATIVOS DEL EVENTO",numbers));
        out.add(InstitutionalIndex.entry(paragraph,"Tabla 2.- Matriz del evento "+(plan?"planificado":"ejecutado"),"DESARROLLO DEL EVENTO",numbers));
        if(plan) {
            heading(out,headings,find(source,"JUSTIFICACIÓN"),"JUSTIFICACIÓN");property(out.getLast(),"pageBreakBefore");lines(out,paragraph,s.plan().justification());
            heading(out,headings,find(source,"OBJETIVO"),"OBJETIVO");out.add(paragraph(paragraph,"OBJETIVO GENERAL"));lines(out,paragraph,data.generalObjective());out.add(paragraph(paragraph,"OBJETIVOS ESPECÍFICOS"));lines(out,paragraph,data.specificObjectives());
        }
        heading(out,headings,heading,"DATOS INFORMATIVOS DEL EVENTO");if(!plan)property(out.getLast(),"pageBreakBefore");
        out.add(paragraph(paragraph,"Tabla 1.- Datos informativos del evento"));
        var info=(Element)tables.getFirst().cloneNode(true);var infoRows=children(info,"tr");
        var values=new ArrayList<String>(List.of(data.topic(),eventType(data.eventType(),data.otherType()),date(data.startsOn())+" — "+date(data.endsOn()),time(data.startsAt()),time(data.endsAt()),modality(data.modality(),data.otherModality()),data.location()));
        if(plan)values.add(number(data.capacity()));
        values.add("Autoridades: "+number(data.authorities())+"\nDocentes: "+number(data.teachers())+"\nEstudiantes: "+number(data.students())+"\nOtros: "+number(data.otherAttendees()));values.addAll(List.of(data.logistics(),data.certificates(),data.diffusion()));
        if(infoRows.size()!=values.size())throw new IllegalStateException("La tabla oficial de datos del evento cambió.");
        for(int i=0;i<values.size();i++)cell(children(infoRows.get(i),"tc").getLast(),values.get(i));out.add(info);notes(out,paragraph,s);
        var matrixStart=paragraph(paragraph,"");out.add(matrixStart);
        heading(out,headings,heading,"DESARROLLO DEL EVENTO");var matrixCaption=paragraph(paragraph,"Tabla 2.- Matriz del evento "+(plan?"planificado":"ejecutado"));property(matrixCaption,"keepNext");out.add(matrixCaption);
        var matrix=(Element)tables.get(1).cloneNode(true);var rows=children(matrix,"tr");var row=(Element)rows.get(1).cloneNode(true);for(int i=1;i<rows.size();i++)matrix.removeChild(rows.get(i));
        for(var a:data.actions()) {
            var r=(Element)row.cloneNode(true);var cells=children(r,"tc");var texts=new ArrayList<>(List.of(a.title(),date(a.date()),time(a.startsAt()),time(a.endsAt()),a.responsibleDisplay()==null?"":a.responsibleDisplay(),a.resources()));
            if(!plan)texts.addAll(List.of(a.percentage()==null?"":a.percentage().stripTrailingZeros().toPlainString()+" %",a.observations()));
            for(int i=0;i<texts.size();i++)cell(cells.get(i),texts.get(i));removeAll(r,"trHeight");matrix.appendChild(r);
        }
        if(data.actions().isEmpty())matrix.appendChild(row);out.add(matrix);notes(out,paragraph,s);
        var matrixEnd=paragraph(paragraph,"");out.add(matrixEnd);
        if(!plan) {heading(out,headings,heading,"CONCLUSIONES Y/O RECOMENDACIONES");lines(out,paragraph,data.conclusions());}
        heading(out,headings,find(source,"ANEXOS"),"ANEXOS");if(s.attachments().isEmpty())out.add(paragraph(paragraph,"(No aplica)"));else for(var a:s.attachments())out.add(paragraph(paragraph,a.label()+". "+a.title()));
        heading(out,headings,find(source,"FIRMAS DE RESPONSABILIDAD"),"FIRMAS DE RESPONSABILIDAD");property(out.getLast(),"pageBreakBefore");
        var signatures=(Element)tables.get(2).cloneNode(true);var signatureRows=children(signatures,"tr");var prototype=(Element)signatureRows.get(1).cloneNode(true);removeAll(prototype,"vMerge");
        for(int i=1;i<signatureRows.size();i++)signatures.removeChild(signatureRows.get(i));
        for(int i=0;i<s.resolvedAuthors().size();i++){var a=s.resolvedAuthors().get(i);signature(signatures,prototype,"Elaborado por:",a.name(),a.position(),i==0?"":"A"+i,markers);}
        int slot=1;if(s.workflow()!=null)for(var stage:s.workflow().stages()) {
            if(stage.recipientKind().equals("COLLEGIATE")&&!stage.requiresSignature())signature(signatures,prototype,stage.label()+":",stage.recipientLabel(),"",null,markers);
            else for(var a:stage.participants())signature(signatures,prototype,stage.label()+":",a.name(),a.position()==null?"":a.position(),stage.requiresSignature()?String.valueOf(slot++):null,markers);
        }
        out.add(signatures);heading(out,headings,find(source,"CONTROL DE HISTORIAL DE CAMBIOS"),"CONTROL DE HISTORIAL DE CAMBIOS");
        var history=(Element)tables.get(3).cloneNode(true);var historyRows=children(history,"tr");for(int i=1;i<historyRows.size();i++)history.removeChild(historyRows.get(i));
        var historyRow=(Element)historyRows.getFirst().cloneNode(true);var hc=children(historyRow,"tc");cell(hc.get(0),s.plan().formalVersion());cell(hc.get(1),(plan?"Elaboración del plan de trabajo de evento: ":"Elaboración del informe de ejecución de evento: ")+data.topic());cell(hc.get(2),date(s.plan().preparationDate()));removeAll(historyRow,"shd");history.appendChild(historyRow);out.add(history);
        var section=(Element)source.getLast().cloneNode(true);
        var originalSection=source.stream().filter(n->!all(n,"sectPr").isEmpty()).findFirst().map(n->all(n,"sectPr").getFirst()).orElseThrow();
        for(String tag:List.of("headerReference","footerReference")) { removeAll(section,tag);for(var ref:children(originalSection,tag))section.insertBefore(ref.cloneNode(true),section.getFirstChild()); }
        for(var n:out)removeAll(n,"sectPr");
        // The official DOCX isolates the wide development matrix in a landscape section.
        sectionBreak(matrixStart,section,false);sectionBreak(matrixEnd,section,true);
        out.add(section);for(var n:children(body,null))body.removeChild(n);out.forEach(body::appendChild);removeAll(body,"bookmarkStart");removeAll(body,"bookmarkEnd");
        int bookmark=3000,ordinal=1;for(var item:headings.entrySet()) {
            var h=item.getValue();removeAll(h,"numPr");var noNumber=element(doc,"numId");noNumber.setAttributeNS(W,"w:val","0");property(h,"numPr").appendChild(noNumber);if(!item.getKey().startsWith("ÍNDICE")&&!Set.of("FIRMAS DE RESPONSABILIDAD","CONTROL DE HISTORIAL DE CAMBIOS").contains(item.getKey()))text(h,(ordinal++)+". "+item.getKey());var start=element(doc,"bookmarkStart");start.setAttributeNS(W,"w:id",String.valueOf(bookmark));start.setAttributeNS(W,"w:name",T1Template.anchor(item.getKey()));h.insertBefore(start,h.getFirstChild());var end=element(doc,"bookmarkEnd");end.setAttributeNS(W,"w:id",String.valueOf(bookmark++));h.appendChild(end);
        }
        for(var page:all(body,"pgSz")){boolean wide="landscape".equals(page.getAttributeNS(W,"orient"));page.setAttributeNS(W,"w:w",wide?"16838":"11906");page.setAttributeNS(W,"w:h",wide?"11906":"16838");}
        for(var t:all(body,"tbl")){fit(t,t==matrix?13958:9026);InstitutionalTables.apply(t);}InstitutionalIndex.finish(doc);
    }
    private void sectionBreak(Element paragraph,Element reference,boolean landscape) {
        var section=(Element)reference.cloneNode(true);removeAll(section,"type");var type=element(section.getOwnerDocument(),"type");type.setAttributeNS(W,"w:val","nextPage");section.appendChild(type);
        for(var size:all(section,"pgSz")){if(landscape)size.setAttributeNS(W,"w:orient","landscape");else size.removeAttributeNS(W,"orient");}
        var target=property(paragraph,"sectPr");target.getParentNode().replaceChild(section,target);
    }
    private void notes(List<Element> out,Element p,Snapshot s) { out.add(paragraph(p,"Fuente: "+s.eventData().source()));out.add(paragraph(p,"Elaborado por: "+s.elaboratedBy()));if(s.eventData().privacyNoticeEnabled())out.add(paragraph(p,"Nota: "+s.eventData().privacyNotice())); }
    private String date(java.time.LocalDate value) {return value==null?"":value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
    private String time(java.time.LocalTime value) {return value==null?"":value.toString();}
    private String number(Integer value) {return value==null?"":value.toString();}
    private String eventType(String value,String other) {return switch(value){case "ACADEMIC"->"Académico";case "CULTURAL"->"Cultural";case "SOCIAL"->"Social";case "SPORTS"->"Deportivo";case "RESEARCH"->"Investigativo";case "OTHER"->other;default->"";};}
    private String modality(String value,String other) {return switch(value){case "PRESENTIAL"->"Presencial";case "VIRTUAL"->"Virtual";case "OTHER"->other;default->"";};}
}
