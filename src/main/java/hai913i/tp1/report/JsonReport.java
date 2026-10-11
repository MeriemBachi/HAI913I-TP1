package hai913i.tp1.report;

import java.util.stream.Collectors;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.metrics.ClassCoupling;
import hai913i.tp1.metrics.MetricsReport;
import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

/** Export JSON déterministe du modèle de faits, des métriques et du graphe. */
public final class JsonReport {

    private JsonReport() {
    }

    public static String render(ProjectFacts facts, MetricsReport metrics, CallGraph graph) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"sourceRoot\": ").append(quote(facts.sourceRoot().toString())).append(",\n");
        json.append("  \"compilationUnits\": ").append(facts.compilationUnitCount()).append(",\n");
        json.append("  \"errors\": ").append(facts.errorCount()).append(",\n");
        json.append("  \"metrics\": {\n");
        json.append("    \"classes\": ").append(metrics.classCount()).append(",\n");
        json.append("    \"applicationLoc\": ").append(metrics.applicationLoc()).append(",\n");
        json.append("    \"methods\": ").append(metrics.methodCount()).append(",\n");
        json.append("    \"packages\": ").append(metrics.packageCount()).append(",\n");
        json.append("    \"methodsPerClass\": ").append(metrics.methodsPerClass()).append(",\n");
        json.append("    \"locPerMethod\": ").append(metrics.locPerMethod()).append(",\n");
        json.append("    \"fieldsPerClass\": ").append(metrics.fieldsPerClass()).append(",\n");
        json.append("    \"thresholdX\": ").append(metrics.thresholdX()).append("\n");
        json.append("  },\n");
        json.append("  \"types\": [\n");
        var types = facts.types();
        for (int i = 0; i < types.size(); i++) {
            json.append(typeJson(types.get(i)));
            json.append(i < types.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ],\n");
        json.append("  \"graph\": {\n");
        json.append("    \"nodes\": ").append(graph.nodes().size()).append(",\n");
        json.append("    \"edges\": ").append(graph.edges().size()).append(",\n");
        json.append("    \"internalSites\": ").append(graph.internalSites()).append(",\n");
        json.append("    \"arcs\": [\n");
        var edges = graph.edges();
        for (int i = 0; i < edges.size(); i++) {
            CallGraph.Edge e = edges.get(i);
            json.append("      {\"from\": ").append(quote(e.from()))
                    .append(", \"to\": ").append(quote(e.to()))
                    .append(", \"weight\": ").append(e.weight()).append("}");
            json.append(i < edges.size() - 1 ? ",\n" : "\n");
        }
        json.append("    ]\n  },\n");
        ClassCoupling coupling = ClassCoupling.of(graph);
        json.append("  \"classCoupling\": [\n");
        for (int i = 0; i < coupling.links().size(); i++) {
            ClassCoupling.Link link = coupling.links().get(i);
            json.append("    {\"from\": ").append(quote(link.fromType()))
                    .append(", \"to\": ").append(quote(link.toType()))
                    .append(", \"weight\": ").append(link.weight()).append("}");
            json.append(i < coupling.links().size() - 1 ? ",\n" : "\n");
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

    private static String typeJson(TypeFact type) {
        String fields = type.fields().stream()
                .map(JsonReport::fieldJson)
                .collect(Collectors.joining(", "));
        String methods = type.methods().stream()
                .map(JsonReport::methodJson)
                .collect(Collectors.joining(",\n      "));
        return """
                  {
                    "qualifiedName": %s,
                    "kind": %s,
                    "package": %s,
                    "superclasses": [%s],
                    "interfaces": [%s],
                    "fields": [%s],
                    "methods": [
                      %s
                    ]
                  }""".formatted(
                quote(type.qualifiedName()),
                quote(type.kind().name()),
                quote(type.packageName()),
                type.superclasses().stream().map(JsonReport::quote).collect(Collectors.joining(", ")),
                type.interfaces().stream().map(JsonReport::quote).collect(Collectors.joining(", ")),
                fields,
                methods);
    }

    private static String fieldJson(FieldFact field) {
        return "{\"name\": %s, \"type\": %s, \"visibility\": %s}".formatted(
                quote(field.name()), quote(field.declaredType()), quote(field.visibility().label()));
    }

    private static String methodJson(MethodFact method) {
        String calls = method.calls().stream()
                .filter(CallFact::countedAsInvocation)
                .map(c -> "{\"line\": %d, \"name\": %s, \"receiver\": %s, \"target\": %s, \"resolved\": %s}".formatted(
                        c.sourceLine(),
                        quote(c.targetName()),
                        quote(c.staticReceiverType()),
                        quote(c.targetId().orElse("")),
                        c.resolved()))
                .collect(Collectors.joining(", "));
        return "{\"id\": %s, \"constructor\": %s, \"loc\": %d, \"calls\": [%s]}".formatted(
                quote(method.id()), method.constructor(), method.bodyLoc(), calls);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
