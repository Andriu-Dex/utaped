package ec.edu.uta.utaped.documents;

import java.util.*;
import org.w3c.dom.*;

/** Explicit source grid edges avoid converter-dependent table style inheritance. */
final class InstitutionalTables {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private InstitutionalTables() {}
    private static List<Element> children(Element parent,String name) {
        var result=new ArrayList<Element>();
        for(Node n=parent.getFirstChild();n!=null;n=n.getNextSibling()) if(n instanceof Element e&&name.equals(e.getLocalName())) result.add(e);
        return result;
    }
    private static Element prop(Element parent,String name) {
        return children(parent,name).stream().findFirst().orElseGet(()->{var e=parent.getOwnerDocument().createElementNS(W,"w:"+name);parent.insertBefore(e,parent.getFirstChild());return e;});
    }
    private static void attr(Element e,String key,String value) { e.setAttributeNS(W,"w:"+key,value); }
    private static void border(Element borders,String side,boolean visible) {
        var edge=prop(borders,side);attr(edge,"val",visible?"single":"nil");attr(edge,"sz","6");attr(edge,"color","000000");attr(edge,"space","0");
    }
    static void apply(Element table) {
        var rows=children(table,"tr");var properties=prop(table,"tblPr");
        var borders=prop(properties,"tblBorders");
        for(String side:List.of("top","left","bottom","right","insideH","insideV")) border(borders,side,true);
        // Absolute widths use the original grid, including horizontal spans, instead of pct conversion.
        var grid=children(prop(table,"tblGrid"),"gridCol");
        if(grid.isEmpty()) return;
        int[] widths=grid.stream().mapToInt(e->Integer.parseInt(e.getAttributeNS(W,"w"))).toArray();
        var width=prop(properties,"tblW");attr(width,"type","dxa");attr(width,"w",String.valueOf(Arrays.stream(widths).sum()));
        attr(prop(properties,"tblLayout"),"type","fixed");
        for(int rowIndex=0;rowIndex<rows.size();rowIndex++) {
            int column=0;
            for(var cell:children(rows.get(rowIndex),"tc")) {
                var cp=prop(cell,"tcPr");var spans=children(cp,"gridSpan");int span=spans.isEmpty()?1:Integer.parseInt(spans.getFirst().getAttributeNS(W,"val"));
                int cellWidth=0;for(int c=column;c<Math.min(column+span,widths.length);c++)cellWidth+=widths[c];
                var cw=prop(cp,"tcW");attr(cw,"type","dxa");attr(cw,"w",String.valueOf(cellWidth));
                var merge=children(cp,"vMerge");boolean continuation=!merge.isEmpty()&&!"restart".equals(merge.getFirst().getAttributeNS(W,"val"));
                boolean continued=false;
                if(!merge.isEmpty()&&rowIndex+1<rows.size()) {
                    int nextColumn=0;
                    for(var next:children(rows.get(rowIndex+1),"tc")) {
                        var np=prop(next,"tcPr");var nm=children(np,"vMerge");
                        if(nextColumn==column) { continued=!nm.isEmpty()&&!"restart".equals(nm.getFirst().getAttributeNS(W,"val"));break; }
                        var ns=children(np,"gridSpan");nextColumn+=ns.isEmpty()?1:Integer.parseInt(ns.getFirst().getAttributeNS(W,"val"));
                    }
                }
                var cb=prop(cp,"tcBorders");border(cb,"left",true);border(cb,"right",true);border(cb,"top",!continuation);border(cb,"bottom",!continued);
                column+=span;
            }
        }
    }
}
