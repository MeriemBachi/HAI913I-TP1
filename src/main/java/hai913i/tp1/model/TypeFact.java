package hai913i.tp1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Type nommé compté : classe, interface, énumération ou record. */
public record TypeFact(
        String qualifiedName,
        String packageName,
        TypeKind kind,
        List<String> superclasses,
        List<String> interfaces,
        List<FieldFact> fields,
        List<MethodFact> methods) implements Comparable<TypeFact> {

    public TypeFact {
        superclasses = List.copyOf(superclasses);
        interfaces = List.copyOf(interfaces);
        fields = sorted(fields);
        methods = sorted(methods);
    }

    public int methodCount() {
        return methods.size();
    }

    public int fieldCount() {
        return fields.size();
    }

    private static <T extends Comparable<T>> List<T> sorted(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.sort(copy);
        return List.copyOf(copy);
    }

    @Override
    public int compareTo(TypeFact other) {
        return qualifiedName.compareTo(other.qualifiedName);
    }
}
