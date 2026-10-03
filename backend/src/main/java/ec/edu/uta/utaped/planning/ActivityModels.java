package ec.edu.uta.utaped.planning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ActivityModels {
    private ActivityModels() {}
    public record Catalog(UUID id,String kind,String label,boolean active) {}
    public record CatalogInput(@NotBlank @Pattern(regexp="RESOURCE|MEANS") String kind,@NotBlank @Size(max=200) String label,boolean active) {}
    public record Definition(UUID id,UUID groupId,String title,String category,boolean mandatory,boolean active) {}
    public record DefinitionInput(@NotNull UUID groupId,@NotBlank @Size(max=500) String title,
        @NotBlank @Pattern(regexp="POA|IMPROVEMENT_PLAN|IMPROVEMENT_ACTION|OTHER") String category,boolean mandatory,boolean active) {}
    public record Member(UUID id,String name) {}
    public record Holiday(@NotNull LocalDate date,@NotBlank @Size(max=200) String label) {}
    public record Label(@NotNull @Size(max=200) String label) {}
    public record Policy(@NotNull Boolean enabled) {}
    public record Choice(UUID catalogId,@Size(max=500) String other,String label) {}
    public record Activity(@NotNull UUID id,UUID catalogId,@NotBlank @Size(max=500) String title,
        @NotBlank @Pattern(regexp="POA|IMPROVEMENT_PLAN|IMPROVEMENT_ACTION|OTHER") String category,boolean mandatory,
        @NotNull LocalDate startsOn,@NotNull LocalDate endsOn,
        @NotNull @Size(min=1,max=200) List<@NotNull UUID> responsibleIds,boolean collective,
        @NotNull @Size(min=1,max=50) List<@NotNull @Valid Choice> resources,
        @NotNull @Size(min=1,max=50) List<@NotNull @Valid Choice> means) {}
    public record Save(@NotNull @PositiveOrZero Long rowVersion,@NotNull @Size(max=500) String source,
        @NotNull @Size(max=100) List<@NotNull @Valid Activity> activities) {}
    public record Matrix(long rowVersion,String source,String elaboratedBy,String collectiveLabel,boolean editable,
        LocalDate periodStartsOn,LocalDate periodEndsOn,boolean restrictHolidayEndpoints,
        List<Activity> activities,List<Definition> definitions,List<Catalog> catalogs,List<Member> members,List<Holiday> holidays) {}
}
