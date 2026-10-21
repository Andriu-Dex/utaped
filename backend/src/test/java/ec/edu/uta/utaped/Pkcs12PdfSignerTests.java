package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.signing.Pkcs12PdfSigner;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.*;

class Pkcs12PdfSignerTests {
    static final Instant NOW=Instant.parse("2026-10-04T12:00:00Z");
    static final Provider BC=new BouncyCastleProvider();
    static KeyPair rootKey,rsaKey,ecKey;static X509Certificate root,rsa,ec;
    final Pkcs12PdfSigner signer=new Pkcs12PdfSigner();
    record Fixture(byte[] container,char[] password) {}
    @BeforeAll static void certificates() throws Exception {
        rootKey=key("RSA",2048);rsaKey=key("RSA",2048);ecKey=key("EC",256);
        root=certificate(rootKey,"Test root",true,KeyUsage.keyCertSign,NOW.minusSeconds(86400),NOW.plusSeconds(86400),true);
        rsa=certificate(rsaKey,"Test signer",false,KeyUsage.digitalSignature,NOW.minusSeconds(60),NOW.plusSeconds(3600),false);
        ec=certificate(ecKey,"Test EC signer",false,KeyUsage.digitalSignature,NOW.minusSeconds(60),NOW.plusSeconds(3600),false);
    }
    static KeyPair key(String type,int size) throws Exception {
        var generator=KeyPairGenerator.getInstance(type);
        if(type.equals("EC")) generator.initialize(new ECGenParameterSpec("secp256r1"));else generator.initialize(size);
        return generator.generateKeyPair();
    }
    static X509Certificate certificate(KeyPair key,String name,boolean ca,int usage,Instant from,Instant to,boolean self) throws Exception {
        var subject=new X500Name("CN="+name);var issuer=self?subject:new X500Name("CN=Test root");
        var builder=new JcaX509v3CertificateBuilder(issuer,new BigInteger(120,new SecureRandom()),Date.from(from),Date.from(to),subject,key.getPublic());
        builder.addExtension(org.bouncycastle.asn1.x509.Extension.basicConstraints,true,new BasicConstraints(ca));
        builder.addExtension(org.bouncycastle.asn1.x509.Extension.keyUsage,true,new KeyUsage(usage));
        return new JcaX509CertificateConverter().setProvider(BC).getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider(BC).build(self?key.getPrivate():rootKey.getPrivate())));
    }
    static String fingerprint(X509Certificate certificate) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded())); }
    static Set<TrustAnchor> anchors() { return Set.of(new TrustAnchor(root,null)); }
    static Fixture container(KeyPair key,X509Certificate certificate,boolean multiple) throws Exception {
        char[] password="Test-only-container-password".toCharArray();var store=KeyStore.getInstance("PKCS12");store.load(null,password);
        store.setKeyEntry("signer",key.getPrivate(),password,new java.security.cert.Certificate[]{certificate,root});
        if(multiple) store.setKeyEntry("other",rsaKey.getPrivate(),password,new java.security.cert.Certificate[]{rsa,root});
        var out=new ByteArrayOutputStream();store.store(out,password);return new Fixture(out.toByteArray(),password);
    }
    static byte[] pdf() throws Exception {
        try(var document=new PDDocument()) {
            var page=new PDPage();document.addPage(page);
            try(var content=new PDPageContentStream(document,page)) {
                content.beginText();content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);content.newLineAtOffset(40,700);content.showText("Original exact artifact");content.endText();
            }
            var out=new ByteArrayOutputStream();document.save(out);return out.toByteArray();
        }
    }
    static void cleared(Fixture fixture) {
        assertArrayEquals(new byte[fixture.container().length],fixture.container());assertArrayEquals(new char[fixture.password().length],fixture.password());
    }
    @Test void rsaAndEcSignRealCmsPreservingOriginalBytesAndPages() throws Exception {
        for(var algorithm:List.of("RSA","EC")) {
            var certificate=algorithm.equals("RSA")?rsa:ec;var fixture=container(algorithm.equals("RSA")?rsaKey:ecKey,certificate,false);
            byte[] original=pdf(),copy=original.clone();byte[] signed=signer.sign(original,fixture.container(),fixture.password(),fingerprint(certificate),anchors(),NOW);
            cleared(fixture);assertArrayEquals(copy,original);assertArrayEquals(original,Arrays.copyOf(signed,original.length));
            signer.verify(original,signed,fingerprint(certificate),anchors(),NOW);
            try(var document=Loader.loadPDF(signed)) { assertEquals(1,document.getNumberOfPages());assertEquals(1,document.getSignatureDictionaries().size()); }
        }
    }
    @Test void rejectsChangedSignedContentAppendedBytesAndDifferentOriginal() throws Exception {
        var fixture=container(rsaKey,rsa,false);byte[] original=pdf();byte[] signed=signer.sign(original,fixture.container(),fixture.password(),fingerprint(rsa),anchors(),NOW);
        byte[] tampered=signed.clone();tampered[15]^=1;
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.verify(original,tampered,fingerprint(rsa),anchors(),NOW));
        byte[] appended=Arrays.copyOf(signed,signed.length+1);
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.verify(original,appended,fingerprint(rsa),anchors(),NOW));
        byte[] altered=original.clone();altered[16]^=1;
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.verify(altered,signed,fingerprint(rsa),anchors(),NOW));
        // Tamper a covered byte in the incremental section without changing the original prefix.
        byte[] incremental=signed.clone();String text=new String(incremental,java.nio.charset.StandardCharsets.ISO_8859_1);
        int date=text.indexOf("20261004",original.length);assertTrue(date>original.length);incremental[date+7]='5';
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.verify(original,incremental,fingerprint(rsa),anchors(),NOW));
    }
    @Test void rejectsWrongPasswordAndMalformedContainerClearingSecrets() throws Exception {
        var fixture=container(rsaKey,rsa,false);Arrays.fill(fixture.password(),'x');
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.sign(pdf(),fixture.container(),fixture.password(),fingerprint(rsa),anchors(),NOW));cleared(fixture);
        var invalid=new Fixture(new byte[]{1,2,3},"test".toCharArray());
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.sign(pdf(),invalid.container(),invalid.password(),fingerprint(rsa),anchors(),NOW));cleared(invalid);
    }
    @Test void requiresExplicitIdentityBindingAndTrustAnchors() throws Exception {
        for(String fingerprint:Arrays.asList(null,"0".repeat(64),fingerprint(ec))) {
            var fixture=container(rsaKey,rsa,false);
            assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.sign(pdf(),fixture.container(),fixture.password(),fingerprint,anchors(),NOW));cleared(fixture);
        }
        var fixture=container(rsaKey,rsa,false);
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.sign(pdf(),fixture.container(),fixture.password(),fingerprint(rsa),Set.of(),NOW));cleared(fixture);
        var unrelated=certificate(key("RSA",2048),"Other root",true,KeyUsage.keyCertSign,NOW.minusSeconds(60),NOW.plusSeconds(3600),true);
        var untrusted=container(rsaKey,rsa,false);
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.sign(pdf(),untrusted.container(),untrusted.password(),fingerprint(rsa),Set.of(new TrustAnchor(unrelated,null)),NOW));cleared(untrusted);
    }
    @Test void rejectsExpiredFutureCaAndNonSigningCertificates() throws Exception {
        var invalids=List.of(
            certificate(rsaKey,"Expired",false,KeyUsage.digitalSignature,NOW.minusSeconds(120),NOW.minusSeconds(60),false),
            certificate(rsaKey,"Future",false,KeyUsage.digitalSignature,NOW.plusSeconds(60),NOW.plusSeconds(120),false),
            certificate(rsaKey,"CA",true,KeyUsage.keyCertSign,NOW.minusSeconds(60),NOW.plusSeconds(120),false),
            certificate(rsaKey,"Encryption",false,KeyUsage.keyEncipherment,NOW.minusSeconds(60),NOW.plusSeconds(120),false));
        for(var certificate:invalids) {
            var fixture=container(rsaKey,certificate,false);
            assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.sign(pdf(),fixture.container(),fixture.password(),fingerprint(certificate),anchors(),NOW));cleared(fixture);
        }
    }
    @Test void rejectsMultipleKeysWeakKeyInvalidPdfAndAlreadySignedInput() throws Exception {
        var multiple=container(ecKey,ec,true);
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.sign(pdf(),multiple.container(),multiple.password(),fingerprint(ec),anchors(),NOW));cleared(multiple);
        var weakKey=key("RSA",1024);var weak=certificate(weakKey,"Weak",false,KeyUsage.digitalSignature,NOW.minusSeconds(60),NOW.plusSeconds(120),false);
        var fixture=container(weakKey,weak,false);
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.sign(pdf(),fixture.container(),fixture.password(),fingerprint(weak),anchors(),NOW));cleared(fixture);
        var invalid=container(rsaKey,rsa,false);
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.sign(new byte[]{1,2,3},invalid.container(),invalid.password(),fingerprint(rsa),anchors(),NOW));cleared(invalid);
        var first=container(rsaKey,rsa,false);byte[] signed=signer.sign(pdf(),first.container(),first.password(),fingerprint(rsa),anchors(),NOW);
        var second=container(rsaKey,rsa,false);
        assertThrows(Pkcs12PdfSigner.SigningException.class,()->signer.sign(signed,second.container(),second.password(),fingerprint(rsa),anchors(),NOW));cleared(second);
    }
}
