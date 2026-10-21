package ec.edu.uta.utaped.planning;

final class DocumentTitles {
    private static final String PLAN_PREFIX="Plan de Trabajo: ";
    private DocumentTitles() {}
    static String plan(String group) { return bounded(PLAN_PREFIX+group); }
    static String report(String planTitle) {
        return bounded("Informe: "+(planTitle.startsWith(PLAN_PREFIX)?planTitle.substring(PLAN_PREFIX.length()):planTitle));
    }
    private static String bounded(String title) { return title.substring(0,Math.min(200,title.length())); }
}
