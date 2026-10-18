package ec.edu.uta.utaped.documents;

import java.time.format.DateTimeFormatter;
import java.util.*;
import org.w3c.dom.*;

/** Repairs the source header grid without changing its institutional graphics or document body. */
final class InstitutionalHeader {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private InstitutionalHeader() {}
    private static List<Element> elements(Element node,String tag) {
        var result=new ArrayList<Element>();var nodes=node.getElementsByTagNameNS(W,tag);for(int i=0;i<nodes.getLength();i++) result.add((Element)nodes.item(i));return result;
    }
    private static List<Element> children(Element node,String tag) {
        var result=new ArrayList<Element>();for(Node n=node.getFirstChild();n!=null;n=n.getNextSibling()) if(n instanceof Element e&&e.getLocalName().equals(tag)) result.add(e);return result;
    }
    private static Element node(Document doc,String tag) { return doc.createElementNS(W,"w:"+tag); }
    private static Element prop(Element parent,String tag) {
        return children(parent,tag).stream().findFirst().orElseGet(()->{var n=node(parent.getOwnerDocument(),tag);parent.insertBefore(n,parent.getFirstChild());return n;});
    }
    private static void attr(Element element,String name,String value) { element.setAttributeNS(W,"w:"+name,value); }
    private static void setText(Element cell,String value,int size,boolean bold,String color) {
        var doc=cell.getOwnerDocument();for(var paragraph:children(cell,"p")) cell.removeChild(paragraph);
        var p=node(doc,"p");var pr=node(doc,"pPr");p.appendChild(pr);var spacing=node(doc,"spacing");attr(spacing,"before","0");attr(spacing,"after","0");attr(spacing,"line","240");attr(spacing,"lineRule","auto");pr.appendChild(spacing);
        var r=node(doc,"r");var rp=node(doc,"rPr");var font=node(doc,"rFonts");attr(font,"ascii","Times New Roman");attr(font,"hAnsi","Times New Roman");rp.appendChild(font);
        var sz=node(doc,"sz");attr(sz,"val",String.valueOf(size));rp.appendChild(sz);var ink=node(doc,"color");attr(ink,"val",color);rp.appendChild(ink);if(bold) rp.appendChild(node(doc,"b"));r.appendChild(rp);var text=node(doc,"t");text.setTextContent(value);r.appendChild(text);p.appendChild(r);cell.appendChild(p);
    }
    private static void logo(Element cell) {
        for(var p:children(cell,"p")) {
            var pp=prop(p,"pPr");var spacing=prop(pp,"spacing");attr(spacing,"before","0");attr(spacing,"after","0");attr(spacing,"line","200");attr(spacing,"lineRule","auto");attr(prop(pp,"jc"),"val","center");
            for(var r:elements(p,"r")) { var rp=prop(r,"rPr");attr(prop(rp,"sz"),"val","14");attr(prop(rp,"spacing"),"val","0"); }
        }
        var extents=cell.getElementsByTagNameNS("http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing","extent");
        if(extents.getLength()>0) {
            var extent=(Element)extents.item(0);double ratio=Math.min(1d,508000d/Long.parseLong(extent.getAttribute("cy")));
            for(String local:List.of("extent","ext")) { var sizes=cell.getElementsByTagNameNS("*",local);for(int i=0;i<sizes.getLength();i++) { var size=(Element)sizes.item(i);if(size.hasAttribute("cx")&&size.hasAttribute("cy")) { size.setAttribute("cx",String.valueOf(Math.round(Long.parseLong(size.getAttribute("cx"))*ratio)));size.setAttribute("cy",String.valueOf(Math.round(Long.parseLong(size.getAttribute("cy"))*ratio))); } } }
        }
    }
    static void apply(Document doc,ArtifactModels.Snapshot snapshot) {
        for(var table:elements(doc.getDocumentElement(),"tbl")) {
            var rows=children(table,"tr");if(rows.size()!=4 || children(rows.getFirst(),"tc").size()!=2) continue;
            var grids=elements(table,"gridCol");if(grids.size()!=3) continue;
            int[] widths={1500,3502,3502};for(int i=0;i<3;i++) attr(grids.get(i),"w",String.valueOf(widths[i]));
            var tw=prop(prop(table,"tblPr"),"tblW");attr(tw,"type","dxa");attr(tw,"w","8504");
            for(var height:elements(table,"trHeight")) height.getParentNode().removeChild(height);
            for(int rowIndex=0;rowIndex<rows.size();rowIndex++) {
                var cells=children(rows.get(rowIndex),"tc");
                for(int i=0;i<cells.size();i++) {
                    var cp=prop(cells.get(i),"tcPr");var width=prop(cp,"tcW");attr(width,"type","dxa");attr(width,"w",String.valueOf(i==0?widths[0]:cells.size()==2?7004:widths[i]));
                    var align=prop(cp,"vAlign");attr(align,"val","center");
                    if(i>0) {
                        var margins=prop(cp,"tcMar");for(String side:List.of("top","bottom","left","right")) { var margin=prop(margins,side);attr(margin,"type","dxa");attr(margin,"w",side.equals("left")||side.equals("right")?"80":"40"); }
                        var shade=prop(cp,"shd");attr(shade,"fill",rowIndex==1||rowIndex==3?"E7E6E6":"FFFFFF"); }
                }
            }
            logo(children(rows.get(0),"tc").getFirst());
            var first=children(rows.get(0),"tc").get(1);setText(first,"UNIVERSIDAD TÉCNICA DE AMBATO",22,true,"000000");attr(prop(prop(children(first,"p").getFirst(),"pPr"),"jc"),"val","center");
            String title=snapshot.plan().title().replaceFirst("(?i)^Plan de Trabajo\\s*:\\s*","").replaceFirst("(?i)^Informe(?: final)?\\s*:\\s*","");
            setText(children(rows.get(1),"tc").get(1),(snapshot.plan().documentType().equals("T2")?"INFORME DE: ":"PLAN DE TRABAJO: ")+title,18,true,"1F4E79");
            var data=children(rows.get(2),"tc");setText(data.get(1),"Unidad académica: "+snapshot.plan().institutionalUnit(),18,false,"000000");setText(data.get(2),"Carrera: "+snapshot.plan().career(),18,false,"000000");
            var last=children(rows.get(3),"tc");setText(last.get(1),"Fecha de elaboración: "+snapshot.plan().preparationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),18,false,"1F4E79");
            // Keep date on one full-width cell, instead of an empty grey column beside it.
            if(last.size()==3) { rows.get(3).removeChild(last.get(2));var cp=prop(last.get(1),"tcPr");attr(prop(cp,"gridSpan"),"val","2");attr(prop(cp,"tcW"),"w","7004"); }
        }
    }
}
