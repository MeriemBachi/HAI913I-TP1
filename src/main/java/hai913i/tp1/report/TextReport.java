package hai913i.tp1.report;

import java.util.List;
import java.util.stream.Collectors;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.metrics.MetricsReport;
import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.CompilationIssue;
import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

/** Sortie texte déterministe (triée) du modèle de faits. */
public final class TextReport {

    private TextReport() {
    }

    public static String environment(ProjectFacts facts) {
        return """
                Racine des sources     : %s
                Unites de compilation  : %d
                Erreurs de compilation : %d
                """.formatted(facts.sourceRoot(), facts.compilationUnitCount(), facts.errorCount());
    }

    public static String issues(ProjectFacts facts) {
        List<CompilationIssue> errors = facts.issues().stream().filter(CompilationIssue::error).toList();
        if (errors.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder("Erreurs signalees :\n");
        for (CompilationIssue issue : errors) {
            out.append("  ").append(issue.fileName()).append(":").append(issue.line())
                    .append(" ").append(issue.message()).append('\n');
        }
        return out.toString();
    }

    public static String structure(ProjectFacts facts) {
        StringBuilder out = new StringBuilder();
        out.append("STRUCTURE\n");
        out.append("  types      : ").append(facts.types().size())
                .append(" (")
                .append(countKind(facts, "classe")).append(" classes, ")
                .append(countKind(facts, "interface")).append(" interfaces, ")
                .append(countKind(facts, "enumeration")).append(" enumerations, ")
                .append(countKind(facts, "record")).append(" records)\n");
        out.append("  methodes   : ").append(facts.methods().size())
                .append(" dont ").append(facts.constructorCount()).append(" constructeurs\n");
        out.append("  attributs  : ").append(facts.types().stream().mapToInt(TypeFact::fieldCount).sum()).append('\n');
        out.append("  paquetages : ").append(facts.packageCount()).append('\n');
        for (TypeFact type : facts.types()) {
            out.append("  * ").append(type.kind().label()).append(" ").append(type.qualifiedName()).append('\n');
            out.append("      paquetage    : ").append(type.packageName()).append('\n');
            out.append("      superclasses : ").append(join(type.superclasses())).append('\n');
            out.append("      interfaces   : ").append(join(type.interfaces())).append('\n');
            out.append("      attributs    :\n");
            if (type.fields().isEmpty()) {
                out.append("        (aucun)\n");
            } else {
                for (FieldFact field : type.fields()) {
                    out.append("        ").append(field.visibility().label()).append(" ")
                            .append(field.declaredType()).append(" ").append(field.name()).append('\n');
                }
            }
            out.append("      methodes     : ").append(type.methodCount()).append('\n');
            for (MethodFact method : type.methods()) {
                out.append("        ").append(method.id())
                        .append(" [").append(method.parameterCount()).append(" param.")
                        .append(method.constructor() ? ", constructeur" : "")
                        .append(", loc=").append(method.bodyLoc()).append("]\n");
            }
        }
        return out.toString();
    }

    public static String calls(ProjectFacts facts) {
        List<CallFact> invocations = facts.invocationCalls();
        long internal = invocations.stream().filter(CallFact::internal).count();
        long external = invocations.stream().filter(c -> c.resolved() && !c.internal()).count();
        long unresolved = invocations.stream().filter(c -> !c.resolved()).count();
        long methodInv = invocations.stream().filter(c -> c.kind().name().equals("INVOCATION")).count();
        long superInv = invocations.stream().filter(c -> c.kind().name().equals("SUPER")).count();
        StringBuilder out = new StringBuilder();
        out.append("APPELS\n");
        out.append("  total            : ").append(invocations.size())
                .append(" (").append(methodInv).append(" MethodInvocation, ")
                .append(superInv).append(" SuperMethodInvocation)\n");
        out.append("  internes         : ").append(internal).append('\n');
        out.append("  externes         : ").append(external).append('\n');
        out.append("  non resolus      : ").append(unresolved).append('\n');
        for (TypeFact type : facts.types()) {
            for (MethodFact method : type.methods()) {
                List<CallFact> methodCalls = method.calls().stream().filter(CallFact::countedAsInvocation).toList();
                if (methodCalls.isEmpty()) {
                    continue;
                }
                out.append("  * ").append(method.id()).append('\n');
                for (CallFact call : methodCalls) {
                    out.append("      L").append(call.sourceLine())
                            .append(" ").append(call.targetName())
                            .append("  receveur=").append(call.staticReceiverType())
                            .append("  cible=").append(call.targetId().orElse("non résolu"));
                    if (!call.resolved()) {
                        out.append(" [non resolu]");
                    } else if (!call.internal()) {
                        out.append(" [externe]");
                    }
                    out.append('\n');
                }
            }
        }
        return out.toString();
    }

    public static String full(ProjectFacts facts, MetricsReport metrics, CallGraph graph, String methodQuery) {
        StringBuilder out = new StringBuilder();
        out.append(environment(facts));
        out.append(issues(facts));
        out.append('\n').append(structure(facts)).append('\n');
        out.append(calls(facts)).append('\n');
        out.append(metrics.asText()).append('\n');
        out.append(graph.asText());
        if (methodQuery != null && !methodQuery.isBlank()) {
            out.append('\n').append(graph.query(methodQuery));
        }
        return out.toString();
    }

    private static long countKind(ProjectFacts facts, String label) {
        return facts.types().stream().filter(t -> t.kind().label().equals(label)).count();
    }

    private static String join(List<String> values) {
        if (values.isEmpty()) {
            return "(aucune)";
        }
        return values.stream().collect(Collectors.joining(", "));
    }
}
