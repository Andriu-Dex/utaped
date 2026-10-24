package ec.edu.uta.utaped.documents;

import static ec.edu.uta.utaped.documents.T2Template.*;
import java.util.*;
import org.w3c.dom.*;
import ec.edu.uta.utaped.planning.DocumentStructures;

/** Applies only explicitly published structural versions; historical/default packages retain their original layout. */
final class StructuredSections {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    static boolean changed(ArtifactModels.Snapshot s) { return s.structure()!=null&&s.structure().revision()>0; }
    static List<String> sections(ArtifactModels.Snapshot s,List<String> original) {
        if(!changed(s)) return original;
        return s.structure().sections().stream().filter(section->original.contains(section.key())||section.key().startsWith("CUSTOM_")).map(DocumentStructures.Section::label).toList();
    }
    private static boolean isContentIndex(Element p) {
        String target=InstitutionalIndex.target(p);
        String text=all(p,"t").stream().map(Node::getTextContent).collect(java.util.stream.Collectors.joining());
        return (InstitutionalIndex.entry(p)||text.contains("...."))&&!text.startsWith("Tabla ")&&!target.startsWith("ÍNDICE");
    }
    static void apply(Document doc,ArtifactModels.Snapshot s,Map<String,Integer> numbers) {
        if(!changed(s))return;
        var body=(Element)doc.getElementsByTagNameNS(W,"body").item(0);var nodes=children(body,null);
        var groups=new LinkedHashMap<String,List<Element>>();String current=null;var prefix=new ArrayList<Element>();Element finalSection=null;
        var nativeKeys=DocumentStructures.nativeSections(s.plan().documentType());
        for(var n:nodes) {
            if(n.getLocalName().equals("sectPr")) { finalSection=n;continue; }
            String found=null;
            for(var key:nativeKeys) if(all(n,"bookmarkStart").stream().anyMatch(b->b.getAttributeNS(W,"name").equals(T1Template.anchor(key)))) { found=key;break; }
            if(found!=null) { current=found;groups.put(current,new ArrayList<>()); }
            if(current==null)prefix.add(n);else groups.get(current).add(n);
        }
        if(groups.isEmpty()||finalSection==null)throw new IllegalStateException("No se identificó la estructura institucional.");
        var heading=groups.values().iterator().next().getFirst();Element plain=null;
        for(var block:groups.values()) for(var n:block) if(n.getLocalName().equals("p")&&all(n,"bookmarkStart").isEmpty()&&!all(n,"t").isEmpty()) {plain=n;break;}
        if(plain==null)plain=heading;
        var ordered=new LinkedHashMap<String,List<Element>>();
        for(var section:s.structure().sections()) {
            List<Element> block=groups.get(section.key());
            if(block==null&&section.key().startsWith("CUSTOM_")) {
                block=new ArrayList<>();var h=paragraph(heading,section.label());removeAll(h,"pageBreakBefore");removeAll(h,"keepNext");property(h,"keepNext");block.add(h);
                for(String line:s.structure().contents().getOrDefault(section.key(),section.initialText()).split("\\R",-1))block.add(paragraph(plain,line.isBlank()?"No aplica.":line));
            }
            if(block!=null) ordered.put(section.label(),block);
        }
        // Rebuild the index with the configured order and the engine's real page numbers.
        Element indexPrototype=null;int insert=-1;
        for(int i=0;i<prefix.size();i++) if(isContentIndex(prefix.get(i))) { if(indexPrototype==null) {indexPrototype=prefix.get(i);insert=i;} }
        if(indexPrototype!=null) {
            prefix.removeIf(StructuredSections::isContentIndex);
            int offset=0;for(var label:ordered.keySet()) {prefix.add(insert+offset,InstitutionalIndex.entry(indexPrototype,(offset+1)+". "+label,label,numbers));offset++;}
        }
        // A renamed published section also changes the table index destination, not its caption.
        for(int i=0;i<prefix.size();i++) {
            var p=prefix.get(i);
            if(!InstitutionalIndex.entry(p)||all(p,"t").isEmpty()||!all(p,"t").getFirst().getTextContent().startsWith("Tabla "))continue;
            String target=InstitutionalIndex.target(p);
            var section=s.structure().sections().stream().filter(item->item.key().equals(target)).findFirst();
            if(section.isPresent())prefix.set(i,InstitutionalIndex.entry(p,all(p,"t").getFirst().getTextContent(),section.get().label(),numbers));
        }
        for(var n:children(body,null))body.removeChild(n);for(var n:prefix)body.appendChild(n);
        int ordinal=1,bookmark=2000,index=0;
        for(var item:ordered.entrySet()) {
            var block=item.getValue();var h=block.getFirst();text(h,ordinal+++". "+item.getKey());
            for(var n:block) { removeAll(n,"bookmarkStart");removeAll(n,"bookmarkEnd");removeAll(n,"sectPr"); }
            removeAll(h,"numPr");var numbering=property(h,"numPr");var noNumber=element(doc,"numId");noNumber.setAttributeNS(W,"w:val","0");numbering.appendChild(noNumber);
            removeAll(h,"pageBreakBefore");if(index==0||item.getKey().equals("FIRMAS DE RESPONSABILIDAD"))property(h,"pageBreakBefore");
            var start=element(doc,"bookmarkStart");start.setAttributeNS(W,"w:id",String.valueOf(bookmark));start.setAttributeNS(W,"w:name",T1Template.anchor(item.getKey()));h.insertBefore(start,children(h,"r").getFirst());
            var end=element(doc,"bookmarkEnd");end.setAttributeNS(W,"w:id",String.valueOf(bookmark++));h.appendChild(end);
            for(var n:block)body.appendChild(n);
            // Explicit section boundaries keep a moved matrix A4 landscape and the narrative A4 portrait.
            if(++index<ordered.size()) {
                var p=element(doc,"p");var pp=element(doc,"pPr");var sp=(Element)finalSection.cloneNode(true);var sizes=all(sp,"pgSz");
                boolean wide=s.plan().documentType().equals("T1")&&item.getKey().equals("MATRIZ DE ACTIVIDADES");
                for(var size:sizes) {size.setAttributeNS(W,"w:w",wide?"16838":"11906");size.setAttributeNS(W,"w:h",wide?"11906":"16838");size.setAttributeNS(W,"w:orient",wide?"landscape":"portrait");}
                var type=children(sp,"type").stream().findFirst().orElseGet(()->{var e=element(doc,"type");sp.insertBefore(e,sp.getFirstChild());return e;});type.setAttributeNS(W,"w:val","continuous");pp.appendChild(sp);p.appendChild(pp);body.appendChild(p);
            }
        }
        body.appendChild(finalSection);
    }
}
