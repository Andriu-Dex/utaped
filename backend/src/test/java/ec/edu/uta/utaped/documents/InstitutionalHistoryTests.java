package ec.edu.uta.utaped.documents;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Element;

class InstitutionalHistoryTests {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    @Test void officialHistoryHasWhiteExpandableDataCellsAndKeepsTheGreyHeader() throws Exception {
        for(String name:new String[]{"t1-reference.docx","t2-reference.docx"}) {
            try(var zip=new ZipInputStream(new ClassPathResource("document-templates/"+name).getInputStream())) {
                for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()) if(entry.getName().equals("word/document.xml")) {
                    var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
                    var document=factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(zip.readAllBytes()));
                    var tables=document.getElementsByTagNameNS(W,"tbl");var history=(Element)tables.item(tables.getLength()-1);
                    InstitutionalHistory.fill(history,"1.0",InstitutionalHistory.description("plan de trabajo","Unidad de Titulación"),LocalDate.of(2026,8,7));
                    var rows=history.getElementsByTagNameNS(W,"tr");assertEquals(2,rows.getLength());
                    var header=(Element)rows.item(0);assertEquals("D9D9D9",((Element)header.getElementsByTagNameNS(W,"shd").item(0)).getAttributeNS(W,"fill"));
                    var body=(Element)rows.item(1);var height=(Element)body.getElementsByTagNameNS(W,"trHeight").item(0);
                    assertEquals("704",height.getAttributeNS(W,"val"));assertEquals("atLeast",height.getAttributeNS(W,"hRule"));
                    var cells=body.getElementsByTagNameNS(W,"tc");assertEquals(3,cells.getLength());
                    String[] expected={"1.0","Elaboración del plan de trabajo de la Unidad de Titulación","07/08/2026"};
                    for(int i=0;i<3;i++) {
                        var cell=(Element)cells.item(i);assertEquals(expected[i],cell.getTextContent());
                        var shade=(Element)cell.getElementsByTagNameNS(W,"shd").item(0);assertEquals("FFFFFF",shade.getAttributeNS(W,"fill"));assertFalse(shade.hasAttributeNS(W,"themeFill"));
                        assertEquals("center",((Element)cell.getElementsByTagNameNS(W,"vAlign").item(0)).getAttributeNS(W,"val"));
                        assertEquals("center",((Element)cell.getElementsByTagNameNS(W,"jc").item(0)).getAttributeNS(W,"val"));
                        assertEquals(i==0?1:0,cell.getElementsByTagNameNS(W,"b").getLength());
                    }
                    break;
                }
            }
        }
    }
}
