package ec.edu.uta.utaped.documents;

import ec.edu.uta.utaped.planning.WorkPlanModels.Plan;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ArtifactModels {
    private ArtifactModels() {}
    public record Activity(String title,String category,String startsOn,String endsOn,String responsibleDisplay,List<UUID> responsibleIds,List<String> resources,List<String> means) {}
    public record ExternalReference(UUID evidenceId,int version,String activity,String medium,String title,String url) {}
    public record Snapshot(Plan plan,String matrixSource,String elaboratedBy,boolean privacyNoticeEnabled,List<Activity> activities,List<AttachmentModels.Attachment> attachments,DocumentWorkflowSnapshot.Workflow workflow,ec.edu.uta.utaped.planning.ReportService.Data report,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY) List<ExternalReference> externalReferences,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY) List<ec.edu.uta.utaped.planning.DocumentAuthors.Author> authors,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) ec.edu.uta.utaped.planning.ExecutionReportService.Data execution,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) ec.edu.uta.utaped.planning.DocumentStructures.Structure structure) {
        public Snapshot(Plan plan,String source,String elaboratedBy,boolean notice,List<Activity> activities,List<AttachmentModels.Attachment> attachments,DocumentWorkflowSnapshot.Workflow workflow,ec.edu.uta.utaped.planning.ReportService.Data report,List<ExternalReference> references) { this(plan,source,elaboratedBy,notice,activities,attachments,workflow,report,references,List.of(),null,null); }
        public Snapshot(Plan plan,String source,String elaboratedBy,boolean notice,List<Activity> activities,List<AttachmentModels.Attachment> attachments,DocumentWorkflowSnapshot.Workflow workflow,ec.edu.uta.utaped.planning.ReportService.Data report,List<ExternalReference> references,List<ec.edu.uta.utaped.planning.DocumentAuthors.Author> authors) { this(plan,source,elaboratedBy,notice,activities,attachments,workflow,report,references,authors,null,null); }
        public Snapshot(Plan plan,String source,String elaboratedBy,boolean notice,List<Activity> activities,List<AttachmentModels.Attachment> attachments,DocumentWorkflowSnapshot.Workflow workflow,ec.edu.uta.utaped.planning.ReportService.Data report,List<ExternalReference> references,List<ec.edu.uta.utaped.planning.DocumentAuthors.Author> authors,ec.edu.uta.utaped.planning.ExecutionReportService.Data execution) { this(plan,source,elaboratedBy,notice,activities,attachments,workflow,report,references,authors,execution,null); }
        public List<ec.edu.uta.utaped.planning.DocumentAuthors.Author> resolvedAuthors() {
            return authors==null||authors.isEmpty()?List.of(new ec.edu.uta.utaped.planning.DocumentAuthors.Author(plan.teacherId(),plan.teacherName(),"")):authors;
        }
        public Snapshot(Plan plan,String source,String elaboratedBy,boolean notice,List<Activity> activities,List<AttachmentModels.Attachment> attachments,DocumentWorkflowSnapshot.Workflow workflow,ec.edu.uta.utaped.planning.ReportService.Data report) { this(plan,source,elaboratedBy,notice,activities,attachments,workflow,report,List.of()); }
    }
    public record Page(int index,int number,float width,float height,String kind,UUID attachmentId) {}
    public record Bounds(float x,float y,float width,float height) {}
    public record SignatureSlot(String role,String action,String label,UUID actorId,int pageIndex,int pageNumber,Bounds bounds) {}
    public record Artifact(UUID id,long sourceRowVersion,String sourceHash,String templateVersion,int pageCount,List<Page> pages,List<SignatureSlot> signatureSlots,OffsetDateTime createdAt,boolean current) {}
    public record Blocker(String section,String message) {}
    public record Readiness(long rowVersion,boolean ready,List<Blocker> blockers) {}
    public record DraftPreview(@NotNull @PositiveOrZero Long rowVersion,
        @NotNull @Size(max=50000) String justification,@NotNull @Size(max=50000) String objective,
        @Min(-1) @Max(499) int pageIndex,@NotNull @Pattern(regexp="JUSTIFICATION|OBJECTIVE") String section,
        @Size(max=200) String title,@Size(max=200) String institutionalUnit,@Size(max=200) String career) {
        public DraftPreview(Long version,String justification,String objective,int page,String section) { this(version,justification,objective,page,section,null,null,null); }
    }
    public record DraftView(long sourceRowVersion,List<Page> pages,java.util.Map<String,Integer> sections,int pageIndex,String image) {}
    public record Generate(@NotNull @PositiveOrZero Long rowVersion) {}
    public record Generated(byte[] pdf,List<Page> pages,List<SignatureSlot> signatureSlots) {}
    public record SignaturePreparation(UUID targetDocId,UUID artifactId,long rowVersion,String pdfHash,boolean artifactCurrent,
        DocumentWorkflowSnapshot.Workflow workflow,String mechanism,boolean signingEnabled,List<Blocker> blockers) {}
}
