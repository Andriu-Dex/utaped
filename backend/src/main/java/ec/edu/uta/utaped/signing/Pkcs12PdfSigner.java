package ec.edu.uta.utaped.signing;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;
import javax.security.auth.DestroyFailedException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import ec.edu.uta.utaped.documents.ArtifactModels.SignatureSlot;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.*;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.*;

/** Cryptographic core; transport, persistence and policy selection belong to the application service. */
public final class Pkcs12PdfSigner {
    private static final Provider PROVIDER=new BouncyCastleProvider();
    private static final int MAX_CONTAINER_BYTES=1024*1024,MAX_PDF_BYTES=64*1024*1024;
    public static final class SigningException extends RuntimeException {
        SigningException(String message) { super(message); }
    }

    /** Consumes and clears caller-owned container/password arrays, including on failure. */
    public byte[] sign(byte[] original,byte[] container,char[] password,String boundFingerprint,
        Set<TrustAnchor> anchors,Instant now) {
        return sign(original,container,password,boundFingerprint,anchors,now,null,null,false,false);
    }
    /** Direct trust in an administratively bound public certificate, without claiming CA/revocation validation. */
    public byte[] signPinned(byte[] original,byte[] container,char[] password,String boundFingerprint,
        Instant now,SignatureSlot slot,String signerName) {
        return sign(original,container,password,boundFingerprint,Set.of(),now,slot,signerName,true,false);
    }
    /** Adds a signature only to an intact, fully signed previous revision held by the application. */
    public byte[] signPinnedAdditional(byte[] original,byte[] container,char[] password,String boundFingerprint,Instant now,SignatureSlot slot,String signerName) {
        return sign(original,container,password,boundFingerprint,Set.of(),now,slot,signerName,true,true);
    }
    private byte[] sign(byte[] original,byte[] container,char[] password,String boundFingerprint,
        Set<TrustAnchor> anchors,Instant now,SignatureSlot slot,String signerName,boolean pinned,boolean additional) {
        PrivateKey key=null;
        try {
            if(original==null || original.length==0 || original.length>MAX_PDF_BYTES) fail("PDF fuera del límite técnico de firma.");
            if(container==null || container.length==0 || container.length>MAX_CONTAINER_BYTES || password==null || password.length==0)
                fail("Archivo o contraseña de firma no válidos.");
            if(!pinned && anchors==null) fail("Falta configuración de confianza.");
            requirePolicy(boundFingerprint,pinned?null:anchors,now);
            var store=KeyStore.getInstance("PKCS12");store.load(new ByteArrayInputStream(container),password);
            var aliases=Collections.list(store.aliases()).stream().filter(alias->{
                try { return store.isKeyEntry(alias); } catch(KeyStoreException e) { throw new SigningException("No se pudo leer el contenedor de firma."); }
            }).toList();
            if(aliases.size()!=1) fail("El contenedor debe incluir exactamente una clave de firma.");
            key=(PrivateKey)store.getKey(aliases.getFirst(),password);
            var certificates=store.getCertificateChain(aliases.getFirst());
            if(key==null || certificates==null || certificates.length==0) fail("El contenedor no tiene certificado y clave de firma.");
            var chain=Arrays.stream(certificates).map(c->(X509Certificate)c).toList();
            validateCertificate(chain.getFirst(),chain,boundFingerprint,pinned?null:anchors,now);
            String algorithm=algorithm(key,chain.getFirst());
            try(var pdf=Loader.loadPDF(original);var options=new SignatureOptions()) {
                if(pdf.isEncrypted() || pdf.getNumberOfPages()==0 || (!additional && !pdf.getSignatureDictionaries().isEmpty()))
                    fail("El núcleo inicial requiere un PDF sin cifrado ni firmas previas.");
                if(additional) verifyPrevious(pdf,original);
                var signature=new PDSignature();signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
                signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
                var calendar=GregorianCalendar.from(now.atZone(java.time.ZoneOffset.UTC));signature.setSignDate(calendar);
                options.setPreferredSignatureSize(32768);
                pdf.addSignature(signature,options);
                if(slot!=null) appearance(pdf,signature,slot,signerName,now);
                var out=new ByteArrayOutputStream();var external=pdf.saveIncrementalForExternalSigning(out);
                var generator=new CMSSignedDataGenerator();
                var attributes=new AttributeTable(new Attribute(CMSAttributes.signingTime,new DERSet(new Time(Date.from(now)))));
                generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(PROVIDER).build())
                    .setSignedAttributeGenerator(new DefaultSignedAttributeTableGenerator(attributes))
                    .build(new JcaContentSignerBuilder(algorithm).setProvider(PROVIDER).build(key),chain.getFirst()));
                generator.addCertificates(new JcaCertStore(chain));
                byte[] signedContent;try(var content=external.getContent()) { signedContent=content.readAllBytes(); }
                external.setSignature(generator.generate(new CMSProcessableByteArray(signedContent),false).getEncoded());
                byte[] result=out.toByteArray();
                verifyInternal(original,result,boundFingerprint,pinned?null:anchors,now);
                return result;
            }
        } catch(SigningException e) { throw e; }
        catch(Exception e) { throw new SigningException("No se pudo firmar: revise el archivo, contraseña, certificado y política de confianza."); }
        finally {
            if(container!=null) Arrays.fill(container,(byte)0);
            if(password!=null) Arrays.fill(password,'\0');
            if(key!=null) try { key.destroy(); } catch(DestroyFailedException e) { /* JCA keys may not support destruction; no references are retained. */ }
        }
    }

    /** Verifies the new incremental signature and the cryptographic integrity of previous signatures. */
    public void verify(byte[] original,byte[] signed,String boundFingerprint,Set<TrustAnchor> anchors,Instant now) {
        if(anchors==null) fail("Falta configuración de confianza.");
        verifyInternal(original,signed,boundFingerprint,anchors,now);
    }
    private void verifyInternal(byte[] original,byte[] signed,String boundFingerprint,Set<TrustAnchor> anchors,Instant now) {
        try {
            requirePolicy(boundFingerprint,anchors,now);
            if(original==null || signed==null || original.length==0 || original.length>MAX_PDF_BYTES || signed.length>MAX_PDF_BYTES+1024*1024
                || signed.length<=original.length || Arrays.mismatch(original,0,original.length,signed,0,original.length)!=-1)
                fail("El PDF firmado no conserva los bytes originales.");
            try(var previous=Loader.loadPDF(original);var pdf=Loader.loadPDF(signed)) {
                var signatures=pdf.getSignatureDictionaries();
                if(signatures.size()!=previous.getSignatureDictionaries().size()+1) fail("Resultado de firma no válido.");
                if(!previous.getSignatureDictionaries().isEmpty()) verifyPrevious(previous,original);
                var signature=signatures.stream().max(Comparator.comparingInt(s->s.getByteRange()[2])).orElseThrow();var range=signature.getByteRange();
                if(!PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED.getName().equals(signature.getSubFilter()) || range==null || range.length!=4
                    || range[0]!=0 || range[1]<original.length || range[2]<=range[1] || range[3]<0 || (long)range[2]+range[3]!=signed.length)
                    fail("La firma no cubre la revisión completa del PDF.");
                var cms=new CMSSignedData(new CMSProcessableByteArray(signature.getSignedContent(signed)),new ByteArrayInputStream(signature.getContents(signed)));
                var signers=cms.getSignerInfos().getSigners();
                if(signers.size()!=1) fail("Resultado de firma no válido.");
                var signer=signers.iterator().next();
                var certificates=cms.getCertificates().getMatches(null);
                var matches=certificates.stream().filter(signer.getSID()::match).toList();
                if(matches.size()!=1) fail("Certificado del firmante no disponible.");
                var converter=new JcaX509CertificateConverter().setProvider(PROVIDER);
                var leaf=converter.getCertificate(matches.iterator().next());
                var chain=new ArrayList<X509Certificate>();
                for(var holder:certificates) chain.add(converter.getCertificate(holder));
                validateCertificate(leaf,chain,boundFingerprint,anchors,now);
                if(!signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(PROVIDER).build(leaf))) fail("La firma criptográfica no es válida.");
            }
        } catch(SigningException e) { throw e; }
        catch(Exception e) { throw new SigningException("No se pudo verificar la integridad y certificado del resultado firmado."); }
    }
    private static void verifyPrevious(PDDocument pdf,byte[] bytes) throws Exception {
        var signatures=pdf.getSignatureDictionaries();if(signatures.isEmpty()||signatures.size()>100) fail("La revisión previa no contiene firmas verificables.");
        long lastEnd=0;
        for(var signature:signatures) {
            var range=signature.getByteRange();
            if(range==null||range.length!=4||range[0]!=0||range[1]<=0||range[2]<=range[1]||range[3]<0||(long)range[2]+range[3]>bytes.length||!PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED.getName().equals(signature.getSubFilter())) fail("Firma previa no verificable.");
            lastEnd=Math.max(lastEnd,(long)range[2]+range[3]);
            var cms=new CMSSignedData(new CMSProcessableByteArray(signature.getSignedContent(bytes)),new ByteArrayInputStream(signature.getContents(bytes)));
            var signers=cms.getSignerInfos().getSigners();if(signers.size()!=1) fail("Firma previa no verificable.");var signer=signers.iterator().next();
            var matches=cms.getCertificates().getMatches(null).stream().filter(signer.getSID()::match).toList();if(matches.size()!=1) fail("Certificado previo no disponible.");
            var certificate=new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(matches.getFirst());
            if(!signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(PROVIDER).build(certificate))) fail("La integridad de una firma previa no es válida.");
        }
        if(lastEnd!=bytes.length) fail("El PDF tiene modificaciones posteriores a su última firma.");
    }

    private static void requirePolicy(String fingerprint,Set<TrustAnchor> anchors,Instant now) {
        if(fingerprint==null || !fingerprint.matches("[a-fA-F0-9]{64}") || (anchors!=null && anchors.isEmpty()) || now==null)
            fail("Falta vinculación de identidad o configuración de confianza.");
    }
    private static void validateCertificate(X509Certificate leaf,List<X509Certificate> chain,String fingerprint,Set<TrustAnchor> anchors,Instant now) throws Exception {
        var actual=MessageDigest.getInstance("SHA-256").digest(leaf.getEncoded());
        if(!MessageDigest.isEqual(actual,HexFormat.of().parseHex(fingerprint))) fail("El certificado no corresponde a la identidad vinculada.");
        leaf.checkValidity(Date.from(now));
        if(leaf.getBasicConstraints()!=-1) fail("Una autoridad certificadora no puede actuar como firmante personal.");
        var usage=leaf.getKeyUsage();
        if(usage!=null && !usage[0] && (usage.length<2 || !usage[1])) fail("El certificado no permite firma digital.");
        if(anchors==null) return; // Explicit pin mode: no claim of issuer-chain or revocation verification.
        var selector=new X509CertSelector();selector.setCertificate(leaf);
        var parameters=new PKIXBuilderParameters(anchors,selector);parameters.setDate(Date.from(now));
        // Offline chain check only. Revocation and institutional acceptance remain mandatory integration gates.
        parameters.setRevocationEnabled(false);
        parameters.addCertStore(CertStore.getInstance("Collection",new CollectionCertStoreParameters(chain)));
        CertPathBuilder.getInstance("PKIX").build(parameters);
    }
    private static String algorithm(PrivateKey key,X509Certificate certificate) {
        if(key instanceof RSAPrivateKey rsa && certificate.getPublicKey() instanceof RSAPublicKey publicKey
            && rsa.getModulus().bitLength()>=2048 && publicKey.getModulus().bitLength()>=2048) return "SHA256withRSA";
        if(key instanceof ECPrivateKey ec && certificate.getPublicKey() instanceof ECPublicKey publicKey
            && ec.getParams().getOrder().bitLength()>=256 && publicKey.getParams().getOrder().bitLength()>=256) return "SHA256withECDSA";
        fail("Algoritmo o tamaño de clave no admitido por el núcleo inicial.");return null;
    }
    private static void fail(String message) { throw new SigningException(message); }
    private static void appearance(PDDocument pdf,PDSignature signature,SignatureSlot slot,String name,Instant now) throws IOException {
        var b=slot.bounds();
        if(b==null || slot.pageIndex()<0 || slot.pageIndex()>=pdf.getNumberOfPages() || slot.pageNumber()!=slot.pageIndex()+1)
            fail("Genere una previsualización con ubicación de firma válida.");
        var page=pdf.getPage(slot.pageIndex());var box=page.getMediaBox();
        if(page.getRotation()!=0 || !Float.isFinite(b.x()+b.y()+b.width()+b.height()) || b.x()<0 || b.y()<0 || b.width()<60 || b.height()<30
            || b.x()+b.width()>box.getWidth() || b.y()+b.height()>box.getHeight()) fail("Ubicación de firma fuera de la página.");
        var field=pdf.getDocumentCatalog().getAcroForm().getFields().stream().filter(f->f instanceof PDSignatureField sf && sf.getSignature()!=null && signature.getCOSObject().equals(sf.getSignature().getCOSObject())).findFirst().orElseThrow();
        field.setPartialName(slot.action().equals("ELABORADO_POR")&&pdf.getDocumentCatalog().getAcroForm().getField("UTAPED_AUTHOR")==null?"UTAPED_AUTHOR":"UTAPED_"+slot.action()+"_"+slot.actorId());var widget=field.getWidgets().getFirst();widget.setRectangle(new PDRectangle(b.x(),b.y(),b.width(),b.height()));widget.setPage(page);widget.setPrinted(true);
        for(var p:pdf.getPages()) { var annotations=p.getAnnotations();annotations.removeIf(a->a.getCOSObject().equals(widget.getCOSObject()));p.setAnnotations(annotations); }page.getAnnotations().add(widget);
        var appearance=new PDAppearanceStream(pdf);appearance.setResources(new PDResources());appearance.setBBox(new PDRectangle(b.width(),b.height()));
        stamp(pdf,appearance,b.width(),b.height(),name,slot.action(),now);
        var dictionary=new PDAppearanceDictionary();dictionary.setNormalAppearance(appearance);widget.setAppearance(dictionary);
        widget.getCOSObject().setNeedToBeUpdated(true);page.getCOSObject().setNeedToBeUpdated(true);
        pdf.getDocumentCatalog().getAcroForm().getCOSObject().setNeedToBeUpdated(true);
    }

    private static final java.time.ZoneId STAMP_ZONE=java.time.ZoneId.of("America/Guayaquil");
    /** Visible stamp: QR with the signature data on the left, signer and time on the right. */
    private static void stamp(PDDocument pdf,PDAppearanceStream appearance,float width,float height,String signer,String action,Instant now) throws IOException {
        var regular=new PDType1Font(Standard14Fonts.FontName.COURIER);var bold=new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
        String name=(signer==null||signer.isBlank()?"Firmante":signer.trim()).toUpperCase(java.util.Locale.ROOT);
        String reason=action==null?"":action.charAt(0)+action.substring(1).toLowerCase(java.util.Locale.ROOT).replace('_',' ');
        // The QR repeats the signature metadata; validity comes from the embedded CMS signature, not from the image.
        String data=String.join("\n","FIRMADO POR: "+name,"RAZON: "+reason,"LOCALIZACION: Universidad Tecnica de Ambato",
            "FECHA: "+java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now.atZone(STAMP_ZONE).truncatedTo(java.time.temporal.ChronoUnit.SECONDS)),
            "VALIDAR CON: www.firmadigital.gob.ec");
        com.google.zxing.common.BitMatrix matrix;
        try {
            matrix=new com.google.zxing.qrcode.QRCodeWriter().encode(data,com.google.zxing.BarcodeFormat.QR_CODE,0,0,Map.of(
                com.google.zxing.EncodeHintType.MARGIN,0,com.google.zxing.EncodeHintType.CHARACTER_SET,"UTF-8",
                com.google.zxing.EncodeHintType.ERROR_CORRECTION,com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M));
        } catch(com.google.zxing.WriterException error) { throw new IOException("No se pudo generar el código QR de la firma.",error); }
        float pad=2,qr=Math.min(height-2*pad,width*0.38f),module=qr/matrix.getWidth(),qrBottom=(height-qr)/2;
        float textX=pad+qr+4,textWidth=width-textX-pad;
        String label=encodable(regular,"Firmado electrónicamente por:"),date=encodable(regular,"Fecha: "+java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(STAMP_ZONE).format(now)+" (EC)");
        float small=Math.max(2.5f,Math.min(Math.min(7,height/7),fit(regular,label,textWidth)));float dateSize=Math.max(2.5f,Math.min(small,fit(regular,date,textWidth)));
        float available=height-2*pad-small*1.6f-dateSize*1.6f;
        String safeName=encodable(bold,name);List<String> lines=List.of(safeName);float nameSize=3;
        for(float size=Math.min(14,height/4);size>=3;size-=0.5f) {
            var wrapped=wrap(bold,safeName,size,textWidth);
            if(wrapped.size()*size*1.15f<=available) { lines=wrapped;nameSize=size;break; }
        }
        if(lines.size()==1&&lines.getFirst().equals(safeName)&&bold.getStringWidth(safeName)/1000*nameSize>textWidth) lines=wrap(bold,safeName,nameSize,textWidth);
        while(lines.size()>1&&lines.size()*nameSize*1.15f>available) lines=lines.subList(0,lines.size()-1);
        try(var content=new PDPageContentStream(pdf,appearance)) {
            content.setNonStrokingColor(java.awt.Color.BLACK);
            for(int y=0;y<matrix.getHeight();y++) for(int x=0;x<matrix.getWidth();x++)
                if(matrix.get(x,y)) content.addRect(pad+x*module,qrBottom+(matrix.getHeight()-1-y)*module,module,module);
            content.fill();
            float top=height-pad-small;
            content.beginText();content.setFont(regular,small);content.newLineAtOffset(textX,top);content.showText(label);content.endText();
            float lineY=top-small*0.6f-nameSize*1.15f;
            for(var line:lines) { content.beginText();content.setFont(bold,nameSize);content.newLineAtOffset(textX,lineY);content.showText(line);content.endText();lineY-=nameSize*1.15f; }
            content.beginText();content.setFont(regular,dateSize);content.newLineAtOffset(textX,pad+1);content.showText(date);content.endText();
        }
    }
    private static float fit(PDType1Font font,String text,float width) throws IOException {
        float units=font.getStringWidth(text)/1000;return units<=0?width:width/units;
    }
    private static List<String> wrap(PDType1Font font,String text,float size,float width) throws IOException {
        var lines=new ArrayList<String>();var current=new StringBuilder();
        for(var word:text.split(" +")) {
            String candidate=current.isEmpty()?word:current+" "+word;
            if(font.getStringWidth(candidate)/1000*size<=width) { current=new StringBuilder(candidate);continue; }
            if(!current.isEmpty()) { lines.add(current.toString());current=new StringBuilder(); }
            String rest=word;
            while(font.getStringWidth(rest)/1000*size>width&&rest.length()>1) {
                int count=rest.length();while(count>1&&font.getStringWidth(rest.substring(0,count))/1000*size>width) count--;
                lines.add(rest.substring(0,count));rest=rest.substring(count);
            }
            current.append(rest);
        }
        if(!current.isEmpty()) lines.add(current.toString());
        return lines;
    }
    /** Keeps the characters the standard font can encode (Spanish accents included) and replaces the rest. */
    private static String encodable(PDType1Font font,String text) {
        var result=new StringBuilder();
        text.codePoints().forEach(c->{
            String character=new String(Character.toChars(c));
            try { font.encode(character);result.append(character); }
            catch(IOException|IllegalArgumentException error) {
                String plain=java.text.Normalizer.normalize(character,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","");
                result.append(plain.matches("[\\x20-\\x7e]")?plain:"?");
            }
        });
        return result.toString();
    }
}
