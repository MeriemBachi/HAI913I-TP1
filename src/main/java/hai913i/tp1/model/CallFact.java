package hai913i.tp1.model;

import java.util.Optional;

/**
 * Un site d'appel. {@code targetId} est vide si l'appel n'est pas résolu, ou s'il vise
 * une méthode hors du modèle (externe) — dans ce cas {@code resolved} peut rester vrai.
 */
public record CallFact(
        String callerId,
        Optional<String> targetId,
        String targetName,
        String staticReceiverType,
        int sourceLine,
        boolean resolved,
        boolean internal,
        CallKind kind) implements Comparable<CallFact> {

    public boolean countedAsInvocation() {
        return kind == CallKind.INVOCATION || kind == CallKind.SUPER;
    }

    @Override
    public int compareTo(CallFact other) {
        int byLine = Integer.compare(sourceLine, other.sourceLine);
        if (byLine != 0) {
            return byLine;
        }
        int byName = targetName.compareTo(other.targetName);
        if (byName != 0) {
            return byName;
        }
        return kind.compareTo(other.kind);
    }
}
