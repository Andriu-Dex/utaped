package ec.edu.uta.utaped.documents;

import java.time.format.DateTimeFormatter;
import java.util.*;
import org.w3c.dom.*;

/** Repairs the source header grid without changing its institutional graphics or document body. */
final class InstitutionalHeader {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final DateTimeFormatter SPANISH_DATE=DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",Locale.forLanguageTag("es-EC"));
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

    private static void setCellContent(Element cell,List<String> lines,int size,boolean bold,String color,int charSpacing,String align) {
        var doc=cell.getOwnerDocument();
        for(var paragraph:children(cell,"p")) cell.removeChild(paragraph);
        for(String line:lines) {
            if(line==null||line.isBlank()) continue;
            var p=node(doc,"p");var pr=node(doc,"pPr");p.appendChild(pr);
            if(align!=null) { var jc=node(doc,"jc");attr(jc,"val",align);pr.appendChild(jc); }
            var spacing=node(doc,"spacing");attr(spacing,"before","0");attr(spacing,"after","0");attr(spacing,"line","220");attr(spacing,"lineRule","auto");pr.appendChild(spacing);
            var r=node(doc,"r");var rp=node(doc,"rPr");var font=node(doc,"rFonts");attr(font,"ascii","Times New Roman");attr(font,"hAnsi","Times New Roman");rp.appendChild(font);
            var sz=node(doc,"sz");attr(sz,"val",String.valueOf(size));rp.appendChild(sz);
            var ink=node(doc,"color");attr(ink,"val",color);rp.appendChild(ink);
            if(bold) rp.appendChild(node(doc,"b"));
            if(charSpacing>0) { var cs=node(doc,"spacing");attr(cs,"val",String.valueOf(charSpacing));rp.appendChild(cs); }
            r.appendChild(rp);
            var text=node(doc,"t");text.setAttributeNS(javax.xml.XMLConstants.XML_NS_URI,"xml:space","preserve");text.setTextContent(line);
            r.appendChild(text);p.appendChild(r);cell.appendChild(p);
        }
        if(children(cell,"p").isEmpty()) cell.appendChild(node(doc,"p"));
    }

    private static void setText(Element cell,String value,int size,boolean bold,String color,int charSpacing,String align) {
        setCellContent(cell,List.of(value),size,bold,color,charSpacing,align);
    }

