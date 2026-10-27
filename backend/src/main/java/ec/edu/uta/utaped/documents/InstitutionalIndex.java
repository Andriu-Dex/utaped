package ec.edu.uta.utaped.documents;

import java.util.*;
import org.w3c.dom.*;

/** Real tab leaders and section destinations; no estimated or fixed PDF page numbers. */
final class InstitutionalIndex {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private InstitutionalIndex() {}
    static boolean entry(Element p) { return p.getAttribute("data-utaped-index").equals("true"); }
    static String target(Element p) { return p.getAttribute("data-utaped-target"); }
    private static Element node(Document d,String name) {return d.createElementNS(W,"w:"+name);}
    private static void attr(Element e,String name,String value) {e.setAttributeNS(W,"w:"+name,value);}
    static Element entry(Element prototype,String label,String section,Map<String,Integer> pages) {
        var doc=prototype.getOwnerDocument();var p=node(doc,"p");
        // Private DOM attributes are removed before serialization; used only while arranging sections.
        p.setAttribute("data-utaped-index","true");p.setAttribute("data-utaped-target",section);
        var pp=node(doc,"pPr");p.appendChild(pp);
        var spacing=node(doc,"spacing");attr(spacing,"after","120");attr(spacing,"line","240");attr(spacing,"lineRule","auto");pp.appendChild(spacing);
        var tabs=node(doc,"tabs");var tab=node(doc,"tab");attr(tab,"val","right");attr(tab,"leader","dot");attr(tab,"pos","8504");tabs.appendChild(tab);pp.appendChild(tabs);
        var link=node(doc,"hyperlink");attr(link,"anchor",T1Template.anchor(section));p.appendChild(link);
        var run=node(doc,"r");var rp=node(doc,"rPr");var font=node(doc,"rFonts");attr(font,"ascii","Helvetica");attr(font,"hAnsi","Helvetica");rp.appendChild(font);var size=node(doc,"sz");attr(size,"val","20");rp.appendChild(size);run.appendChild(rp);
        var text=node(doc,"t");text.setTextContent(label);run.appendChild(text);run.appendChild(node(doc,"tab"));
        var page=node(doc,"t");page.setTextContent(pages.containsKey(section)?String.valueOf(pages.get(section)):" ");run.appendChild(page);link.appendChild(run);return p;
    }
    static void finish(Document doc) {
        var paragraphs=doc.getElementsByTagNameNS(W,"p");
        int bookmark=4000;
        for(int i=0;i<paragraphs.getLength();i++) {
            var p=(Element)paragraphs.item(i);var text=new StringBuilder();var nodes=p.getElementsByTagNameNS(W,"t");
            for(int n=0;n<nodes.getLength();n++)text.append(nodes.item(n).getTextContent());
            if(!entry(p)&&List.of("ÍNDICE DE CONTENIDO","ÍNDICE DE TABLAS").contains(text.toString().strip())) {
                var start=node(doc,"bookmarkStart");attr(start,"id",String.valueOf(bookmark));attr(start,"name",T1Template.anchor(text.toString().strip()));
                Node position=p.getFirstChild();if(position instanceof Element e&&"pPr".equals(e.getLocalName()))position=position.getNextSibling();
                p.insertBefore(start,position);var end=node(doc,"bookmarkEnd");attr(end,"id",String.valueOf(bookmark++));p.appendChild(end);
            }
            p.removeAttribute("data-utaped-index");p.removeAttribute("data-utaped-target");
        }
    }
}
