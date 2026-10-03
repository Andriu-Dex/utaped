package ec.edu.uta.utaped.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class WorkflowModels {
    private WorkflowModels() {}
    public record Stage(@NotNull UUID id,@NotBlank @Size(max=160) String label,
        @NotBlank @Pattern(regexp="REVIEW|VALIDATE|APPROVE") String action,
        @NotBlank @Pattern(regexp="PERSON|GROUP_ROLE|COLLEGIATE") String recipientKind,
        @NotNull @Size(max=50) List<@NotNull UUID> assigneeIds,
        @Pattern(regexp="MEMBER|COORDINATOR") String groupRole,
        @NotNull @Size(max=160) String recipientLabel,boolean requiresSignature) {}
    public record Definition(@NotBlank @Size(max=160) String name,@NotNull @Size(max=20) List<@NotNull @Valid Stage> stages) {}
    public record Save(@NotNull @PositiveOrZero Long rowVersion,@NotNull @Valid Definition definition) {}
    public record Version(@NotNull @PositiveOrZero Long rowVersion) {}
    public record Revision(UUID id,int revisionNumber,Definition definition,String configuredBy,OffsetDateTime configuredAt) {}
    public record State(long rowVersion,boolean groupActive,Definition draft,Revision current,List<Revision> revisions,List<String> blockers) {}
}