    private static void logo(Element cell) {
        for(var p:children(cell,"p")) {
            if(elements(p,"t").isEmpty()&&p.getElementsByTagNameNS(W,"drawing").getLength()==0) { cell.removeChild(p);continue; }
            var pp=prop(p,"pPr");var spacing=prop(pp,"spacing");attr(spacing,"before","0");attr(spacing,"after","0");attr(spacing,"line","200");attr(spacing,"lineRule","auto");attr(prop(pp,"jc"),"val","center");
            for(var r:elements(p,"r")) { var rp=prop(r,"rPr");attr(prop(rp,"sz"),"val","14");attr(prop(rp,"spacing"),"val","0"); }
            var tNodes=elements(p,"t");
            if(!tNodes.isEmpty()) {
                String full=tNodes.stream().map(Node::getTextContent).collect(java.util.stream.Collectors.joining());
                if(full.contains("SISTEMA DE GESTIÓN")||full.contains("SISTEMA DE")||full.contains("CALIDAD")) {
                    var doc=cell.getOwnerDocument();
                    for(var r:elements(p,"r")) r.getParentNode().removeChild(r);
                    var r1=node(doc,"r");var rp1=node(doc,"rPr");
                    var font=node(doc,"rFonts");attr(font,"ascii","Times New Roman");attr(font,"hAnsi","Times New Roman");rp1.appendChild(font);
                    attr(prop(rp1,"sz"),"val","14");
                    rp1.appendChild(node(doc,"b"));
                    attr(prop(rp1,"color"),"val","000856");
                    r1.appendChild(rp1);
                    var t1=node(doc,"t");t1.setTextContent("SISTEMA DE GESTIÓN");r1.appendChild(t1);
                    r1.appendChild(node(doc,"br"));
                    var t2=node(doc,"t");t2.setTextContent("DE LA CALIDAD");r1.appendChild(t2);
                    p.appendChild(r1);
                }
            }
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
        attr(edge,"val",visible?"single":"nil");attr(edge,"sz","6");attr(edge,"space","0");attr(edge,"color","000000");
    }

    static String title(ArtifactModels.Snapshot snapshot) {
        return snapshot.plan().title().replaceFirst("(?i)^Plan de Trabajo\\s*:\\s*","").replaceFirst("(?i)^Informe(?: final)?\\s*:\\s*","").strip();
    }

    static void apply(Document doc,ArtifactModels.Snapshot snapshot) {
        for(var table:elements(doc.getDocumentElement(),"tbl")) {
            var rows=children(table,"tr");if(rows.size()!=4 || children(rows.getFirst(),"tc").size()!=2) continue;
            var grids=elements(table,"gridCol");if(grids.size()!=3) continue;
            int[] widths={1837,3120,4568};
            for(int i=0;i<3;i++) attr(grids.get(i),"w",String.valueOf(widths[i]));
            var tblPr=prop(table,"tblPr");
            var tw=prop(tblPr,"tblW");attr(tw,"type","dxa");attr(tw,"w","9525");
            var jc=prop(tblPr,"jc");attr(jc,"val","center");
            // Minimum heights reproduce the official rows and still allow long names to wrap.
            int[] minimumHeights={400,280,640,380};
            for(int i=0;i<rows.size();i++) {
                var height=prop(prop(rows.get(i),"trPr"),"trHeight");
                attr(height,"val",String.valueOf(minimumHeights[i]));attr(height,"hRule","atLeast");
            }
            for(int rowIndex=0;rowIndex<rows.size();rowIndex++) {
                var cells=children(rows.get(rowIndex),"tc");
                for(int i=0;i<cells.size();i++) {
                    var cp=prop(cells.get(i),"tcPr");var width=prop(cp,"tcW");
                    attr(width,"type","dxa");
                    attr(width,"w",String.valueOf(i==0?widths[0]:cells.size()==2?7688:widths[i]));
                    var align=prop(cp,"vAlign");attr(align,"val","center");
                    if(i>0) {
                        var margins=prop(cp,"tcMar");
                        for(String side:List.of("top","bottom","left","right")) {
                            var margin=prop(margins,side);
                            attr(margin,"type","dxa");
                            attr(margin,"w",side.equals("left")||side.equals("right")?"70":"0");
                        }
                        var shade=prop(cp,"shd");
                        attr(shade,"val","clear");
                        attr(shade,"color","auto");
                        attr(shade,"fill",rowIndex==1||rowIndex==3?"D9D9D9":"FFFFFF");
                    }
                }
            }
            logo(children(rows.get(0),"tc").getFirst());
            var first=children(rows.get(0),"tc").get(1);
            setText(first,"UNIVERSIDAD TÉCNICA DE AMBATO",22,true,"000000",0,"center");

            String title=title(snapshot).toUpperCase(Locale.ROOT);
            String prefix=snapshot.plan().documentType().equals("T2")?"INFORME DE: ":"PLAN DE TRABAJO: ";
            setText(children(rows.get(1),"tc").get(1),prefix+title,20,true,"2F5496",12,null);

            var data=children(rows.get(2),"tc");
            String unitLabel="ADMINISTRATIVE".equals(snapshot.plan().institutionalUnitType())?"Unidad administrativa:":"Unidad académica:";
            setText(data.get(1),unitLabel,14,true,"000000",12,null);

            var contentLines=new ArrayList<String>();
            contentLines.add(snapshot.plan().institutionalUnit());
            if(!"ADMINISTRATIVE".equals(snapshot.plan().institutionalUnitType()) && snapshot.plan().career()!=null && !snapshot.plan().career().isBlank()) {
                String c=snapshot.plan().career().trim();
                contentLines.add(c.toLowerCase(Locale.ROOT).startsWith("carrera") ? c : "Carrera de " + c);
            }
            setCellContent(data.get(2),contentLines,15,false,"000000",0,null);

            var last=children(rows.get(3),"tc");
            setText(last.get(1),"Fecha de elaboración:",14,true,"2F5496",12,null);
            String formattedDate=snapshot.plan().preparationDate().format(SPANISH_DATE);
            setText(last.get(2),formattedDate,14,true,"2F5496",0,null);

            borders(table,rows);

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
