package ec.edu.uta.utaped.institution;

/** Name correction for new documents derived from older institutional snapshots. */
public final class InstitutionalNames {
    public static final String FISEI="Facultad de Ingeniería en Sistemas, Electrónica e Industrial";
    private InstitutionalNames() {}
    public static String forNewDocument(String name) {
        return "FISEI — Universidad Técnica de Ambato".equals(name)?FISEI:name;
    }
}
