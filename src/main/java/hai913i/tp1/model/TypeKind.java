package hai913i.tp1.model;

/** Genre d'un type nommé compté (sujet § 4.1). */
public enum TypeKind {
    CLASS("classe"),
    INTERFACE("interface"),
    ENUM("enumeration"),
    RECORD("record");

    private final String label;

    TypeKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
