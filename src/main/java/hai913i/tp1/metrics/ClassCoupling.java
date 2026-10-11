package hai913i.tp1.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.model.TypeFact;

/**
 * Variante supplémentaire, nommée et définie (sujet § 4) : couplage d'appels inter-classes.
 *
 * Définition : deux types distincts A et B du projet sont couplés s'il existe au moins un
 * arc du graphe d'appel dont le caller appartient à A et la cible à B. Le poids est la
 * somme des poids de ces arcs. Les appels internes à une même classe ne comptent pas.
 * Fan-out(A) = nombre de types distincts que A appelle ; fan-in(A) = nombre de types
 * distincts qui appellent A. C'est la donnée de départ du couplage du TP 2.
 */
public final class ClassCoupling {

    public record Link(String fromType, String toType, int weight) {
    }

    public record TypeScore(String type, int fanIn, int fanOut, int strength) {
        public int partners() {
            return fanIn + fanOut;
        }
    }

    private final List<Link> links;
    private final List<TypeScore> scores;

    private ClassCoupling(List<Link> links, List<TypeScore> scores) {
        this.links = List.copyOf(links);
        this.scores = List.copyOf(scores);
    }

    public static ClassCoupling of(CallGraph graph) {
        Map<String, Integer> weights = new TreeMap<>();
        for (CallGraph.Edge edge : graph.edges()) {
            String from = CallGraph.ownerType(edge.from());
            String to = CallGraph.ownerType(edge.to());
            if (from.equals(to)) {
                continue;
            }
            weights.merge(from + "\n" + to, edge.weight(), Integer::sum);
        }
        List<Link> links = new ArrayList<>();
        Map<String, Integer> fanOut = new LinkedHashMap<>();
        Map<String, Integer> fanIn = new LinkedHashMap<>();
        Map<String, Integer> strength = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            int split = entry.getKey().indexOf('\n');
            String from = entry.getKey().substring(0, split);
            String to = entry.getKey().substring(split + 1);
            int w = entry.getValue();
            links.add(new Link(from, to, w));
            fanOut.merge(from, 1, Integer::sum);
            fanIn.merge(to, 1, Integer::sum);
            strength.merge(from, w, Integer::sum);
            strength.merge(to, w, Integer::sum);
        }
        List<TypeScore> scores = new ArrayList<>();
        for (String type : union(fanIn, fanOut, strength)) {
            scores.add(new TypeScore(type, fanIn.getOrDefault(type, 0), fanOut.getOrDefault(type, 0),
                    strength.getOrDefault(type, 0)));
        }
        scores.sort(Comparator.comparingInt(TypeScore::strength).reversed().thenComparing(TypeScore::type));
        return new ClassCoupling(links, scores);
    }

    public List<Link> links() {
        return links;
    }

    public List<TypeScore> scores() {
        return scores;
    }

    public String asText() {
        StringBuilder out = new StringBuilder();
        out.append("COUPLAGE D'APPELS INTER-CLASSES (variante, hors barème Q1-13)\n");
        out.append("  Definition : A->B ssi un arc du graphe va d'une methode de A vers une methode de B, A!=B.\n");
        out.append("  ").append(links.size()).append(" paires couplees.\n");
        for (Link link : links) {
            out.append("    ").append(link.fromType()).append(" -> ").append(link.toType())
                    .append("  [").append(link.weight()).append("]\n");
        }
        out.append("  Scores (force = somme des poids incidents) :\n");
        for (TypeScore score : scores) {
            out.append("    ").append(score.type())
                    .append("  fan-out=").append(score.fanOut())
                    .append("  fan-in=").append(score.fanIn())
                    .append("  force=").append(score.strength()).append('\n');
        }
        return out.toString();
    }

    public static String inheritanceDepths(List<TypeFact> types) {
        StringBuilder out = new StringBuilder();
        out.append("PROFONDEUR D'HERITAGE (variante) : nombre de superclasses hors Object.\n");
        types.stream()
                .sorted((a, b) -> Integer.compare(b.superclasses().size(), a.superclasses().size()))
                .forEach(t -> out.append("    ").append(t.qualifiedName()).append(" : ")
                        .append(t.superclasses().size()).append('\n'));
        return out.toString();
    }

    @SafeVarargs
    private static List<String> union(Map<String, Integer>... maps) {
        Map<String, Integer> all = new TreeMap<>();
        for (Map<String, Integer> map : maps) {
            all.putAll(map);
        }
        return new ArrayList<>(all.keySet());
    }
}
