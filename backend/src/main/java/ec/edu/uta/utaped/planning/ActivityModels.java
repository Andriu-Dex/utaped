package ec.edu.uta.utaped.planning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ActivityModels {
    private ActivityModels() {}
    public record Catalog(UUID id,String kind,String label,boolean active,String evidenceKind) {}
    public record CatalogInput(@NotBlank @Pattern(regexp="RESOURCE|MEANS") String kind,@NotBlank @Size(max=200) String label,boolean active,@Pattern(regexp="PDF|URL") String evidenceKind) {
        public CatalogInput(String kind,String label,boolean active) { this(kind,label,active,"PDF"); }
    }
    public record Context(@Size(max=200) String sourceKey,@Size(max=300) String sourceFile,
        @Size(max=4000) String objective,@Size(max=1000) String criterion,@Size(max=1000) String indicator,
        @Size(max=1000) String strategy,@Size(max=2000) String measure,@Size(max=1000) String plannedCompliance,
        @Min(2000) @Max(2200) Integer sourceYear,UUID periodId,UUID careerId) {}
    public record Definition(UUID id,UUID groupId,String title,String category,boolean mandatory,boolean active,Context context) {
        public Definition(UUID id,UUID group,String title,String category,boolean mandatory,boolean active) { this(id,group,title,category,mandatory,active,null); }
    }
    public record DefinitionInput(@NotNull UUID groupId,@NotBlank @Size(max=500) String title,
        @NotBlank @Pattern(regexp="POA|IMPROVEMENT_PLAN|IMPROVEMENT_ACTION|OTHER") String category,boolean mandatory,boolean active,@Valid Context context) {
        public DefinitionInput(UUID group,String title,String category,boolean mandatory,boolean active) { this(group,title,category,mandatory,active,null); }
    }
    public record Member(UUID id,String name) {}
    public record Holiday(@NotNull LocalDate date,@NotBlank @Size(max=200) String label,@Size(max=2048) String sourceUrl) { public Holiday(LocalDate date,String label) { this(date,label,null); } }
    public record Label(@NotNull @Size(max=200) String label) {}
    public record Policy(@NotNull Boolean enabled) {}
    public record Choice(UUID catalogId,@Size(max=500) String other,String label,String evidenceKind) {
        public Choice(UUID id,String other,String label) { this(id,other,label,"PDF"); }
        public String effectiveEvidenceKind() { return evidenceKind==null?"PDF":evidenceKind; }
    }
    public record Activity(@NotNull UUID id,UUID catalogId,@NotBlank @Size(max=500) String title,
        @NotBlank @Pattern(regexp="POA|IMPROVEMENT_PLAN|IMPROVEMENT_ACTION|OTHER") String category,boolean mandatory,
        @NotNull LocalDate startsOn,@NotNull LocalDate endsOn,
        @NotNull @Size(min=1,max=200) List<@NotNull UUID> responsibleIds,boolean collective,
        @NotNull @Size(min=1,max=50) List<@NotNull @Valid Choice> resources,
        @NotNull @Size(min=1,max=50) List<@NotNull @Valid Choice> means,Context context) {
        public Activity(UUID id,UUID catalogId,String title,String category,boolean mandatory,LocalDate starts,LocalDate ends,List<UUID> people,boolean collective,List<Choice> resources,List<Choice> means) {
            this(id,catalogId,title,category,mandatory,starts,ends,people,collective,resources,means,null);
        }
    }
    public record Save(@NotNull @PositiveOrZero Long rowVersion,@NotNull @Size(max=500) String source,
        @NotNull @Size(max=100) List<@NotNull @Valid Activity> activities,@Size(max=2000) String note) {
        public Save(Long version,String source,List<Activity> activities) { this(version,source,activities,null); }
    }
    public record Matrix(long rowVersion,String source,String elaboratedBy,String collectiveLabel,boolean editable,String readOnlyReason,
        LocalDate periodStartsOn,LocalDate periodEndsOn,boolean restrictHolidayEndpoints,
        List<Activity> activities,List<Definition> definitions,List<Catalog> catalogs,List<Member> members,List<Holiday> holidays,String note) {}
}
