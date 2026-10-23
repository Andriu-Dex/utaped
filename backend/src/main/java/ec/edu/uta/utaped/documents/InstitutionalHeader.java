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
            if(elements(p,"t").isEmpty()&&p.getElementsByTagNameNS(W,"drawing").getLength()==0) { cell.removeChild(p);continue; }
            var pp=prop(p,"pPr");var spacing=prop(pp,"spacing");attr(spacing,"before","0");attr(spacing,"after","0");attr(spacing,"line","200");attr(spacing,"lineRule","auto");attr(prop(pp,"jc"),"val","center");
            for(var r:elements(p,"r")) { var rp=prop(r,"rPr");attr(prop(rp,"sz"),"val","14");attr(prop(rp,"spacing"),"val","0"); }
        }
        // Preserve the official crest dimensions; shrinking it compressed the merged header.
    }
    private static void borders(Element table,List<Element> rows) {
        var tableProperties=prop(table,"tblPr");
        for(var spacing:children(tableProperties,"tblCellSpacing")) tableProperties.removeChild(spacing);
        var outer=prop(tableProperties,"tblBorders");
        for(var child:children(outer,"top")) outer.removeChild(child);
        for(String side:List.of("top","left","bottom","right","insideH","insideV")) border(prop(outer,side),true);
        for(int row=0;row<rows.size();row++) {
            var cells=children(rows.get(row),"tc");
            for(int column=0;column<cells.size();column++) {
                var cellBorders=prop(prop(cells.get(column),"tcPr"),"tcBorders");
                for(String side:List.of("top","left","bottom","right")) {
                    boolean visible=column!=0 || !side.equals("top")&&!side.equals("bottom") || side.equals("top")&&row==0 || side.equals("bottom")&&row==rows.size()-1;
                    border(prop(cellBorders,side),visible);
                }
            }
        }
    }
    private static void border(Element edge,boolean visible) {
        attr(edge,"val",visible?"single":"nil");attr(edge,"sz","8");attr(edge,"space","0");attr(edge,"color","475569");
    }
    static void apply(Document doc,ArtifactModels.Snapshot snapshot) {
        for(var table:elements(doc.getDocumentElement(),"tbl")) {
            var rows=children(table,"tr");if(rows.size()!=4 || children(rows.getFirst(),"tc").size()!=2) continue;
            var grids=elements(table,"gridCol");if(grids.size()!=3) continue;
            int[] widths={1640,2786,4078};for(int i=0;i<3;i++) attr(grids.get(i),"w",String.valueOf(widths[i]));
            var tw=prop(prop(table,"tblPr"),"tblW");attr(tw,"type","dxa");attr(tw,"w","8504");
            // Minimum heights reproduce the official rows and still allow long names to wrap.
            int[] minimumHeights={370,250,500,760};
            for(int i=0;i<rows.size();i++) {
                var height=prop(prop(rows.get(i),"trPr"),"trHeight");
                attr(height,"val",String.valueOf(minimumHeights[i]));attr(height,"hRule","atLeast");
            }
            for(int rowIndex=0;rowIndex<rows.size();rowIndex++) {
                var cells=children(rows.get(rowIndex),"tc");
                for(int i=0;i<cells.size();i++) {
                    var cp=prop(cells.get(i),"tcPr");var width=prop(cp,"tcW");attr(width,"type","dxa");attr(width,"w",String.valueOf(i==0?widths[0]:cells.size()==2?6864:widths[i]));
                    var align=prop(cp,"vAlign");attr(align,"val","center");
                    if(i>0) {
                        var margins=prop(cp,"tcMar");for(String side:List.of("top","bottom","left","right")) { var margin=prop(margins,side);attr(margin,"type","dxa");attr(margin,"w",side.equals("left")||side.equals("right")?"80":"0"); }
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
            if(last.size()==3) { rows.get(3).removeChild(last.get(2));var cp=prop(last.get(1),"tcPr");attr(prop(cp,"gridSpan"),"val","2");attr(prop(cp,"tcW"),"w","6864"); }
            borders(table,rows);
            // The header is a separate Word story: reserve its trailing space explicitly for FOP.
            for(Node sibling=table.getNextSibling();sibling!=null;sibling=sibling.getNextSibling()) {
                if(sibling instanceof Element p&&p.getLocalName().equals("p")) {
                    var spacing=prop(prop(p,"pPr"),"spacing");
                    attr(spacing,"before","0");attr(spacing,"after","360");attr(spacing,"line","100");attr(spacing,"lineRule","exact");
                    break;
                }
            }
        }
    }
}
