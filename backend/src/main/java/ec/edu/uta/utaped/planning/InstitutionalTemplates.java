package ec.edu.uta.utaped.planning;

public final class InstitutionalTemplates {
    private InstitutionalTemplates() {}

    public static final String DEFAULT_JUSTIFICATION =
        "El presente Plan de Trabajo se formula en cumplimiento del Estatuto de la Universidad Técnica de Ambato, " +
        "el Reglamento de Carrera y Escalafón del Personal Académico del Sistema de Educación Superior y los lineamientos " +
        "académicos de la Facultad de Ingeniería en Sistemas, Electrónica e Industrial (FISEI). Su propósito es articular, " +
        "planificar y ejecutar de manera eficiente las actividades académicas, pedagógicas, de tutoría, investigación " +
        "formativa y vinculación con la sociedad correspondientes al presente período académico, garantizando el rigor " +
        "formativo y la calidad institucional.";

    public static String defaultJustificationFor(String documentType) {
        if ("T1".equalsIgnoreCase(documentType)) {
            return DEFAULT_JUSTIFICATION;
        }
        return "";
    }
}
