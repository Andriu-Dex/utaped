package ec.edu.uta.utaped.documents;

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

/** Preserves the official T2 package, including its headers, graphics, styles and section order. */
@Component
public class T2Template {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private final byte[] reference;
    public T2Template() throws IOException { try(var in=new ClassPathResource("document-templates/t2-reference.docx").getInputStream()) { reference=in.readAllBytes(); } }
    public String version() { return "T2-"+StoredFiles.hash(reference).substring(0,16)+"-A4-engine4"; }
    private static List<Element> children(Node n,String local) { var result=new ArrayList<Element>();for(Node c=n.getFirstChild();c!=null;c=c.getNextSibling()) if(c instanceof Element e&&(local==null||local.equals(e.getLocalName()))) result.add(e);return result; }
    private static List<Element> all(Node n,String local) { var nodes=((Element)n).getElementsByTagNameNS(W,local);var result=new ArrayList<Element>();for(int i=0;i<nodes.getLength();i++) result.add((Element)nodes.item(i));return result; }
    private static Element element(Document doc,String name) { return doc.createElementNS(W,"w:"+name); }
    private static Element property(Element p,String name) { var pr=children(p,"pPr").stream().findFirst().orElseGet(()->{ var e=element(p.getOwnerDocument(),"pPr");p.insertBefore(e,p.getFirstChild());return e; });return children(pr,name).stream().findFirst().orElseGet(()->{var e=element(p.getOwnerDocument(),name);pr.appendChild(e);return e;}); }
    private static void removeAll(Element n,String local) { for(var e:all(n,local)) e.getParentNode().removeChild(e); }
    private static void text(Element p,String value) {
        var runs=children(p,"r");Element style=runs.isEmpty()?null:children(runs.getFirst(),"rPr").stream().findFirst().map(e->(Element)e.cloneNode(true)).orElse(null);
        for(var e:children(p,null)) if(!e.getLocalName().equals("pPr")) p.removeChild(e);var r=element(p.getOwnerDocument(),"r");if(style!=null) r.appendChild(style);var t=element(p.getOwnerDocument(),"t");t.setAttributeNS(XMLConstants.XML_NS_URI,"xml:space","preserve");t.setTextContent(value);r.appendChild(t);p.appendChild(r);
    }
    private static Element paragraph(Element prototype,String value) { var p=(Element)prototype.cloneNode(true);text(p,value);removeAll(p,"color");removeAll(p,"sectPr");return p; }
    private static void cell(Element cell,String value) {
        // Allow long identifiers and email addresses to wrap inside the official narrow columns.
        if(!value.startsWith("@@SIG")) value=value.replaceAll("(?<=\\S)(?=[@./])","\u200B").replaceAll("(\\S{12})(?=\\S)","$1\u200B");
        var paragraphs=children(cell,"p");var prototype=paragraphs.getFirst();for(var p:paragraphs) cell.removeChild(p);for(String line:value.split("\\R",-1)) cell.appendChild(paragraph(prototype,line));
    }
    private static void lines(List<Element> out,Element prototype,String value) { for(String line:value.split("\\R",-1)) out.add(paragraph(prototype,line)); }
    public List<String> sections(Snapshot s) {
        var sections=new ArrayList<>(List.of("ANTECEDENTES","DESARROLLO DE ACTIVIDADES","CONCLUSIONES","OPORTUNIDADES DE MEJORA"));
        if(!s.report().delegations().isEmpty()) sections.add("REGISTRO DE CONTACTOS Y GESTIONES DE LA DELEGACIÓN");
        if(!s.attachments().isEmpty()) sections.add("ANEXOS");sections.addAll(List.of("FIRMAS DE RESPONSABILIDAD","CONTROL DE HISTORIAL DE CAMBIOS"));return List.copyOf(sections);
    }
    public byte[] compose(Snapshot s,Map<String,Integer> pageNumbers) {
        try(var input=new ZipInputStream(new ByteArrayInputStream(reference));var buffer=new ByteArrayOutputStream();var out=new ZipOutputStream(buffer)) {
            for(ZipEntry entry;(entry=input.getNextEntry())!=null;) {
                byte[] bytes=input.readAllBytes();
                if(entry.getName().equals("word/document.xml")||entry.getName().matches("word/(header|footer)\\d+\\.xml")) {
                    var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
                    var doc=factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
                    if(entry.getName().equals("word/document.xml")) body(doc,s,pageNumbers);else furniture(doc,s);
                    var transformers=TransformerFactory.newDefaultInstance();transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");var xml=new ByteArrayOutputStream();transformers.newTransformer().transform(new DOMSource(doc),new StreamResult(xml));bytes=xml.toByteArray();
                }
                out.putNextEntry(new ZipEntry(entry.getName()));out.write(bytes);out.closeEntry();
            }
            out.finish();return buffer.toByteArray();
        } catch(Exception e) { throw new IllegalStateException("No se pudo componer la plantilla T2.",e); }
    }
    private void body(Document doc,Snapshot s,Map<String,Integer> numbers) {
        var body=(Element)doc.getElementsByTagNameNS(W,"body").item(0);var source=children(body,null);var result=new ArrayList<Element>();var sectionNodes=new LinkedHashMap<String,Element>();
        text(source.get(5),"UNIDAD ACADÉMICA / ADMINISTRATIVA: "+s.plan().institutionalUnit());text(source.get(7),"INFORME DE: "+s.plan().title());text(source.get(9),"PERÍODO: "+s.plan().periodName());
        result.addAll(source.subList(0,20));
        for(String section:sections(s)) result.add(paragraph(source.get(20),section+" ................................ "+numbers.getOrDefault(section,0)));
        result.add(source.get(28));result.add(paragraph(source.get(20),"Tabla 1. Resultados de la matriz de actividades ................................ "+numbers.getOrDefault("DESARROLLO DE ACTIVIDADES",0)));
        heading(result,sectionNodes,source.get(32),"ANTECEDENTES");lines(result,source.get(34),s.report().background());
        result.add(paragraph(source.get(38),"Informe derivado del Plan de Trabajo, versión formal "+s.report().baseFormalVersion()+". Los resultados declarados corresponden a sus actividades aprobadas."));
        heading(result,sectionNodes,source.get(39),"DESARROLLO DE ACTIVIDADES");result.add(source.get(42));
        var table=source.get(43);var rows=children(table,"tr");var prototype=rows.get(1);for(int i=1;i<rows.size();i++) table.removeChild(rows.get(i));
        for(int i=0;i<s.activities().size();i++) {
            var a=s.activities().get(i);var report=s.report().results().get(i);var row=(Element)prototype.cloneNode(true);removeAll(row,"trHeight");var cells=children(row,"tc");
            cell(cells.get(0),a.title());cell(cells.get(1),String.join("\n",a.means()));cell(cells.get(2),report.percentage().stripTrailingZeros().toPlainString()+" %");cell(cells.get(3),report.observations());table.appendChild(row);
        }
        result.add(table);heading(result,sectionNodes,source.get(46),"CONCLUSIONES");lines(result,source.get(34),s.report().conclusions());heading(result,sectionNodes,source.get(47),"OPORTUNIDADES DE MEJORA");lines(result,source.get(34),s.report().improvements());
        if(!s.report().delegations().isEmpty()) {
            heading(result,sectionNodes,source.get(52),"REGISTRO DE CONTACTOS Y GESTIONES DE LA DELEGACIÓN");
            for(var delegation:s.report().delegations()) {
                var contact=(Element)source.get(56).cloneNode(true);var contactRows=children(contact,"tr");
                cell(children(contactRows.get(0),"tc").getFirst(),"Nombre o propósito de la delegación: "+delegation.purpose());
                cell(children(contactRows.get(1),"tc").getFirst(),"Ciudad, país o institución: "+delegation.location());
                var dataRow=(Element)contactRows.getLast().cloneNode(true);for(int i=4;i<contactRows.size();i++) contact.removeChild(contactRows.get(i));
                var cells=children(dataRow,"tc");var values=List.of(delegation.institution()+"\n"+delegation.contact(),delegation.contactDetails(),delegation.topic(),delegation.agreement());
                for(int i=0;i<cells.size();i++) cell(cells.get(i),values.get(i));contact.appendChild(dataRow);result.add(contact);
            }
        }
        if(!s.attachments().isEmpty()) { heading(result,sectionNodes,source.get(58),"ANEXOS");for(var a:s.attachments()) { result.add(paragraph(source.get(34),a.label()+". "+a.title()));if(!a.description().isBlank()) result.add(paragraph(source.get(34),a.description())); } }
        heading(result,sectionNodes,source.get(63),"FIRMAS DE RESPONSABILIDAD");property(sectionNodes.get("FIRMAS DE RESPONSABILIDAD"),"pageBreakBefore");var signatures=source.get(65);var signatureRows=children(signatures,"tr");var signaturePrototype=(Element)signatureRows.get(1).cloneNode(true);for(int i=1;i<signatureRows.size();i++) signatures.removeChild(signatureRows.get(i));
        signature(signatures,signaturePrototype,"Elaborado por:",s.plan().teacherName(),"");int slot=1;
        if(s.workflow()!=null) for(var stage:s.workflow().stages()) for(var person:stage.participants()) signature(signatures,signaturePrototype,stage.label()+":",person.name(),stage.requiresSignature()?String.valueOf(slot++):null);
        result.add(signatures);heading(result,sectionNodes,source.get(67),"CONTROL DE HISTORIAL DE CAMBIOS");var history=source.get(69);var historyRows=children(history,"tr");for(int i=1;i<historyRows.size();i++) history.removeChild(historyRows.get(i));var entry=(Element)historyRows.getFirst().cloneNode(true);removeAll(entry,"b");var cells=children(entry,"tc");cell(cells.get(0),s.plan().formalVersion());cell(cells.get(1),"Elaboración del Informe");cell(cells.get(2),s.plan().preparationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));history.appendChild(entry);result.add(history);
        result.add(source.get(76));for(var node:children(body,null)) body.removeChild(node);for(var node:result) body.appendChild(node);removeAll(body,"bookmarkStart");removeAll(body,"bookmarkEnd");
        int bookmark=1000;for(var entryNode:sectionNodes.entrySet()) {
            var p=entryNode.getValue();var start=element(doc,"bookmarkStart");start.setAttributeNS(W,"w:id",String.valueOf(bookmark));start.setAttributeNS(W,"w:name",T1Template.anchor(entryNode.getKey()));p.insertBefore(start,children(p,"r").getFirst());var end=element(doc,"bookmarkEnd");end.setAttributeNS(W,"w:id",String.valueOf(bookmark++));p.appendChild(end);
            for(var index:result) if(index.getLocalName().equals("p")&&all(index,"t").stream().anyMatch(t->t.getTextContent().startsWith(entryNode.getKey()+" ...."))) { var link=element(doc,"hyperlink");link.setAttributeNS(W,"w:anchor",T1Template.anchor(entryNode.getKey()));for(var r:children(index,"r")) link.appendChild(r);index.appendChild(link); }
        }
        for(var size:all(body,"pgSz")) { boolean landscape="landscape".equals(size.getAttributeNS(W,"orient"));size.setAttributeNS(W,"w:w",landscape?"16838":"11906");size.setAttributeNS(W,"w:h",landscape?"11906":"16838"); }
        for(var t:all(body,"tbl")) fit(t,9026);
    }
    private static void heading(List<Element> result,Map<String,Element> nodes,Element source,String label) { var p=paragraph(source,label);property(p,"keepNext");result.add(p);nodes.put(label,p); }
    private static void signature(Element table,Element prototype,String label,String name,String suffix) {
        var row=(Element)prototype.cloneNode(true);removeAll(row,"trHeight");var cells=children(row,"tc");cell(cells.get(0),label);cell(cells.get(1),name);cell(cells.get(2),"");cell(cells.get(3),suffix==null?"":"@@SIGTOP"+suffix+"@@\n@@SIGBOTTOM"+suffix+"@@");
        if(suffix!=null) { var markers=children(cells.get(3),"p");for(int i=0;i<markers.size();i++) { var p=markers.get(i);property(p,"jc").setAttributeNS(W,"w:val",i==0?"left":"right");var spacing=property(p,"spacing");spacing.setAttributeNS(W,"w:before","0");spacing.setAttributeNS(W,"w:after",i==0?"1000":"0");spacing.setAttributeNS(W,"w:line","40");spacing.setAttributeNS(W,"w:lineRule","exact");for(var r:children(p,"r")) { var pr=children(r,"rPr").stream().findFirst().orElseGet(()->{var e=element(p.getOwnerDocument(),"rPr");r.insertBefore(e,r.getFirstChild());return e;});var size=element(p.getOwnerDocument(),"sz");size.setAttributeNS(W,"w:val","2");pr.appendChild(size); } } }
        table.appendChild(row);
    }
    private void furniture(Document doc,Snapshot s) {
        var root=doc.getDocumentElement();
        for(var p:all(root,"p")) {
            String value=String.join("",all(p,"t").stream().map(Element::getTextContent).toList());
            if(value.startsWith("INFORME DE:")) text(p,"INFORME DE: "+s.plan().title());
            else if(value.startsWith("Unidad académica/ administrativa:")) text(p,"Unidad académica/ administrativa: "+s.plan().institutionalUnit());
            else if(value.startsWith("Fecha de elaboración:")) text(p,"Fecha de elaboración: "+s.plan().preparationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            if(value.contains("Formato N")) text(p,"Formato N°: UTA-SGC-A-2-1-P7-T2");
            if(all(p,"instrText").stream().anyMatch(n->n.getTextContent().contains("PAGE"))) {
                text(p,"@@PN@@");property(p,"jc").setAttributeNS(W,"w:val","right");
                for(var run:children(p,"r")) for(var size:all(run,"sz")) size.setAttributeNS(W,"w:val","4");
            }
        }
        for(var table:all(root,"tbl")) fit(table,8504);
        if(root.getLocalName().equals("ftr")) for(var table:all(root,"tbl")) {
            var columns=all(table,"gridCol");var widths=all(table,"tcW");
            if(columns.size()==3 && widths.size()==3) { int[] sizes={4404,3500,600};for(int i=0;i<3;i++) { columns.get(i).setAttributeNS(W,"w:w",String.valueOf(sizes[i]));widths.get(i).setAttributeNS(W,"w:w",String.valueOf(sizes[i])); } }
        }
    }
    private static void fit(Element table,int width) {
        int previous=all(table,"gridCol").stream().mapToInt(c->Integer.parseInt(c.getAttributeNS(W,"w"))).sum();if(previous==0) return;double factor=(double)width/previous;
        for(String tag:List.of("gridCol","tcW","tblW")) for(var size:all(table,tag)) if(tag.equals("gridCol")||size.getAttributeNS(W,"type").equals("dxa")) size.setAttributeNS(W,"w:w",String.valueOf((int)Math.round(Integer.parseInt(size.getAttributeNS(W,"w"))*factor)));
        for(var indent:all(table,"tblInd")) indent.setAttributeNS(W,"w:w","0");
    }
}
