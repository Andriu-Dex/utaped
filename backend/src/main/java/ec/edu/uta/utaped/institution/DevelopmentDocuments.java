package ec.edu.uta.utaped.institution;

import ec.edu.uta.utaped.documents.*;
import ec.edu.uta.utaped.planning.*;
import ec.edu.uta.utaped.signing.*;
import ec.edu.uta.utaped.workflow.*;
import java.io.*;
import java.math.BigDecimal;
import java.security.*;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Opt-in fixtures, composed and transitioned through the normal document services. */
@Component
@Profile("development-seed")
@ConditionalOnProperty(name="app.development.documents",havingValue="true")
@Order(20)
public class DevelopmentDocuments implements ApplicationRunner {
    private final JdbcTemplate jdbc; private final WorkPlanService plans; private final ActivityService activities;
    private final AttachmentService attachments; private final ArtifactService artifacts; private final CertificateBindings bindings;
    private final AuthorSignatureService signatures; private final ReviewService reviews; private final EvidenceService evidence;
    private final ReportService reports; private final PeriodWorkflowService periods; private final ec.edu.uta.utaped.audit.AuditService audit;
    private static final String AUTHOR="principal@utaped.example.invalid",ADMIN="admin@utaped.example.invalid";
    public DevelopmentDocuments(JdbcTemplate jdbc,WorkPlanService plans,ActivityService activities,AttachmentService attachments,
        ArtifactService artifacts,CertificateBindings bindings,AuthorSignatureService signatures,ReviewService reviews,
        EvidenceService evidence,ReportService reports,PeriodWorkflowService periods,ec.edu.uta.utaped.audit.AuditService audit) {
        this.jdbc=jdbc;this.plans=plans;this.activities=activities;this.attachments=attachments;this.artifacts=artifacts;
        this.bindings=bindings;this.signatures=signatures;this.reviews=reviews;this.evidence=evidence;this.reports=reports;this.periods=periods;this.audit=audit;
    }
    @Override @Transactional(rollbackFor=Exception.class) public void run(ApplicationArguments args) throws Exception {
        if(!"utaped_dev".equals(jdbc.queryForObject("SELECT current_database()",String.class))) throw new IllegalStateException("Example documents require utaped_dev");
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended('utaped-prepared-documents',0))");
        // A completed fixture is never reset, even after the owner edits or returns it.
        if(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='DEVELOPMENT_DOCUMENTS_CREATED'",Integer.class)>0) return;
        var scopes=plans.options(AUTHOR).scopes().stream().filter(WorkPlanModels.ScopeOption::canCreate).sorted(Comparator.comparing(s->s.id().toString())).toList();
        if(scopes.size()<3) throw new IllegalStateException("Three editable example scopes are required");
        var period=DevelopmentSeed.id("period");
        if(jdbc.queryForObject("SELECT workflow_locked_at IS NULL FROM academic_period WHERE id=?",Boolean.class,period))
            periods.activate(period,ADMIN,new PeriodWorkflowService.Version(jdbc.queryForObject("SELECT row_version FROM academic_period WHERE id=?",Long.class,period)));
        var draft=plan("editable",scopes.get(0));
        var reviewing=plan("revision",scopes.get(1));
        var approved=plan("aprobado",scopes.get(2));
        var previous=bindings.current(DevelopmentSeed.id("principal"));
        var fixture=certificate();
        var binding=bindings.register(DevelopmentSeed.id("principal"),ADMIN,new CertificateBindings.Register(StoredFiles.hash(fixture.certificate().getEncoded()),true,"Certificado efímero de ejemplo, sin validez institucional",previous==null?null:previous.id()));
        try {
            submit(reviewing,fixture);submit(approved,fixture);
            for(String role:List.of("reviewer","approver")) {
                String actor=role+"@utaped.example.invalid";
                var task=reviews.inbox(actor).stream().filter(t->t.documentId().equals(approved)).findFirst().orElseThrow();
                reviews.decide(task.id(),actor,new ReviewService.Decision(task.rowVersion(),"COMPLETE","",UUID.randomUUID()));
            }
        } finally {
            bindings.revoke(DevelopmentSeed.id("principal"),binding.id(),ADMIN);
            if(previous!=null) bindings.register(DevelopmentSeed.id("principal"),ADMIN,new CertificateBindings.Register(previous.fingerprint(),true,previous.verificationNote(),null));
            Arrays.fill(fixture.body(),(byte)0);
        }
        for(var activity:activities.get(approved,AUTHOR).activities())
            evidence.upload(approved,activity.id(),0,0,AUTHOR,new Upload("evidencia-ejemplo.pdf",pdf("Evidencia de seguimiento - ejemplo",activity.title())));
        var report=reports.create(approved,AUTHOR);var data=reports.get(report.id(),AUTHOR).data();
        var results=data.results().stream().map(r->new ReportService.Result(r.activityId(),BigDecimal.valueOf(100),"Actividad documentada con evidencia de ejemplo. No acredita ejecución institucional real.")).toList();
        reports.save(report.id(),AUTHOR,new ReportService.Save(report.rowVersion(),"Informe: seguimiento académico - Ejemplo", "Escenario de desarrollo derivado del Plan aprobado de ejemplo. Período Julio-Diciembre 2026.",
            "Se documentaron las actividades previstas y se organizó su evidencia para revisión.","Mantener revisiones periódicas y mejorar la organización de los registros.",results,List.of()));
        attachments.settings(report.id(),AUTHOR,new AttachmentModels.Settings(plans.get(report.id(),AUTHOR).rowVersion(),false,true));
        artifacts.generate(report.id(),AUTHOR,new ArtifactModels.Generate(plans.get(report.id(),AUTHOR).rowVersion()));
        audit.record(DevelopmentSeed.id("admin"),"DEVELOPMENT_DOCUMENTS_CREATED",draft);
    }
    private UUID plan(String scenario,WorkPlanModels.ScopeOption scope) throws Exception {
        var created=plans.create(AUTHOR,new WorkPlanModels.Create(scope.groupId(),scope.periodId(),DevelopmentSeed.id("prepared-document-"+scenario),"Plan de Trabajo: "+scope.groupName()+" - Ejemplo "+scenario,scope.id()));
        UUID doc=created.id();
        plans.update(doc,AUTHOR,new WorkPlanModels.Update(created.rowVersion(),created.title(),created.institutionalUnit(),created.career(),
            "Documento de ejemplo para revisar el flujo de planificación académica de "+scope.groupName()+" durante Julio-Diciembre 2026. No constituye una planificación ni una aprobación institucional real.\n\nSe organiza el trabajo, sus responsables y los registros de seguimiento para demostrar el funcionamiento del sistema.",
            "Planificar las actividades del ámbito, documentar su seguimiento y organizar los medios de verificación del período."));
        var matrix=activities.get(doc,AUTHOR);var definitions=matrix.definitions().stream().filter(ActivityModels.Definition::active).limit(3).toList();
        var resource=matrix.catalogs().stream().filter(c->c.active()&&c.kind().equals("RESOURCE")).findFirst().orElseThrow();
        var medium=matrix.catalogs().stream().filter(c->c.active()&&c.kind().equals("MEANS")&&c.evidenceKind().equals("PDF")).findFirst().orElseThrow();
        var responsible=matrix.members().stream().map(ActivityModels.Member::id).toList();var rows=new ArrayList<ActivityModels.Activity>();
        for(var definition:definitions) rows.add(new ActivityModels.Activity(UUID.randomUUID(),definition.id(),definition.title(),definition.category(),definition.mandatory(),matrix.periodStartsOn(),matrix.periodEndsOn(),responsible,true,
            List.of(new ActivityModels.Choice(resource.id(),null,null)),List.of(new ActivityModels.Choice(medium.id(),null,null))));
        if(rows.isEmpty()) throw new IllegalStateException("Example scope has no reference activities");
        activities.save(doc,AUTHOR,new ActivityModels.Save(plans.get(doc,AUTHOR).rowVersion(),scope.groupName()+" (escenario de ejemplo)",rows));
        attachments.settings(doc,AUTHOR,new AttachmentModels.Settings(plans.get(doc,AUTHOR).rowVersion(),true,true));
        attachments.upload(doc,AUTHOR,plans.get(doc,AUTHOR).rowVersion(),DevelopmentSeed.id("annex-"+scenario),"Registro de seguimiento de ejemplo",new Upload("registro-ejemplo.pdf",pdf("Registro de seguimiento - ejemplo",scope.groupName())));
        artifacts.generate(doc,AUTHOR,new ArtifactModels.Generate(plans.get(doc,AUTHOR).rowVersion()));
        return doc;
    }
    private void submit(UUID doc,Fixture fixture) {
        var artifact=artifacts.list(doc,AUTHOR).getFirst();var state=signatures.state(doc,artifact.id(),AUTHOR,true);
        signatures.sign(doc,artifact.id(),AUTHOR,plans.get(doc,AUTHOR).rowVersion(),state.preparation().pdfHash(),UUID.randomUUID(),fixture.body().clone(),true);
    }
    private record Fixture(java.security.cert.X509Certificate certificate,byte[] body) {}
    private static Fixture certificate() throws Exception {
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);var pair=generator.generateKeyPair();
        var name=new org.bouncycastle.asn1.x500.X500Name("CN=UTAPED EJEMPLO SIN VALIDEZ INSTITUCIONAL");var now=java.time.Instant.now();
        var builder=new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(name,new java.math.BigInteger(120,new SecureRandom()),Date.from(now.minusSeconds(60)),Date.from(now.plusSeconds(86400)),name,pair.getPublic());
        builder.addExtension(org.bouncycastle.asn1.x509.Extension.basicConstraints,true,new org.bouncycastle.asn1.x509.BasicConstraints(false));
        builder.addExtension(org.bouncycastle.asn1.x509.Extension.keyUsage,true,new org.bouncycastle.asn1.x509.KeyUsage(org.bouncycastle.asn1.x509.KeyUsage.digitalSignature));
        var provider=new org.bouncycastle.jce.provider.BouncyCastleProvider();
        var cert=new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().setProvider(provider).getCertificate(builder.build(new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(pair.getPrivate())));
        char[] password=UUID.randomUUID().toString().toCharArray();byte[] encoded=null,container=null;
        try {
            var store=KeyStore.getInstance("PKCS12");store.load(null,password);store.setKeyEntry("example",pair.getPrivate(),password,new java.security.cert.Certificate[]{cert});
            var output=new ByteArrayOutputStream();store.store(output,password);container=output.toByteArray();
            encoded=new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return new Fixture(cert,java.nio.ByteBuffer.allocate(4+encoded.length+container.length).putInt(encoded.length).put(encoded).put(container).array());
        } finally { Arrays.fill(password,'\0');if(encoded!=null)Arrays.fill(encoded,(byte)0);if(container!=null)Arrays.fill(container,(byte)0); }
    }
    private static byte[] pdf(String title,String subject) throws Exception {
        try(var pdf=new org.apache.pdfbox.pdmodel.PDDocument();var out=new ByteArrayOutputStream()) {
            var page=new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);pdf.addPage(page);
            try(var stream=new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf,page)) {
                stream.beginText();stream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),11);stream.setLeading(20);stream.newLineAtOffset(45,790);
                for(String line:List.of("DOCUMENTO DE EJEMPLO - SIN VALIDEZ INSTITUCIONAL",title,"Periodo: Julio-Diciembre 2026",subject)) {
                    for(int offset=0;offset<line.length();offset+=75) {stream.showText(line.substring(offset,Math.min(offset+75,line.length())));stream.newLine();}
                }
                stream.endText();
            }
            pdf.save(out);return out.toByteArray();
        }
    }
    private record Upload(String filename,byte[] bytes) implements MultipartFile {
        public String getName(){return "file";}public String getOriginalFilename(){return filename;}public String getContentType(){return "application/pdf";}
        public boolean isEmpty(){return bytes.length==0;}public long getSize(){return bytes.length;}public byte[] getBytes(){return bytes.clone();}
        public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}public void transferTo(java.io.File file)throws IOException{java.nio.file.Files.write(file.toPath(),bytes);}
    }
}
