package ec.edu.uta.utaped.documents;

import static ec.edu.uta.utaped.documents.ArtifactModels.*;
import java.awt.Color;
import java.io.*;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.docx4j.Docx4J;
import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Component;

@Component
public class T1PdfEngine {
    private final T1Template template;private final T2Template reports;private final StoredFiles files;
    public T1PdfEngine(T1Template template,T2Template reports,StoredFiles files) { this.template=template;this.reports=reports;this.files=files; }
    public String version(String type) { return type.equals("T2")?reports.version():template.version(); }
    private byte[] export(byte[] docx) throws Exception {
        var pkg=WordprocessingMLPackage.load(new ByteArrayInputStream(docx));
        var mapper=new IdentityPlusMapper();var sans=PhysicalFonts.get("Liberation Sans");if(sans==null) sans=PhysicalFonts.get("DejaVu Sans");
        if(sans!=null) mapper.put("Helvetica",sans);pkg.setFontMapper(mapper);
        var out=new ByteArrayOutputStream();Docx4J.toPDF(pkg,out);return out.toByteArray();
    }
    private static int sectionPage(PDDocument pdf,String section) throws IOException {
        var destination=pdf.getDocumentCatalog().findNamedDestinationPage(new PDNamedDestination(T1Template.anchor(section)));
        if(destination!=null && destination.getPage()!=null) return pdf.getPages().indexOf(destination.getPage());
        // Fallback for converters which expose bookmark destinations as direct links only.
        var stripper=new PDFTextStripper();int found=-1;
        for(int i=0;i<pdf.getNumberOfPages();i++) {
            stripper.setStartPage(i+1);stripper.setEndPage(i+1);String text=stripper.getText(pdf);
            String heading="(?m)^\\s*(?:\\d+[.\\s]*)?"+Arrays.stream(section.split(" ")).map(java.util.regex.Pattern::quote).collect(java.util.stream.Collectors.joining("\\s+"))+"\\s*$";
            if(java.util.regex.Pattern.compile(heading).matcher(text).find()) found=i;
        }
        if(found<0) throw new IOException("No se encontró la sección generada: "+section);return found;
    }
    public Map<String,Integer> contentSections(byte[] bytes) {
        try(var pdf=Loader.loadPDF(bytes)) {
            return Map.of("JUSTIFICATION",sectionPage(pdf,"JUSTIFICACIÓN"),"OBJECTIVE",sectionPage(pdf,"OBJETIVO"));
        } catch(IOException error) { throw new IllegalStateException("No se localizaron las secciones del borrador.",error); }
    }
    public Generated generate(Snapshot snapshot) {
        try {
            Map<String,Integer> numbers=new LinkedHashMap<>();byte[] nativeBytes=null;
            var sections=snapshot.report()!=null?new ArrayList<>(reports.sections(snapshot)):new ArrayList<>(List.of("JUSTIFICACIÓN","OBJETIVO","MATRIZ DE ACTIVIDADES"));
            if(snapshot.report()==null) { if(!snapshot.attachments().isEmpty()) sections.add("ANEXOS");sections.addAll(List.of("FIRMAS DE RESPONSABILIDAD","CONTROL DE HISTORIAL DE CAMBIOS")); }
            int insertedPages=snapshot.attachments().stream().mapToInt(AttachmentModels.Attachment::pageCount).sum();
            boolean stable=false;
            for(int attempt=0;attempt<4;attempt++) {
                nativeBytes=export(snapshot.report()!=null?reports.compose(snapshot,numbers):template.compose(snapshot,numbers));var actual=new LinkedHashMap<String,Integer>();
                try(var nativePdf=Loader.loadPDF(nativeBytes)) {
                    int insertion=sectionPage(nativePdf,"FIRMAS DE RESPONSABILIDAD");
                    for(String section:sections) { int page=sectionPage(nativePdf,section);actual.put(section,page+1+(page>=insertion?insertedPages:0)); }
                }
                if(numbers.equals(actual)) { stable=true;break; }numbers=actual;
            }
            if(!stable) throw new IOException("La paginación no se estabilizó.");
            try(var nativePdf=Loader.loadPDF(nativeBytes);var composed=new PDDocument()) {
                int insertion=sectionPage(nativePdf,"FIRMAS DE RESPONSABILIDAD");
                var sources=new ArrayList<PDDocument>();var pages=new ArrayList<Page>();
                try {
                    for(int i=0;i<nativePdf.getNumberOfPages();i++) {
                        if(i==insertion) for(var attachment:snapshot.attachments()) {
                            var source=Loader.loadPDF(files.read(attachment.fileId()));sources.add(source);
                            for(var page:source.getPages()) { composed.importPage(page);pages.add(metadata(page,pages.size(),"ANNEX",attachment.id())); }
                        }
                        var page=nativePdf.getPage(i);composed.importPage(page);pages.add(metadata(page,pages.size(),snapshot.plan().documentType(),null));
                    }
                    if(pages.size()>500) throw new IOException("El documento excede el límite de páginas generadas.");
                    int signaturePage=sectionPage(composed,"FIRMAS DE RESPONSABILIDAD");
                    var stripper=new PDFTextStripper();int namePage=-1;
                    for(int p=signaturePage;p<composed.getNumberOfPages();p++) {
                        stripper.setStartPage(p+1);stripper.setEndPage(p+1);
                        if(stripper.getText(composed).replaceAll("\\s+","").contains(snapshot.plan().teacherName().replaceAll("\\s+",""))) { namePage=p;break; }
                    }
                    if(namePage<0) throw new IOException("No se encontró la fila del elaborador.");
                    var location=signatureBounds(composed,namePage);
                    var slots=new ArrayList<SignatureSlot>();
                    slots.add(new SignatureSlot("DOCENTE","ELABORADO_POR","Elaborado por",snapshot.plan().teacherId(),namePage,namePage+1,location));
                    if(snapshot.workflow()!=null) {
                        int slotNumber=1;
                        for(var stage:snapshot.workflow().stages()) if(stage.requiresSignature()) for(var person:stage.participants()) {
                            String marker="@@SIGTOP"+slotNumber+"@@";int found=-1;
                            for(int p=signaturePage;p<composed.getNumberOfPages();p++) {
                                stripper.setStartPage(p+1);stripper.setEndPage(p+1);
                                if(stripper.getText(composed).contains(marker)) { found=p;break; }
                            }
                            if(found<0) throw new IOException("No se encontró la ubicación de firma de la etapa.");
                            slots.add(new SignatureSlot("REVIEWER",DocumentWorkflowSnapshot.signatureAction(stage.action()),stage.label(),person.id(),found,found+1,signatureBounds(composed,found,String.valueOf(slotNumber))));
                            slotNumber++;
                        }
                    }
                    stampPageNumbers(composed);
                    var out=new ByteArrayOutputStream();composed.save(out);
                    return new Generated(out.toByteArray(),List.copyOf(pages),List.copyOf(slots));
                } finally { for(var source:sources) source.close(); }
            }
        } catch(Exception e) { throw new IllegalStateException("No se pudo generar el documento T1.",e); }
    }
    private static Page metadata(PDPage page,int index,String kind,UUID attachmentId) { return new Page(index,index+1,page.getMediaBox().getWidth(),page.getMediaBox().getHeight(),kind,attachmentId); }
    private static class SignatureMarkers extends PDFTextStripper {
        TextPosition top,bottom,last;
        final String suffix;
        SignatureMarkers(String suffix) throws IOException { this.suffix=suffix; }
        @Override protected void writeString(String text,List<TextPosition> positions) {
            text=positions.stream().map(TextPosition::getUnicode).collect(java.util.stream.Collectors.joining());
            int first=text.indexOf("@@SIGTOP"+suffix+"@@"),end=text.indexOf("@@SIGBOTTOM"+suffix+"@@");
            if(first>=0) top=positions.get(first);
            if(end>=0) { bottom=positions.get(end);last=positions.get(Math.min(end+12+suffix.length(),positions.size()-1)); }
        }
    }
    private static Bounds signatureBounds(PDDocument pdf,int index) throws IOException {
        return signatureBounds(pdf,index,"");
    }
    private static Bounds signatureBounds(PDDocument pdf,int index,String suffix) throws IOException {
        var marker=new SignatureMarkers(suffix);marker.setStartPage(index+1);marker.setEndPage(index+1);marker.getText(pdf);
        if(marker.top==null || marker.bottom==null) throw new IOException("Signature bounds not found.");
        float x=marker.top.getXDirAdj(),y=pdf.getPage(index).getMediaBox().getHeight()-marker.bottom.getYDirAdj();
        float width=marker.last.getXDirAdj()+marker.last.getWidthDirAdj()-x,height=marker.bottom.getYDirAdj()-marker.top.getYDirAdj();
        if(width<60 || height<30) throw new IOException("Signature bounds too small.");
        return new Bounds(x+1,y+1,width-2,height-2);
    }
    private static class NumberMarker extends PDFTextStripper {
        final List<TextPosition> positions=new ArrayList<>();
        NumberMarker() throws IOException {}
        @Override protected void writeString(String text,List<TextPosition> positions) throws IOException {
            text=positions.stream().map(TextPosition::getUnicode).collect(java.util.stream.Collectors.joining());
            int start=text.indexOf("@@PN@@");if(start>=0) this.positions.addAll(positions.subList(start,Math.min(start+6,positions.size())));
        }
    }
    private static void stampPageNumbers(PDDocument pdf) throws IOException {
        for(int i=0;i<pdf.getNumberOfPages();i++) {
            var marker=new NumberMarker();marker.setStartPage(i+1);marker.setEndPage(i+1);marker.getText(pdf);
            // Remove composition markers on every page, including pages without a page-number marker.
            var page=pdf.getPage(i);var parser=new PDFStreamParser(page);var tokens=parser.parse();parser.close();
            for(var token:tokens) {
                if(token instanceof COSString value) clearMarker(value);
                if(token instanceof COSArray values) for(var value:values) if(value instanceof COSString string) clearMarker(string);
            }
            var rewritten=new PDStream(pdf);try(var output=rewritten.createOutputStream(COSName.FLATE_DECODE)) { new ContentStreamWriter(output).writeTokens(tokens); }page.setContents(rewritten);
            if(marker.positions.isEmpty()) continue;
            var first=marker.positions.getFirst();var last=marker.positions.getLast();float x=first.getXDirAdj(),y=page.getMediaBox().getHeight()-first.getYDirAdj();
            try(var stream=new PDPageContentStream(pdf,page,PDPageContentStream.AppendMode.APPEND,true,true)) {
                stream.setNonStrokingColor(Color.BLACK);stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),Math.max(8,first.getFontSizeInPt()));stream.newLineAtOffset(x,y);stream.showText(String.valueOf(i+1));stream.endText();
            }
        }
    }
    private static void clearMarker(COSString value) {
        byte[] bytes=value.getBytes();String text=new String(bytes,java.nio.charset.StandardCharsets.ISO_8859_1);
        if(text.contains("@@")) value.setValue(text.replace("@@PN@@","").replaceAll("@@SIG(?:TOP|BOTTOM)[0-9]*@@","").getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
    }
}
