package ec.edu.uta.utaped.documents;

import static ec.edu.uta.utaped.documents.ArtifactModels.*;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

@Component
public class T1Template {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private final byte[] reference;private final String format;
    public T1Template(@Value("${app.documents.page-format:A4}") String format) throws IOException {
        if(!Set.of("A4","REFERENCE").contains(format)) throw new IllegalArgumentException("DOCUMENT_PAGE_FORMAT must be A4 or REFERENCE.");
        this.format=format;try(var input=new ClassPathResource("document-templates/t1-reference.docx").getInputStream()) { this.reference=input.readAllBytes(); }
    }
    public String version() { return "T1-"+StoredFiles.hash(reference).substring(0,16)+"-"+format+"-engine1"; }
    private static List<Element> children(Node node,String local) {
        var result=new ArrayList<Element>();for(Node n=node.getFirstChild();n!=null;n=n.getNextSibling()) if(n instanceof Element e && (local==null || local.equals(e.getLocalName()))) result.add(e);return result;
    }
    private static List<Element> all(Node node,String local) {
        NodeList list=((Element)node).getElementsByTagNameNS(W,local);var result=new ArrayList<Element>();for(int i=0;i<list.getLength();i++) result.add((Element)list.item(i));return result;
    }
    private static Element element(Document d,String name) { return d.createElementNS(W,"w:"+name); }
    private static Element property(Element p,String name) {
        var pr=children(p,"pPr").stream().findFirst().orElseGet(()->{var e=element(p.getOwnerDocument(),"pPr");p.insertBefore(e,p.getFirstChild());return e;});
        return children(pr,name).stream().findFirst().orElseGet(()->{var e=element(p.getOwnerDocument(),name);pr.appendChild(e);return e;});
    }
    private static void text(Element p,String value) {
        var runs=children(p,"r");Element style=null;
        if(!runs.isEmpty()) style=children(runs.getFirst(),"rPr").stream().findFirst().map(e->(Element)e.cloneNode(true)).orElse(null);
        for(var n:children(p,null)) if(!"pPr".equals(n.getLocalName())) p.removeChild(n);
        var run=element(p.getOwnerDocument(),"r");if(style!=null) run.appendChild(style);
        var t=element(p.getOwnerDocument(),"t");t.setAttributeNS(XMLConstants.XML_NS_URI,"xml:space","preserve");t.setTextContent(value);run.appendChild(t);p.appendChild(run);
    }
    private static Element paragraph(Element prototype,String value) { var p=(Element)prototype.cloneNode(true);text(p,value);return p; }
    private static void cell(Element cell,String value) {
        var paragraphs=children(cell,"p");var prototype=paragraphs.getFirst();for(var p:paragraphs) cell.removeChild(p);
        for(String line:value.split("\\R",-1)) cell.appendChild(paragraph(prototype,line));
    }
    private static String date(String value) { return java.time.LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    private static void keepWithNext(Element p) { property(p,"keepNext"); }
    private static void removeAll(Element root,String name) { for(var e:all(root,name)) e.getParentNode().removeChild(e); }
    private static void fitTable(Element table,int width) {
        var columns=all(table,"gridCol");int previous=columns.stream().mapToInt(c->Integer.parseInt(c.getAttributeNS(W,"w"))).sum();
        double factor=(double)width/previous;
        for(String tag:List.of("gridCol","tcW","tblW")) for(var size:all(table,tag)) {
            if(tag.equals("gridCol") || "dxa".equals(size.getAttributeNS(W,"type"))) size.setAttributeNS(W,"w:w",String.valueOf((int)Math.round(Integer.parseInt(size.getAttributeNS(W,"w"))*factor)));
        }
        for(var indent:all(table,"tblInd")) indent.setAttributeNS(W,"w:w","0");
    }
    public byte[] compose(Snapshot s,Map<String,Integer> indexPages) {
        try {
            var input=new ZipInputStream(new ByteArrayInputStream(reference));var buffer=new ByteArrayOutputStream();var out=new ZipOutputStream(buffer);
            for(ZipEntry entry;(entry=input.getNextEntry())!=null;) {
                byte[] bytes=input.readAllBytes();
                if(entry.getName().equals("word/document.xml") || entry.getName().matches("word/(header|footer)\\d+\\.xml")) {
                    var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
                    var doc=factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
                    if(entry.getName().equals("word/document.xml")) body(doc,s,indexPages);
                    else if(entry.getName().startsWith("word/header")) header(doc,s);
                    else footer(doc);
                    var transformers=TransformerFactory.newDefaultInstance();transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
                    var xml=new ByteArrayOutputStream();transformers.newTransformer().transform(new DOMSource(doc),new StreamResult(xml));bytes=xml.toByteArray();
                }
                out.putNextEntry(new ZipEntry(entry.getName()));out.write(bytes);out.closeEntry();
            }
            out.close();input.close();return buffer.toByteArray();
        } catch(Exception e) { throw new IllegalStateException("No se pudo componer la plantilla T1.",e); }
    }
    private void body(Document doc,Snapshot s,Map<String,Integer> pageNumbers) {
        var body=(Element)doc.getElementsByTagNameNS(W,"body").item(0);var source=children(body,null);
        // Fixed source-node positions are a versioned template binding, never PDF page numbers.
        text(source.get(4),"UNIDAD ACADÉMICA / ADMINISTRATIVA: "+s.plan().institutionalUnit());
        text(source.get(6),"PLAN DE TRABAJO DE: "+s.plan().title());text(source.get(9),"PERÍODO: "+s.plan().periodName());
        var result=new ArrayList<Element>();result.addAll(source.subList(0,14));
        if(!s.plan().career().isBlank()) result.add(8,paragraph(source.get(9),"CARRERA: "+s.plan().career()));
        result.add(source.get(14));
        var sections=new ArrayList<>(List.of("JUSTIFICACIÓN","OBJETIVO","MATRIZ DE ACTIVIDADES"));
        if(!s.attachments().isEmpty()) sections.add("ANEXOS");sections.addAll(List.of("FIRMAS DE RESPONSABILIDAD","CONTROL DE HISTORIAL DE CAMBIOS"));
        for(String section:sections) result.add(paragraph(source.get(16),section+" ................................ "+pageNumbers.getOrDefault(section,0)));
        result.add(source.get(25));result.add(paragraph(source.get(27),"Tabla 1. Matriz de actividades ................................ "+pageNumbers.getOrDefault("MATRIZ DE ACTIVIDADES",0)));result.add(source.get(28));
        keepWithNext(source.get(29));keepWithNext(source.get(30));result.add(source.get(29));result.add(source.get(30));
        for(String line:s.plan().justification().split("\\R",-1)) result.add(paragraph(source.get(31),line));
        keepWithNext(source.get(36));keepWithNext(source.get(37));result.add(source.get(36));result.add(source.get(37));
        String[] objective=s.plan().objective().split("\\R",-1);
        for(int i=0;i<objective.length;i++) { var p=paragraph(source.get(38),objective[i]);removeAll(p,"color");if(i<objective.length-1) removeAll(p,"sectPr");result.add(p); }
        result.add(source.get(39));result.add(source.get(40));result.add(source.get(41));keepWithNext(source.get(41));
        var table=source.get(42);var rows=children(table,"tr");var rowTemplate=(Element)rows.get(2).cloneNode(true);
        for(int i=2;i<rows.size();i++) table.removeChild(rows.get(i));
        for(int i=0;i<2;i++) { var headerRow=rows.get(i);var trPr=children(headerRow,"trPr").stream().findFirst().orElseGet(()->{var e=element(doc,"trPr");headerRow.insertBefore(e,headerRow.getFirstChild());return e;});trPr.appendChild(element(doc,"tblHeader")); }
        for(var a:s.activities()) {
            var row=(Element)rowTemplate.cloneNode(true);removeAll(row,"trHeight");removeAll(row,"cantSplit");var cells=children(row,"tc");
            List<String> values=List.of(a.title(),date(a.startsOn()),date(a.endsOn()),a.responsibleDisplay(),String.join("\n",a.resources()),String.join("\n",a.means()));
            for(int i=0;i<6;i++) cell(cells.get(i),values.get(i));table.appendChild(row);
        }
        result.add(table);text(source.get(43),"Fuente: "+s.matrixSource());text(source.get(44),"Elaborado por: "+s.elaboratedBy());result.add(source.get(43));result.add(source.get(44));
        if(s.privacyNoticeEnabled()) result.add(paragraph(source.get(47),"Nota: Los datos proporcionados serán tratados conforme a la Ley Orgánica de Protección de Datos Personales, garantizando su confidencialidad, seguridad y uso responsable, y serán utilizados exclusivamente para fines institucionales."));
        result.add(source.get(48));
        if(!s.attachments().isEmpty()) {
            property(source.get(50),"pageBreakBefore");result.add(source.get(50));
            for(var a:s.attachments()) { var p=paragraph(source.get(51),a.label()+". "+a.title());removeAll(p,"color");keepWithNext(p);result.add(p);if(!a.description().isBlank()) result.add(paragraph(source.get(52),a.description())); }
        }
        property(source.get(54),"pageBreakBefore");keepWithNext(source.get(54));result.add(source.get(54));result.add(source.get(55));
        var signatures=source.get(56);var signatureRows=children(signatures,"tr");for(int i=2;i<signatureRows.size();i++) signatures.removeChild(signatureRows.get(i));
        var signatureCells=children(signatureRows.get(1),"tc");cell(signatureCells.get(0),"Elaborado por:");cell(signatureCells.get(1),s.plan().teacherName());cell(signatureCells.get(2),"");cell(signatureCells.get(3),"");result.add(signatures);
        result.add(source.get(57));result.add(source.get(58));keepWithNext(source.get(59));result.add(source.get(59));
        var history=source.get(60);var historyRows=children(history,"tr");
        // The reference merges the first two cells in its instructional example row.
        // Use the three-column header grid for an actual history entry.
        var historyRow=(Element)historyRows.getFirst().cloneNode(true);history.replaceChild(historyRow,historyRows.get(1));
        removeAll(historyRow,"b");var historyCells=children(historyRow,"tc");cell(historyCells.get(0),"v"+s.plan().formalVersion());cell(historyCells.get(1),"Elaboración del Plan de Trabajo");cell(historyCells.get(2),s.plan().preparationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));result.add(history);
        result.add(source.get(64));for(var n:children(body,null)) body.removeChild(n);for(var n:result) body.appendChild(n);
        removeAll(body,"bookmarkStart");removeAll(body,"bookmarkEnd");
        var sectionNodes=Map.of("JUSTIFICACIÓN",source.get(29),"OBJETIVO",source.get(36),"MATRIZ DE ACTIVIDADES",source.get(39),"ANEXOS",source.get(50),"FIRMAS DE RESPONSABILIDAD",source.get(54),"CONTROL DE HISTORIAL DE CAMBIOS",source.get(59));
        int bookmarkId=900;
        for(var section:sections) {
            var p=sectionNodes.get(section);var start=element(doc,"bookmarkStart");start.setAttributeNS(W,"w:id",String.valueOf(bookmarkId));start.setAttributeNS(W,"w:name",anchor(section));
            p.insertBefore(start,children(p,"r").getFirst());var end=element(doc,"bookmarkEnd");end.setAttributeNS(W,"w:id",String.valueOf(bookmarkId++));p.appendChild(end);
            for(var index:result) if("p".equals(index.getLocalName()) && all(index,"t").stream().anyMatch(t->t.getTextContent().startsWith(section+" ...."))) {
                var link=element(doc,"hyperlink");link.setAttributeNS(W,"w:anchor",anchor(section));for(var r:children(index,"r")) link.appendChild(r);index.appendChild(link);
            }
        }
        if(format.equals("A4")) for(var size:all(body,"pgSz")) {
            boolean landscape="landscape".equals(size.getAttributeNS(W,"orient"));size.setAttributeNS(W,"w:w",landscape?"16838":"11906");size.setAttributeNS(W,"w:h",landscape?"11906":"16838");
        }
        if(format.equals("A4")) { fitTable(signatures,9026);fitTable(history,9026); }
    }
    public static String anchor(String section) { return "UTAPED_"+java.text.Normalizer.normalize(section,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace(' ','_'); }
    private void header(Document doc,Snapshot s) {
        var root=doc.getDocumentElement();for(var p:all(root,"p")) {
            String value=String.join("",all(p,"t").stream().map(Element::getTextContent).toList());
            if(value.startsWith("PLAN DE TRABAJO:")) text(p,"PLAN DE TRABAJO: "+s.plan().title());
            else if(value.startsWith("Unidad académica/ administrativa:")) text(p,"Unidad académica/ administrativa: "+s.plan().institutionalUnit());
            else if(value.startsWith("Fecha de elaboración:")) text(p,"Fecha de elaboración: "+s.plan().preparationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        if(!s.plan().career().isBlank()) for(var row:all(root,"tr")) {
            var cells=children(row,"tc");
            if(cells.size()==3 && all(cells.get(1),"t").stream().anyMatch(t->t.getTextContent().startsWith("Unidad académica/ administrativa:"))) {
                for(var p:children(cells.get(2),"p")) cells.get(2).removeChild(p);
                var p=(Element)children(cells.get(1),"p").getFirst().cloneNode(true);text(p,"Carrera: "+s.plan().career());cells.get(2).appendChild(p);
            }
        }
        if(format.equals("A4")) for(var table:all(root,"tbl")) fitTable(table,8504);
    }
    private void footer(Document doc) {
        var root=doc.getDocumentElement();for(var p:all(root,"p")) if(all(p,"instrText").stream().anyMatch(n->n.getTextContent().contains("PAGE"))) {
            boolean field=false;var remove=new ArrayList<Element>();
            for(var run:children(p,"r")) {
                if(!all(run,"fldChar").isEmpty() && "begin".equals(all(run,"fldChar").getFirst().getAttributeNS(W,"fldCharType"))) field=true;
                if(field) remove.add(run);
                if(!all(run,"fldChar").isEmpty() && "end".equals(all(run,"fldChar").getFirst().getAttributeNS(W,"fldCharType"))) field=false;
            }
            for(var r:remove) p.removeChild(r);
            var run=element(doc,"r");var t=element(doc,"t");t.setTextContent("@@PN@@");run.appendChild(t);p.appendChild(run);
        }
        if(format.equals("A4")) for(var table:all(root,"tbl")) fitTable(table,8504);
    }
}
