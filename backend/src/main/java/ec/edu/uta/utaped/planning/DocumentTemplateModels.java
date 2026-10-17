package ec.edu.uta.utaped.planning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class DocumentTemplateModels {
    private DocumentTemplateModels() {}

    public record DocumentTemplate(
        String documentType,
        String name,
        String description,
        String justification,
        long rowVersion,
        OffsetDateTime updatedAt,
        UUID updatedBy,
        String updatedByName
    ) {}

    public record UpdateInput(
        @NotNull @PositiveOrZero Long rowVersion,
        @NotBlank @Size(max = 50000) String justification
    ) {}

    public record ResetInput(
        @NotNull @PositiveOrZero Long rowVersion
    ) {}
}
