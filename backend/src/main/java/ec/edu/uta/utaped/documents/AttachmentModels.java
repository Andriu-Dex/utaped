package ec.edu.uta.utaped.documents;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class AttachmentModels {
    private AttachmentModels() {}
    public record Attachment(UUID id,UUID fileId,String label,String title,String description,String originalName,long sizeBytes,int pageCount,String sha256) {}
    public record State(long rowVersion,boolean editable,boolean enabled,boolean privacyNoticeEnabled,long maxFileBytes,long maxTotalBytes,int maxAttachments,List<Attachment> items) {}
    public record Settings(@NotNull @PositiveOrZero Long rowVersion,@NotNull Boolean enabled,@NotNull Boolean privacyNoticeEnabled) {}
    public record Update(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=200) String title,@NotNull @Size(max=2000) String description) {}
    public record Reorder(@NotNull @PositiveOrZero Long rowVersion,@NotNull @Size(max=100) List<@NotNull UUID> attachmentIds) {}
}
