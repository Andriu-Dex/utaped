package ec.edu.uta.utaped.documents;

import java.io.IOException;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PdfValidation {
    private final int maxPages;
    public PdfValidation(@Value("${app.documents.max-attachment-pages:100}") int maxPages) { this.maxPages=maxPages; }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un PDF legible, sin cifrado ni contenido activo y dentro del límite de páginas."); }
    public int pages(byte[] bytes) {
        if(bytes.length<5 || bytes[0]!='%' || bytes[1]!='P' || bytes[2]!='D' || bytes[3]!='F' || bytes[4]!='-') throw invalid();
        try(PDDocument pdf=Loader.loadPDF(bytes)) {
            if(pdf.isEncrypted() || pdf.getNumberOfPages()<1 || pdf.getNumberOfPages()>maxPages) throw invalid();
            inspect(pdf.getDocumentCatalog().getCOSObject());
            var catalog=pdf.getDocumentCatalog().getCOSObject();
            if(catalog.containsKey(COSName.OPEN_ACTION) || catalog.containsKey(COSName.AA)) throw invalid();
            var names=pdf.getDocumentCatalog().getNames();
            if(names!=null && (names.getJavaScript()!=null || names.getEmbeddedFiles()!=null)) throw invalid();
            for(var page:pdf.getPages()) {
                var box=page.getMediaBox();if(!Float.isFinite(box.getWidth()) || !Float.isFinite(box.getHeight()) || box.getWidth()<1 || box.getHeight()<1 || box.getWidth()>14400 || box.getHeight()>14400) throw invalid();
                if(page.getCOSObject().containsKey(COSName.AA)) throw invalid();
                for(var annotation:page.getAnnotations()) {
                    if(annotation.getCOSObject().containsKey(COSName.AA)) throw invalid();
                    var action=annotation.getCOSObject().getCOSDictionary(COSName.A);
                    if(action!=null && !COSName.URI.equals(action.getCOSName(COSName.S)) && !COSName.getPDFName("GoTo").equals(action.getCOSName(COSName.S))) throw invalid();
                }
            }
            if(pdf.getDocumentCatalog().getAcroForm()!=null && pdf.getDocumentCatalog().getAcroForm().hasXFA()) throw invalid();
            return pdf.getNumberOfPages();
        } catch(IOException e) { throw invalid(); }
    }
    private void inspect(COSBase root) {
        Set<COSBase> visited=Collections.newSetFromMap(new IdentityHashMap<>());var pending=new ArrayDeque<COSBase>();pending.add(root);
        var forbidden=Set.of("JavaScript","Launch","SubmitForm","ImportData","GoToR","GoToE","Rendition","Movie","Sound","ResetForm");
        while(!pending.isEmpty()) {
            var value=pending.removeFirst();if(!visited.add(value)) continue;if(visited.size()>200000) throw invalid();
            if(value instanceof COSObject object) { if(object.getObject()!=null) pending.add(object.getObject()); }
            else if(value instanceof COSArray array) for(var child:array) { if(child!=null) pending.add(child); }
            else if(value instanceof COSDictionary dict) {
                if(dict.containsKey(COSName.AA) || dict.containsKey(COSName.JS) || forbidden.contains(dict.getNameAsString(COSName.S,""))) throw invalid();
                for(var key:dict.keySet()) { var child=dict.getItem(key);if(child!=null) pending.add(child); }
            }
        }
    }
}
