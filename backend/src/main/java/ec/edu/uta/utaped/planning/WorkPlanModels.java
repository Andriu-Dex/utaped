package ec.edu.uta.utaped.planning;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class WorkPlanModels {
    private WorkPlanModels() {}
    public record Create(@NotNull UUID groupId,@NotNull UUID periodId,@NotNull UUID requestKey,@NotBlank @Size(max=200) String title) {}
    public record Update(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=200) String title,
        @NotNull @Size(max=200) String institutionalUnit,@NotNull @Size(max=200) String career,
        @NotNull @Size(max=50000) String justification,@NotNull @Size(max=50000) String objective) {}
    public record Plan(UUID id,UUID teacherId,String teacherName,UUID groupId,String groupName,UUID periodId,String periodName,
        String title,String institutionalUnit,String career,String justification,String objective,String documentState,
        String formalVersion,long rowVersion,LocalDate preparationDate,OffsetDateTime createdAt,OffsetDateTime updatedAt,boolean editable) {}
    public record Summary(UUID id,String title,String groupName,String periodName,String documentState,
        String formalVersion,OffsetDateTime updatedAt,boolean editable) {}
    public record Page(List<Summary> items,long total,int page,int size) {}
    public record GroupOption(UUID id,String name) {}
    public record PeriodOption(UUID id,String name,LocalDate preparationStartsOn,LocalDate preparationEndsOn,boolean editable) {}
    public record Options(List<GroupOption> groups,List<PeriodOption> periods,boolean singlePlanPerScope,boolean enforcePreparationWindow) {}
}
