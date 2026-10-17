package ec.edu.uta.utaped.institution;

import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/** Curated source data for the isolated example environment, not institutional assignments. */
public record DevelopmentReference(List<String> sources, String periodName, LocalDate startsOn, LocalDate endsOn,
    List<Career> careers, List<Group> groups, List<String> resources, List<String> means, List<Activity> activities) {
    public record Career(String key, String code, String name) {}
    public record Group(String key, String name, String type, List<String> careers, String source) {}
    public record Activity(String key, String groupKey, String title, String category, String source) {}
    public static DevelopmentReference load() {
        try (var stream = new ClassPathResource("development/institutional-reference.json").getInputStream()) {
            return JsonMapper.builder().build().readValue(stream, DevelopmentReference.class);
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot read development reference data", e); }
    }
}
