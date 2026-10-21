package ec.edu.uta.utaped.planning;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class WorkPlanModels {
    private WorkPlanModels() {}
    public record Create(@NotNull UUID groupId,@NotNull UUID periodId,@NotNull UUID requestKey,@Size(max=200) String title,UUID scopeId) {
        public Create(UUID groupId,UUID periodId,UUID requestKey,String title) { this(groupId,periodId,requestKey,title,null); }
    }
    public record Update(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=200) String title,
        @NotNull @Size(max=200) String institutionalUnit,@NotNull @Size(max=200) String career,
        @NotNull @Size(max=50000) String justification,@NotNull @Size(max=50000) String objective) {}
    public record Plan(UUID id,UUID teacherId,String teacherName,UUID groupId,String groupName,UUID periodId,String periodName,
        String title,String institutionalUnit,String career,String justification,String objective,String documentState,
        String formalVersion,long rowVersion,LocalDate preparationDate,OffsetDateTime createdAt,OffsetDateTime updatedAt,boolean editable,
        UUID scopeId,UUID createdBy,int reviewRound,OffsetDateTime archivedAt,String documentType,UUID basePlanId,boolean canManage) {
        public Plan withDraftContent(String draftTitle,String unit,String draftCareer,String justification,String objective) {
            return new Plan(id,teacherId,teacherName,groupId,groupName,periodId,periodName,draftTitle==null?title:draftTitle,unit==null?institutionalUnit:unit,draftCareer==null?career:draftCareer,
                justification,objective,documentState,formalVersion,rowVersion,preparationDate,createdAt,updatedAt,editable,
                scopeId,createdBy,reviewRound,archivedAt,documentType,basePlanId,canManage);
        }
        public Plan withPreparationDate(LocalDate date) {
            return new Plan(id,teacherId,teacherName,groupId,groupName,periodId,periodName,title,institutionalUnit,career,justification,objective,documentState,formalVersion,rowVersion,date,createdAt,updatedAt,editable,
                scopeId,createdBy,reviewRound,archivedAt,documentType,basePlanId,canManage);
        }
    }
    public record Summary(UUID id,String title,String groupName,String periodName,String documentState,
        String formalVersion,OffsetDateTime updatedAt,boolean editable,String documentType,OffsetDateTime archivedAt) {}
    public record Page(List<Summary> items,long total,int page,int size) {}
    public record GroupOption(UUID id,String name) {}
    public record PeriodOption(UUID id,String name,LocalDate preparationStartsOn,LocalDate preparationEndsOn,boolean editable,String status) {}
    public record ScopeOption(UUID id,UUID groupId,UUID periodId,String groupName,String periodName,String careerName,boolean canCreate,String suggestedTitle) {}
    public record Options(List<GroupOption> groups,List<PeriodOption> periods,boolean singlePlanPerScope,boolean enforcePreparationWindow,List<ScopeOption> scopes,String defaultJustification) {
        public Options(List<GroupOption> groups,List<PeriodOption> periods,boolean singlePlanPerScope,boolean enforcePreparationWindow,List<ScopeOption> scopes) {
            this(groups,periods,singlePlanPerScope,enforcePreparationWindow,scopes,InstitutionalTemplates.DEFAULT_JUSTIFICATION);
        }
    }
}
