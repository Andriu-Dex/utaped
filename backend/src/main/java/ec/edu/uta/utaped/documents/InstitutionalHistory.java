package ec.edu.uta.utaped.documents;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.w3c.dom.*;

/** Three-column history grid from the official filled example, without header styling in its data row. */
final class InstitutionalHistory {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private InstitutionalHistory() {}
    private static List<Element> children(Element parent,String tag) {
        var result=new ArrayList<Element>();
        for(Node n=parent.getFirstChild();n!=null;n=n.getNextSibling()) if(n instanceof Element e&&tag.equals(e.getLocalName())) result.add(e);
        return result;
    }
    private static Element prop(Element parent,String tag) {
        return children(parent,tag).stream().findFirst().orElseGet(()->{var n=parent.getOwnerDocument().createElementNS(W,"w:"+tag);parent.insertBefore(n,parent.getFirstChild());return n;});
    }
    private static void attr(Element e,String name,String value) { e.setAttributeNS(W,"w:"+name,value); }
    static String description(String document,String group) {
        if(group==null||group.isBlank()) return "Elaboración del "+document;
        String name=group.strip(),lower=name.toLowerCase(Locale.ROOT);
        String link=List.of("unidad","comisión","facultad","carrera").stream().anyMatch(lower::startsWith)?" de la ":
            List.of("departamento","centro","comité","consejo","grupo").stream().anyMatch(lower::startsWith)?" del ":" de ";
        return "Elaboración del "+document+link+name;
    }
    static void fill(Element table,String version,String description,LocalDate date) {
        var rows=children(table,"tr");var header=rows.getFirst();
        for(int i=1;i<rows.size();i++) table.removeChild(rows.get(i));
        var entry=(Element)header.cloneNode(true);
        var rowProperties=prop(entry,"trPr");
        for(var marker:children(rowProperties,"tblHeader")) rowProperties.removeChild(marker);
        var height=prop(rowProperties,"trHeight");attr(height,"val","704");attr(height,"hRule","atLeast");
        prop(rowProperties,"cantSplit");
        var values=List.of(version,description,date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        var cells=children(entry,"tc");
        for(int i=0;i<cells.size();i++) {
            var cell=cells.get(i);var cp=prop(cell,"tcPr");
            var shade=prop(cp,"shd");while(shade.getAttributes().getLength()>0) shade.removeAttributeNode((Attr)shade.getAttributes().item(0));
            attr(shade,"val","clear");attr(shade,"fill","FFFFFF");attr(prop(cp,"vAlign"),"val","center");
            var margins=prop(cp,"tcMar");
            for(String side:List.of("top","bottom")) {
                var margin=prop(margins,side);attr(margin,"type","dxa");attr(margin,"w","60"); // 3 pt
            }
            for(var paragraph:children(cell,"p")) cell.removeChild(paragraph);
            var doc=cell.getOwnerDocument();var p=doc.createElementNS(W,"w:p");var pp=prop(p,"pPr");
            attr(prop(pp,"jc"),"val","center");var spacing=prop(pp,"spacing");attr(spacing,"before","0");attr(spacing,"after","0");attr(spacing,"line","240");attr(spacing,"lineRule","auto");
            var run=doc.createElementNS(W,"w:r");var rp=prop(run,"rPr");var font=prop(rp,"rFonts");attr(font,"ascii","Helvetica");attr(font,"hAnsi","Helvetica");attr(prop(rp,"sz"),"val","20");
            if(i==0) prop(rp,"b");
            var text=doc.createElementNS(W,"w:t");text.setTextContent(values.get(i));run.appendChild(text);p.appendChild(run);cell.appendChild(p);
        }
        table.appendChild(entry);
    }
}
