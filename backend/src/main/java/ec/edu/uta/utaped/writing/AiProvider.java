package ec.edu.uta.utaped.writing;

public interface AiProvider {
    record Configuration(boolean available, String provider, int maxInputCharacters) {}
    Configuration configuration();
    default String holidays(int year) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"La consulta asistida no está disponible."); }
    String suggest(String field, String tone, String text);
}
