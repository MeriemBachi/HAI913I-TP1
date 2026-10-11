package hai913i.tp1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Méthode ou constructeur écrit dans un type compté. */
public record MethodFact(
        String id,
        String name,
        List<String> parameterTypes,
        boolean constructor,
        int bodyLoc,
        Visibility visibility,
        List<CallFact> calls) implements Comparable<MethodFact> {

    public MethodFact {
        parameterTypes = List.copyOf(parameterTypes);
        calls = sorted(calls);
    }

    public int parameterCount() {
        return parameterTypes.size();
    }

    public boolean hasBody() {
        return bodyLoc > 0;
    }

    private static List<CallFact> sorted(List<CallFact> calls) {
        List<CallFact> copy = new ArrayList<>(calls);
        Collections.sort(copy);
        return List.copyOf(copy);
    }

    @Override
    public int compareTo(MethodFact other) {
        return id.compareTo(other.id);
    }
}
