package ec.edu.uta.utaped.documents;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class AttachmentModels {
    private AttachmentModels() {}
    public record Attachment(UUID id,UUID fileId,String label,String title,String description,String originalName,long sizeBytes,int pageCount,String sha256,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) UUID originalFileId,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) String originalMediaType,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) String sourceName,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) Long sourceSizeBytes) {
        public Attachment(UUID id,UUID file,String label,String title,String description,String name,long size,int pages,String hash) { this(id,file,label,title,description,name,size,pages,hash,null,null,null,null); }
    }
    public record State(long rowVersion,boolean editable,String readOnlyReason,boolean enabled,boolean privacyNoticeEnabled,long maxFileBytes,long maxTotalBytes,int maxAttachments,List<Attachment> items) {}
    public record Settings(@NotNull @PositiveOrZero Long rowVersion,@NotNull Boolean enabled,@NotNull Boolean privacyNoticeEnabled) {}
    public record GeneratedInput(@NotNull @PositiveOrZero Long rowVersion,@NotNull UUID requestKey,@NotBlank @Size(max=200) String title,@NotBlank @Pattern(regexp="URL|TABLE") String kind,@Size(max=2048) String url,@Size(max=100) List<@NotNull @Size(min=1,max=8) List<@NotNull @Size(max=200) String>> rows) {}
    public record Update(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=200) String title,@NotNull @Size(max=2000) String description) {}
    public record Reorder(@NotNull @PositiveOrZero Long rowVersion,@NotNull @Size(max=100) List<@NotNull UUID> attachmentIds) {}
}
