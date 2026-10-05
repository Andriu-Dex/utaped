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
        return sign(original,container,password,boundFingerprint,anchors,now,null,null,false);
    }
    /** Direct trust in an administratively bound public certificate, without claiming CA/revocation validation. */
    public byte[] signPinned(byte[] original,byte[] container,char[] password,String boundFingerprint,
        Instant now,SignatureSlot slot,String signerName) {
        return sign(original,container,password,boundFingerprint,Set.of(),now,slot,signerName,true);
    }
    private byte[] sign(byte[] original,byte[] container,char[] password,String boundFingerprint,
        Set<TrustAnchor> anchors,Instant now,SignatureSlot slot,String signerName,boolean pinned) {
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
                if(pdf.isEncrypted() || pdf.getNumberOfPages()==0 || !pdf.getSignatureDictionaries().isEmpty())
                    fail("El núcleo inicial requiere un PDF sin cifrado ni firmas previas.");
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

    /** Verifies only this core's single-signature incremental output, not arbitrary PDF revisions. */
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
            try(var pdf=Loader.loadPDF(signed)) {
                var signatures=pdf.getSignatureDictionaries();
                if(signatures.size()!=1) fail("Resultado de firma no válido.");
                var signature=signatures.getFirst();var range=signature.getByteRange();
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
        field.setPartialName("UTAPED_AUTHOR");var widget=field.getWidgets().getFirst();widget.setRectangle(new PDRectangle(b.x(),b.y(),b.width(),b.height()));widget.setPage(page);widget.setPrinted(true);
        for(var p:pdf.getPages()) { var annotations=p.getAnnotations();annotations.removeIf(a->a.getCOSObject().equals(widget.getCOSObject()));p.setAnnotations(annotations); }page.getAnnotations().add(widget);
        var appearance=new PDAppearanceStream(pdf);appearance.setResources(new PDResources());appearance.setBBox(new PDRectangle(b.width(),b.height()));
        var font=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        String display=java.text.Normalizer.normalize(name==null?"Firmante":name,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^\\x20-\\x7e]","?");
        var lines=new ArrayList<String>();lines.add("Firma electronica");
        String remaining=display;while(!remaining.isEmpty() && lines.size()<4) {
            int count=remaining.length();while(count>1 && font.getStringWidth(remaining.substring(0,count))/1000*7>b.width()-8) count--;
            lines.add(remaining.substring(0,count));remaining=remaining.substring(count).stripLeading();
        }
        if(!remaining.isEmpty()) { var last=lines.removeLast();lines.add(last.substring(0,Math.max(1,last.length()-3))+"..."); }
        lines.add(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(java.time.ZoneId.of("America/Guayaquil")).format(now));
        try(var content=new PDPageContentStream(pdf,appearance)) {
            content.setNonStrokingColor(java.awt.Color.BLACK);content.beginText();content.setFont(font,7);content.newLineAtOffset(4,b.height()-9);
            for(var line:lines) { content.showText(line);content.newLineAtOffset(0,-8); }content.endText();
        }
        var dictionary=new PDAppearanceDictionary();dictionary.setNormalAppearance(appearance);widget.setAppearance(dictionary);
        widget.getCOSObject().setNeedToBeUpdated(true);page.getCOSObject().setNeedToBeUpdated(true);
        pdf.getDocumentCatalog().getAcroForm().getCOSObject().setNeedToBeUpdated(true);
    }
}
