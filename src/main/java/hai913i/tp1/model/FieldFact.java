package hai913i.tp1.model;

/** Attribut déclaré dans un type compté. */
public record FieldFact(String name, String declaredType, Visibility visibility) implements Comparable<FieldFact> {

    @Override
    public int compareTo(FieldFact other) {
        int byName = name.compareTo(other.name);
        if (byName != 0) {
            return byName;
        }
        return declaredType.compareTo(other.declaredType);
    }
}
