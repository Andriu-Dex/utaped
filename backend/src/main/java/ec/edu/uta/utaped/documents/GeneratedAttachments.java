package ec.edu.uta.utaped.documents;

import java.io.*;
import java.net.URI;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

final class GeneratedAttachments {
    record Upload(String name,byte[] bytes) implements MultipartFile {
        public String getName() { return "file"; } public String getOriginalFilename() { return name; }
        public String getContentType() { return "application/pdf"; } public boolean isEmpty() { return bytes.length==0; }
        public long getSize() { return bytes.length; } public byte[] getBytes() { return bytes; }
        public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        public void transferTo(java.io.File target) throws IOException { java.nio.file.Files.write(target.toPath(),bytes); }
    }
    static byte[] render(AttachmentModels.GeneratedInput input) {
        if(input.kind().equals("URL")) {
            try { var uri=new URI(input.url());if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||input.url().chars().anyMatch(c->Character.isWhitespace(c)||Character.isISOControl(c))) throw new IllegalArgumentException(); }
            catch(Exception error) { throw invalid("Escriba un enlace HTTPS válido, sin usuario ni contraseña."); }
        } else {
            if(input.rows()==null || input.rows().size()<2 || input.rows().size()>100) throw invalid("La tabla requiere encabezados y al menos una fila (máximo 100 filas).");
            int columns=input.rows().getFirst().size();
            if(columns<1 || columns>8 || input.rows().stream().anyMatch(row->row.size()!=columns) || input.rows().getFirst().stream().anyMatch(value->value.isBlank() || value.length()>80) || input.rows().stream().flatMap(List::stream).anyMatch(value->value.length()>200) || input.rows().stream().skip(1).allMatch(row->row.stream().allMatch(String::isBlank))) throw invalid("Complete los encabezados y datos de una tabla rectangular de hasta ocho columnas.");
        }
        try(var document=new PDDocument();var output=new ByteArrayOutputStream()) {
            var writer=new Writer(document,input.title());
            if(input.kind().equals("URL")) writer.link(input.url());else writer.table(input.rows());
            writer.close();document.save(output);return output.toByteArray();
        } catch(IOException | IllegalArgumentException error) { throw invalid("No se pudo generar el anexo. Revise el texto y sus caracteres."); }
    }
    private static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private static final class Writer implements AutoCloseable {
        final PDDocument document;final String title;final PDFont regular=new PDType1Font(Standard14Fonts.FontName.HELVETICA),bold=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDPage page;PDPageContentStream content;float y;final float left=36,width=PDRectangle.A4.getWidth()-72;
        Writer(PDDocument document,String title) throws IOException { this.document=document;this.title=title;newPage(); }
        void newPage() throws IOException {
            if(content!=null) content.close();page=new PDPage(PDRectangle.A4);document.addPage(page);content=new PDPageContentStream(document,page);y=PDRectangle.A4.getHeight()-40;
            for(var line:wrap(title,bold,12,width)) { text(line,left,y,bold,12);y-=16; }y-=14;
        }
        List<String> wrap(String value,PDFont font,float size,float available) throws IOException {
            var lines=new ArrayList<String>();StringBuilder line=new StringBuilder();
            for(int point:value.replaceAll("\\s+"," ").codePoints().toArray()) {
                String next=new String(Character.toChars(point));
                if(font.getStringWidth(line.toString()+next)*size/1000>available && !line.isEmpty()) { lines.add(line.toString());line.setLength(0); }
                line.append(next);
            }
            lines.add(line.toString());return lines;
        }
        void text(String value,float x,float y,PDFont font,float size) throws IOException { content.setNonStrokingColor(java.awt.Color.BLACK);content.beginText();content.setFont(font,size);content.newLineAtOffset(x,y);content.showText(value);content.endText(); }
        void link(String url) throws IOException {
            text("Referencia externa",left,y,bold,10);y-=20;
            for(var line:wrap(url,regular,10,width)) { text(line,left,y,regular,10);var link=new PDAnnotationLink();link.setRectangle(new PDRectangle(left,y-3,width,14));var action=new PDActionURI();action.setURI(url);link.setAction(action);page.getAnnotations().add(link);y-=15; }
        }
        List<List<String>> cells(List<String> row,boolean header) throws IOException { var result=new ArrayList<List<String>>();for(var value:row) result.add(wrap(value,header?bold:regular,8.5f,width/row.size()-12));return result; }
        float height(List<List<String>> cells) { return cells.stream().mapToInt(List::size).max().orElse(1)*11+14; }
        void row(List<List<String>> cells,boolean header) throws IOException {
            float h=height(cells),cellWidth=width/cells.size();
            if(header) { content.setNonStrokingColor(new java.awt.Color(225,225,225));content.addRect(left,y-h,width,h);content.fill(); }
            content.setStrokingColor(java.awt.Color.BLACK);content.setLineWidth(.5f);
            for(int column=0;column<cells.size();column++) { float x=left+column*cellWidth;content.addRect(x,y-h,cellWidth,h);content.stroke();float baseline=y-14;for(var line:cells.get(column)) { text(line,x+6,baseline,header?bold:regular,8.5f);baseline-=11; } }
            y-=h;
        }
        void table(List<List<String>> rows) throws IOException {
            var header=cells(rows.getFirst(),true);row(header,true);
            for(var source:rows.subList(1,rows.size())) { var cells=cells(source,false);if(y-height(cells)<36) { newPage();row(header,true); }row(cells,false); }
        }
        public void close() throws IOException { content.close(); }
    }
}
