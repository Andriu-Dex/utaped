package ec.edu.uta.utaped.writing;

public interface AiProvider {
    record Configuration(boolean available, String provider, int maxInputCharacters) {}
    Configuration configuration();
    String suggest(String field, String tone, String text);
}
