package hai913i.tp1.callgraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.CallKind;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

/**
 * Graphe d'appel construit uniquement à partir du modèle de faits (§ 4.4).
 */
public final class CallGraph {

    public record Edge(String from, String to, int weight) implements Comparable<Edge> {
        @Override
        public int compareTo(Edge other) {
            int byFrom = from.compareTo(other.from);
            if (byFrom != 0) {
                return byFrom;
            }
            return to.compareTo(other.to);
        }
    }

    private final List<String> nodes;
    private final List<Edge> edges;
    private final int internalSites;
    private final int externalCalls;
    private final int unresolvedCalls;
    private final boolean constructors;
    private final boolean cha;

    private CallGraph(List<String> nodes, List<Edge> edges, int internalSites, int externalCalls, int unresolvedCalls,
            boolean constructors, boolean cha) {
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
        this.internalSites = internalSites;
        this.externalCalls = externalCalls;
        this.unresolvedCalls = unresolvedCalls;
        this.constructors = constructors;
        this.cha = cha;
    }

    public static CallGraph build(ProjectFacts facts, boolean constructors, boolean cha) {
        List<String> nodes = facts.methods().stream().map(MethodFact::id).sorted().toList();
        Set<String> nodeSet = Set.copyOf(nodes);
        Map<String, Integer> weights = new TreeMap<>();
        int internal = 0;
        int external = 0;
        int unresolved = 0;

        Hierarchy hierarchy = cha ? Hierarchy.of(facts) : null;

        for (CallFact call : facts.allCalls()) {
            if (!isIncluded(call.kind(), constructors)) {
                continue;
            }
            if (!call.resolved()) {
                unresolved++;
                continue;
            }
            if (!call.internal() || call.targetId().isEmpty() || !nodeSet.contains(call.targetId().get())) {
                external++;
                continue;
            }
            internal++;
            String from = call.callerId();
            String to = call.targetId().get();
            addWeight(weights, from, to);
            if (hierarchy != null) {
                for (String override : hierarchy.overrides(to)) {
                    addWeight(weights, from, override);
                }
            }
        }

        List<Edge> edges = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            int split = entry.getKey().indexOf('\n');
            edges.add(new Edge(entry.getKey().substring(0, split), entry.getKey().substring(split + 1),
                    entry.getValue()));
        }
        edges.sort(Comparator.naturalOrder());
        return new CallGraph(nodes, edges, internal, external, unresolved, constructors, cha);
    }

    private static boolean isIncluded(CallKind kind, boolean constructors) {
        if (kind == CallKind.INVOCATION || kind == CallKind.SUPER) {
            return true;
        }
        return constructors && (kind == CallKind.CONSTRUCTOR || kind == CallKind.THIS_CONSTRUCTOR
                || kind == CallKind.SUPER_CONSTRUCTOR);
    }

    private static void addWeight(Map<String, Integer> weights, String from, String to) {
        String key = from + "\n" + to;
        weights.merge(key, 1, Integer::sum);
    }

    public List<String> nodes() {
        return nodes;
    }

    public List<Edge> edges() {
        return edges;
    }

    public int internalSites() {
        return internalSites;
    }

    public int externalCalls() {
        return externalCalls;
    }

    public int unresolvedCalls() {
        return unresolvedCalls;
    }

    public List<Edge> callees(String methodId) {
        return edges.stream().filter(e -> e.from().equals(methodId)).toList();
    }

    public List<Edge> callers(String methodId) {
        return edges.stream().filter(e -> e.to().equals(methodId)).toList();
    }

    public static String ownerType(String methodId) {
        int hash = methodId.indexOf('#');
        return hash < 0 ? methodId : methodId.substring(0, hash);
    }

    public static String shortLabel(String id) {
        int hash = id.indexOf('#');
        if (hash < 0) {
            return id;
        }
        String type = id.substring(0, hash);
        String rest = id.substring(hash + 1);
        int dot = type.lastIndexOf('.');
        String simple = dot < 0 ? type : type.substring(dot + 1);
        return simple + "#" + rest;
    }

    /**
     * Vue filtrée : nœuds et arcs dont au moins une extrémité appartient au paquetage
     * (préfixe). C2 du sujet : visualisation éventuellement filtrée par paquetage.
     */
    public CallGraph restrictToPackage(String packagePrefix) {
        if (packagePrefix == null || packagePrefix.isBlank()) {
            return this;
        }
        List<Edge> kept = edges.stream()
                .filter(e -> inPackage(e.from(), packagePrefix) || inPackage(e.to(), packagePrefix))
                .toList();
        List<String> keptNodes = nodes.stream()
                .filter(n -> inPackage(n, packagePrefix)
                        || kept.stream().anyMatch(e -> e.from().equals(n) || e.to().equals(n)))
                .toList();
        return new CallGraph(keptNodes, kept, internalSites, externalCalls, unresolvedCalls, constructors, cha);
    }

    private static boolean inPackage(String methodId, String packagePrefix) {
        String type = ownerType(methodId);
        return type.equals(packagePrefix) || type.startsWith(packagePrefix + ".");
    }

    public String asText() {
        StringBuilder out = new StringBuilder();
        out.append("GRAPHE D'APPEL");
        if (constructors) {
            out.append(" (avec constructeurs)");
        }
        if (cha) {
            out.append(" (CHA)");
        }
        out.append('\n');
        out.append("  noeuds              : ").append(nodes.size()).append('\n');
        out.append("  arcs                : ").append(edges.size()).append('\n');
        out.append("  sites internes      : ").append(internalSites).append('\n');
        out.append("  appels externes     : ").append(externalCalls).append('\n');
        out.append("  appels non resolus  : ").append(unresolvedCalls).append('\n');
        out.append("  arcs (poids) :\n");
        for (Edge edge : edges) {
            out.append("    ").append(edge.from()).append(" -> ").append(edge.to())
                    .append("  [").append(edge.weight()).append("]\n");
        }
        return out.toString();
    }

    public String toDot() {
        StringBuilder out = new StringBuilder();
        out.append("digraph callgraph {\n");
        out.append("  rankdir=LR;\n");
        out.append("  node [shape=box, fontname=Helvetica, fontsize=9];\n");
        for (String node : nodes) {
            boolean isolated = edges.stream().noneMatch(e -> e.from().equals(node) || e.to().equals(node));
            if (isolated) {
                continue;
            }
            out.append("  ").append(quote(node)).append(" [label=").append(quote(shortLabel(node))).append("];\n");
        }
        for (Edge edge : edges) {
            out.append("  ").append(quote(edge.from())).append(" -> ").append(quote(edge.to()))
                    .append(" [label=\"").append(edge.weight()).append("\"];\n");
        }
        out.append("}\n");
        return out.toString();
    }

    public String query(String methodId) {
        StringBuilder out = new StringBuilder();
        out.append("Methode : ").append(methodId).append('\n');
        out.append("  appelees :\n");
        List<Edge> outEdges = callees(methodId);
        if (outEdges.isEmpty()) {
            out.append("    (aucune)\n");
        } else {
            for (Edge edge : outEdges) {
                out.append("    -> ").append(edge.to()).append(" [").append(edge.weight()).append("]\n");
            }
        }
        out.append("  appelantes :\n");
        List<Edge> inEdges = callers(methodId);
        if (inEdges.isEmpty()) {
            out.append("    (aucune)\n");
        } else {
            for (Edge edge : inEdges) {
                out.append("    <- ").append(edge.from()).append(" [").append(edge.weight()).append("]\n");
            }
        }
        return out.toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Hiérarchie du projet, pour l'option CHA (bonus C3). */
    static final class Hierarchy {
        private final Map<String, Set<String>> subtypes = new LinkedHashMap<>();
        private final Map<String, List<MethodFact>> methodsByType = new LinkedHashMap<>();

        static Hierarchy of(ProjectFacts facts) {
            Hierarchy hierarchy = new Hierarchy();
            Map<String, TypeFact> byName = new LinkedHashMap<>();
            for (TypeFact type : facts.types()) {
                byName.put(type.qualifiedName(), type);
                hierarchy.methodsByType.put(type.qualifiedName(), type.methods());
            }
            for (TypeFact type : facts.types()) {
                for (String parent : type.superclasses()) {
                    hierarchy.subtypes.computeIfAbsent(parent, key -> new TreeSet<>()).add(type.qualifiedName());
                }
                for (String parent : type.interfaces()) {
                    hierarchy.subtypes.computeIfAbsent(parent, key -> new TreeSet<>()).add(type.qualifiedName());
                }
            }
            boolean changed;
            do {
                changed = false;
                for (String parent : List.copyOf(hierarchy.subtypes.keySet())) {
                    for (String child : List.copyOf(hierarchy.subtypes.getOrDefault(parent, Set.of()))) {
                        for (String grand : List.copyOf(hierarchy.subtypes.getOrDefault(child, Set.of()))) {
                            if (hierarchy.subtypes.computeIfAbsent(parent, key -> new TreeSet<>()).add(grand)) {
                                changed = true;
                            }
                        }
                    }
                }
            } while (changed);
            return hierarchy;
        }

        List<String> overrides(String methodId) {
            int hash = methodId.indexOf('#');
            if (hash < 0) {
                return List.of();
            }
            String type = methodId.substring(0, hash);
            String signature = methodId.substring(hash + 1);
            List<String> result = new ArrayList<>();
            for (String subtype : subtypes.getOrDefault(type, Set.of())) {
                for (MethodFact method : methodsByType.getOrDefault(subtype, List.of())) {
                    int localHash = method.id().indexOf('#');
                    if (localHash > 0 && method.id().substring(localHash + 1).equals(signature)
                            && !method.id().equals(methodId) && !method.constructor()) {
                        result.add(method.id());
                    }
                }
            }
            return result;
        }
    }
}
