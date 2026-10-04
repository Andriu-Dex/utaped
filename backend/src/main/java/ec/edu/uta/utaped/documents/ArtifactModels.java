package ec.edu.uta.utaped.documents;

import ec.edu.uta.utaped.planning.WorkPlanModels.Plan;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ArtifactModels {
    private ArtifactModels() {}
    public record Activity(String title,String category,String startsOn,String endsOn,String responsibleDisplay,List<UUID> responsibleIds,List<String> resources,List<String> means) {}
    public record Snapshot(Plan plan,String matrixSource,String elaboratedBy,boolean privacyNoticeEnabled,List<Activity> activities,List<AttachmentModels.Attachment> attachments,DocumentWorkflowSnapshot.Workflow workflow) {}
    public record Page(int index,int number,float width,float height,String kind,UUID attachmentId) {}
    public record SignatureSlot(String role,String action,String label,UUID actorId,int pageIndex,int pageNumber) {}
    public record Artifact(UUID id,long sourceRowVersion,String sourceHash,String templateVersion,int pageCount,List<Page> pages,List<SignatureSlot> signatureSlots,OffsetDateTime createdAt,boolean current) {}
    public record Blocker(String section,String message) {}
    public record Readiness(long rowVersion,boolean ready,List<Blocker> blockers) {}
    public record Generate(@NotNull @PositiveOrZero Long rowVersion) {}
    public record Generated(byte[] pdf,List<Page> pages,List<SignatureSlot> signatureSlots) {}
    public record SignaturePreparation(UUID targetDocId,UUID artifactId,long rowVersion,String pdfHash,boolean artifactCurrent,
        DocumentWorkflowSnapshot.Workflow workflow,String mechanism,boolean signingEnabled,List<Blocker> blockers) {}
}
