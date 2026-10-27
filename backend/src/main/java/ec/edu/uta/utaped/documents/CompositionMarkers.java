package ec.edu.uta.utaped.documents;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Ephemeral commands for one composition; never document data or persisted snapshot fields. */
final class CompositionMarkers {
    private final String page;
    private CompositionMarkers(String page) { this.page=page; }
    static CompositionMarkers forSnapshot(ArtifactModels.Snapshot snapshot) {
        // All native user-supplied strings are record components (including nested records/lists).
        // Reject a token occurring anywhere in them, even when it is a whole paragraph.
        String content=snapshot.toString().toUpperCase(Locale.ROOT),token;
        do { token=UUID.randomUUID().toString().replace("-","").substring(0,6).toUpperCase(Locale.ROOT); }
        while(content.contains(token));
        return new CompositionMarkers(token);
    }
    String page() { return page; } // Six narrow ASCII characters, no wider than the previous @@PN@@ placeholder.
    String top(String suffix) { return "@@SIGTOP"+page+suffix+"@@"; }
    String bottom(String suffix) { return "@@SIGBOTTOM"+page+suffix+"@@"; }
    String clear(String value) {
        return value.replaceAll("@@SIG(?:TOP|BOTTOM)"+Pattern.quote(page)+"[A0-9]*@@","").replace(page,"");
    }
}
