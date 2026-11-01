package ec.edu.uta.utaped.documents;

import java.io.*;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class AttachmentImages {
    record Converted(byte[] pdf,String mediaType) {}
    static Converted convert(byte[] bytes) {
        try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw invalid();
            var reader=readers.next();
            try {
                String format=reader.getFormatName().toUpperCase(Locale.ROOT);
                if(!format.equals("PNG") && !format.equals("JPEG")) throw invalid();
                reader.setInput(input,true,true);
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1 || height<1 || width>10000 || height>10000 || (long)width*height>16000000) throw invalid();
                var image=reader.read(0);
                try(var document=new PDDocument();var output=new ByteArrayOutputStream()) {
                    var page=new PDPage(PDRectangle.A4);document.addPage(page);
                    var embedded=format.equals("JPEG")?JPEGFactory.createFromImage(document,image,.92f):LosslessFactory.createFromImage(document,image);
                    float scale=Math.min((PDRectangle.A4.getWidth()-72)/width,(PDRectangle.A4.getHeight()-72)/height);
                    float drawnWidth=width*scale,drawnHeight=height*scale;
                    try(var content=new PDPageContentStream(document,page)) { content.drawImage(embedded,(PDRectangle.A4.getWidth()-drawnWidth)/2,(PDRectangle.A4.getHeight()-drawnHeight)/2,drawnWidth,drawnHeight); }
                    document.save(output);return new Converted(output.toByteArray(),format.equals("PNG")?"image/png":"image/jpeg");
                } finally { image.flush(); }
            } finally { reader.dispose(); }
        } catch(IOException | IllegalArgumentException e) { throw invalid(); }
    }
    private static ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un PDF o una imagen PNG/JPEG legible de hasta 16 megapíxeles."); }
}
