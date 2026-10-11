package hai913i.tp1.model;

/** Visibilité d'un membre, y compris le cas « public implicite » des interfaces. */
public enum Visibility {
    PUBLIC("public"),
    PROTECTED("protected"),
    PRIVATE("private"),
    PACKAGE("package"),
    PUBLIC_IMPLICIT("public implicite");

    private final String label;

    Visibility(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
