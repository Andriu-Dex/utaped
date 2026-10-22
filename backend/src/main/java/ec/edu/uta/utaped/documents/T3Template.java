package ec.edu.uta.utaped.documents;

import static ec.edu.uta.utaped.documents.T2Template.*;
import ec.edu.uta.utaped.documents.ArtifactModels.Snapshot;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

/** Official execution report package (UTA-SGC-A-1-1-P4-T2): headers, styles and section order are preserved. */
@Component
public class T3Template {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    static final List<String> SECTIONS=List.of("RESULTADOS DEL PLAN DE ACCIÓN","ANÁLISIS DE RESULTADOS","CONCLUSIONES Y/O RECOMENDACIONES","ACCIONES DE MEJORA","ANEXOS","FIRMAS DE RESPONSABILIDAD","CONTROL DE HISTORIAL DE CAMBIOS");
    private static final DateTimeFormatter PRESENTATION=DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",Locale.forLanguageTag("es-EC"));
    private final byte[] reference;
    public T3Template() throws IOException { try(var in=new ClassPathResource("document-templates/t3-reference.docx").getInputStream()) { reference=in.readAllBytes(); } }
    public String version() { return "T3-"+StoredFiles.hash(reference).substring(0,16)+"-A4-engine7"; }
    public List<String> sections(Snapshot s) { return SECTIONS; }
    private static String title(Snapshot s) { return "INFORME DE EJECUCIÓN "+ec.edu.uta.utaped.planning.ExecutionReportService.roman(s.execution().sequenceNumber())+": "+s.plan().groupName().toUpperCase(Locale.ROOT); }
    public byte[] compose(Snapshot s,Map<String,Integer> pageNumbers) {
        try(var input=new ZipInputStream(new ByteArrayInputStream(reference));var buffer=new ByteArrayOutputStream();var out=new ZipOutputStream(buffer)) {
            for(ZipEntry entry;(entry=input.getNextEntry())!=null;) {
                byte[] bytes=input.readAllBytes();
                if(entry.getName().equals("word/document.xml")||entry.getName().matches("word/(header|footer)\\d+\\.xml")) {
                    var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
                    var doc=factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
                    if(entry.getName().equals("word/document.xml")) { body(doc,s,pageNumbers);StructuredSections.apply(doc,s,pageNumbers); }else furniture(doc,s);
                    var transformers=TransformerFactory.newDefaultInstance();transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
                    var xml=new ByteArrayOutputStream();transformers.newTransformer().transform(new DOMSource(doc),new StreamResult(xml));bytes=xml.toByteArray();
                }
                out.putNextEntry(new ZipEntry(entry.getName()));out.write(bytes);out.closeEntry();
            }
            out.finish();return buffer.toByteArray();
        } catch(Exception e) { throw new IllegalStateException("No se pudo componer la plantilla del Informe de ejecución.",e); }
    }
    private static List<String> items(String value) { return Arrays.stream(value.split("\\R")).map(String::strip).filter(line->!line.isEmpty()).toList(); }
    private static void list(List<Element> out,Element prototype,String value) { var lines=items(value);for(String line:lines.isEmpty()?List.of("No aplica."):lines) out.add(paragraph(prototype,line)); }
    private void body(Document doc,Snapshot s,Map<String,Integer> numbers) {
        var body=(Element)doc.getElementsByTagNameNS(W,"body").item(0);var source=children(body,null);var result=new ArrayList<Element>();var sectionNodes=new LinkedHashMap<String,Element>();
        var data=s.execution();var plain=source.get(36);
        text(source.get(13),title(s));text(source.get(15),"PERÍODO: "+s.plan().periodName().toUpperCase(Locale.ROOT));
        // Blank paragraphs in the Word cover have opaque white shading over a floating logo.
        // Keep the original graphic and positioning, but let it remain visible in the PDF renderer.
        for(var cover:source.subList(0,9)) removeAll(cover,"shd");
        property(source.get(19),"pageBreakBefore");
        result.addAll(source.subList(0,21));
        // The template's static table of contents is rebuilt with the real page numbers.
        var tocPrototype=all(source.get(21),"p").stream().filter(p->all(p,"pStyle").stream().anyMatch(st->st.getAttributeNS(W,"val").equals("TDC1"))).findFirst().orElseThrow();
        for(int i=0;i<SECTIONS.size();i++) result.add(paragraph(tocPrototype,(i+1)+". "+SECTIONS.get(i)+" ................................ "+numbers.getOrDefault(SECTIONS.get(i),0)));
        result.addAll(source.subList(22,27));
        heading(result,sectionNodes,source.get(27),SECTIONS.get(0));result.add(source.get(28));
        lines(result,plain,s.plan().objective().isBlank()?"No registrado en el Plan.":s.plan().objective());
        result.add(paragraph(plain,"Informe de ejecución derivado del Plan de Trabajo, versión formal "+data.baseFormalVersion()+"."));
        var table=source.get(31);var rows=children(table,"tr");var groupPrototype=rows.get(1);var rowPrototype=rows.get(2);
        for(int i=1;i<rows.size();i++) table.removeChild(rows.get(i));
        String category=null;
        for(int i=0;i<s.activities().size();i++) {
            var a=s.activities().get(i);var outcome=data.results().get(i);
            String label=switch(a.category()==null?"":a.category()) { case "POA" -> "PLAN OPERATIVO ANUAL";case "IMPROVEMENT_PLAN" -> "PLAN DE MEJORAS";case "IMPROVEMENT_ACTION" -> "ACCIONES DE MEJORA";default -> "OTRAS ACTIVIDADES"; };
            if(!label.equals(category)) { var groupRow=(Element)groupPrototype.cloneNode(true);removeAll(groupRow,"trHeight");cell(children(groupRow,"tc").getFirst(),label);table.appendChild(groupRow);category=label; }
            var row=(Element)rowPrototype.cloneNode(true);removeAll(row,"trHeight");var cells=children(row,"tc");
            cell(cells.get(0),a.title());cell(cells.get(1),String.join("\n",a.means()));cell(cells.get(2),outcome.percentage().stripTrailingZeros().toPlainString()+" %");cell(cells.get(3),outcome.observations());table.appendChild(row);
        }
        result.add(table);
        heading(result,sectionNodes,source.get(32),SECTIONS.get(1));lines(result,plain,data.analysis());
        result.add(paragraph(source.get(35),"ANÁLISIS FODA"));removeAll(source.get(36),"color");result.add(source.get(36));
        var foda=List.of(List.of(source.get(37),data.strengths()),List.of(source.get(42),data.weaknesses()),List.of(source.get(48),data.opportunities()),List.of(source.get(53),data.threats()));
        for(var part:foda) { removeAll((Element)part.get(0),"color");result.add((Element)part.get(0));list(result,source.get(38),(String)part.get(1)); }
        heading(result,sectionNodes,source.get(58),SECTIONS.get(2));
        result.add(source.get(59));list(result,source.get(61),data.conclusions());
        result.add(source.get(63));list(result,source.get(61),data.recommendations());
        heading(result,sectionNodes,source.get(67),SECTIONS.get(3));
        var actions=source.get(69);var actionRows=children(actions,"tr");var actionPrototype=actionRows.get(1);actions.removeChild(actionPrototype);
        for(var action:data.improvementActions().isEmpty()?List.of(new ec.edu.uta.utaped.planning.ExecutionReportService.Action("No aplica.","No aplica.")):data.improvementActions()) {
            var row=(Element)actionPrototype.cloneNode(true);removeAll(row,"trHeight");var cells=children(row,"tc");cell(cells.get(0),action.situation());cell(cells.get(1),action.action());actions.appendChild(row);
        }
        result.add(actions);
        heading(result,sectionNodes,source.get(71),SECTIONS.get(4));
        result.add(paragraph(source.get(73),"Enlace de evidencias: "+(data.evidenceLink().isBlank()?"No aplica.":data.evidenceLink())));
        for(var a:s.attachments()) { result.add(paragraph(plain,a.label()+". "+a.title()));if(!a.description().isBlank()) result.add(paragraph(plain,a.description())); }
        if(s.externalReferences()!=null) for(var reference:s.externalReferences()) result.add(paragraph(plain,"Referencia externa · "+reference.activity()+" · "+reference.medium()+": "+reference.url()));
        heading(result,sectionNodes,source.get(76),SECTIONS.get(5));property(sectionNodes.get(SECTIONS.get(5)),"pageBreakBefore");
        var signatures=source.get(79);var signatureRows=children(signatures,"tr");var signaturePrototype=(Element)signatureRows.get(1).cloneNode(true);for(int i=1;i<signatureRows.size();i++) signatures.removeChild(signatureRows.get(i));
        for(int i=0;i<s.resolvedAuthors().size();i++) { var author=s.resolvedAuthors().get(i);signature(signatures,signaturePrototype,"Elaborado por:",author.name(),author.position(),i==0?"":"A"+i); }
        int slot=1;
        if(s.workflow()!=null) for(var stage:s.workflow().stages()) {
            if(stage.recipientKind().equals("COLLEGIATE") && !stage.requiresSignature()) signature(signatures,signaturePrototype,stage.label()+":",stage.recipientLabel(),"",null);
            else for(var person:stage.participants()) signature(signatures,signaturePrototype,stage.label()+":",person.name(),person.position()==null?"":person.position(),stage.requiresSignature()?String.valueOf(slot++):null);
        }
        result.add(signatures);
        heading(result,sectionNodes,source.get(82),SECTIONS.get(6));
        var history=source.get(84);var historyRows=children(history,"tr");var entry=historyRows.get(1);var cells=children(entry,"tc");
        cell(cells.get(0),"V."+s.plan().formalVersion());cell(cells.get(1),"Elaboración del Informe de ejecución");cell(cells.get(2),s.plan().preparationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        for(int i=2;i<historyRows.size();i++) history.removeChild(historyRows.get(i));
        result.add(history);result.add(source.get(source.size()-1));
        for(var node:children(body,null)) body.removeChild(node);for(var node:result) body.appendChild(node);removeAll(body,"bookmarkStart");removeAll(body,"bookmarkEnd");
        int bookmark=1000;for(var entryNode:sectionNodes.entrySet()) {
            var p=entryNode.getValue();var start=element(doc,"bookmarkStart");start.setAttributeNS(W,"w:id",String.valueOf(bookmark));start.setAttributeNS(W,"w:name",T1Template.anchor(entryNode.getKey()));p.insertBefore(start,children(p,"r").getFirst());
            var end=element(doc,"bookmarkEnd");end.setAttributeNS(W,"w:id",String.valueOf(bookmark++));p.appendChild(end);
        }
        for(var size:all(body,"pgSz")) { boolean landscape="landscape".equals(size.getAttributeNS(W,"orient"));size.setAttributeNS(W,"w:w",landscape?"16838":"11906");size.setAttributeNS(W,"w:h",landscape?"11906":"16838"); }
        for(var t:all(body,"tbl")) { fit(t,9026);grid(t); }
    }
    private static void grid(Element table) {
        var doc=table.getOwnerDocument();var properties=children(table,"tblPr").stream().findFirst().orElseGet(()->{var e=element(doc,"tblPr");table.insertBefore(e,table.getFirstChild());return e;});
        var targets=new ArrayList<Element>();removeAll(properties,"tblBorders");var outer=element(doc,"tblBorders");properties.appendChild(outer);targets.add(outer);
        for(var cell:all(table,"tc")) { var cp=children(cell,"tcPr").stream().findFirst().orElseGet(()->{var e=element(doc,"tcPr");cell.insertBefore(e,cell.getFirstChild());return e;});removeAll(cp,"tcBorders");var edges=element(doc,"tcBorders");cp.appendChild(edges);targets.add(edges); }
        for(var target:targets)for(String edge:target==outer?List.of("top","left","bottom","right","insideH","insideV"):List.of("top","left","bottom","right")) {var border=element(doc,edge);border.setAttributeNS(W,"w:val","single");border.setAttributeNS(W,"w:sz","4");border.setAttributeNS(W,"w:color","000000");target.appendChild(border);}
    }
    private void furniture(Document doc,Snapshot s) {
        var root=doc.getDocumentElement();
        for(var p:all(root,"p")) {
            String value=String.join("",all(p,"t").stream().map(Element::getTextContent).toList());
            if(value.startsWith("INFORME DE EJECUCI")) text(p,title(s));
            else if(value.startsWith("Período:")) text(p,"Período: "+s.plan().periodName());
            else if(value.startsWith("Fecha de Presentación:")) text(p,"Fecha de Presentación: "+s.plan().preparationDate().format(PRESENTATION));
            if(all(p,"instrText").stream().anyMatch(n->n.getTextContent().contains("PAGE"))) {
                text(p,"Página: @@PN@@");property(p,"jc").setAttributeNS(W,"w:val","right");
            }
        }
        for(var table:all(root,"tbl")) fit(table,8504);
    }
}
