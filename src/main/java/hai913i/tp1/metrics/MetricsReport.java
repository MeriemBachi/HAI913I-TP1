package hai913i.tp1.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

/** Les 13 métriques du sujet, calculées uniquement sur le modèle de faits. */
public final class MetricsReport {

    private final ProjectFacts facts;
    private final int thresholdX;

    public MetricsReport(ProjectFacts facts, int thresholdX) {
        this.facts = facts;
        this.thresholdX = thresholdX;
    }

    public int classCount() {
        return facts.types().size();
    }

    public int applicationLoc() {
        return facts.applicationLoc();
    }

    public int methodCount() {
        return facts.methods().size();
    }

    public long packageCount() {
        return facts.packageCount();
    }

    public double methodsPerClass() {
        return classCount() == 0 ? 0.0 : methodCount() / (double) classCount();
    }

    public int methodsWithBody() {
        return (int) facts.methods().stream().filter(MethodFact::hasBody).count();
    }

    public int bodyLocSum() {
        return facts.methods().stream().filter(MethodFact::hasBody).mapToInt(MethodFact::bodyLoc).sum();
    }

    public double locPerMethod() {
        int n = methodsWithBody();
        return n == 0 ? 0.0 : bodyLocSum() / (double) n;
    }

    public double fieldsPerClass() {
        int fields = facts.types().stream().mapToInt(TypeFact::fieldCount).sum();
        return classCount() == 0 ? 0.0 : fields / (double) classCount();
    }

    public List<TypeFact> topMethods() {
        return TopPercent.retain(facts.types(), TypeFact::methodCount);
    }

    public List<TypeFact> topFields() {
        return TopPercent.retain(facts.types(), TypeFact::fieldCount);
    }

    public List<TypeFact> intersection() {
        Set<String> fields = topFields().stream().map(TypeFact::qualifiedName).collect(Collectors.toSet());
        return topMethods().stream().filter(t -> fields.contains(t.qualifiedName())).toList();
    }

    public List<TypeFact> moreThanXMethods() {
        return facts.types().stream()
                .filter(t -> t.methodCount() > thresholdX)
                .sorted(Comparator.comparing(TypeFact::qualifiedName))
                .toList();
    }

    public List<MethodFact> topMethodsOf(TypeFact type) {
        return TopPercent.retain(type.methods(), MethodFact::bodyLoc);
    }

    public int maxParameters() {
        return facts.methods().stream().mapToInt(MethodFact::parameterCount).max().orElse(0);
    }

    public List<MethodFact> methodsWithMaxParameters() {
        int max = maxParameters();
        return facts.methods().stream().filter(m -> m.parameterCount() == max).toList();
    }

    public int thresholdX() {
        return thresholdX;
    }

    public static String formatAverage(double value) {
        return String.format(Locale.FRANCE, "%.2f", value);
    }

    public String asText() {
        StringBuilder out = new StringBuilder();
        out.append("METRIQUES\n");
        out.append("  1. classes                 : ").append(classCount()).append('\n');
        out.append("  2. lignes de l'application : ").append(applicationLoc()).append('\n');
        out.append("  3. methodes                : ").append(methodCount()).append('\n');
        out.append("  4. paquetages              : ").append(packageCount()).append('\n');
        out.append("  5. methodes / classe       : ").append(formatAverage(methodsPerClass())).append('\n');
        out.append("  6. lignes / methode        : ").append(formatAverage(locPerMethod()))
                .append(" (").append(bodyLocSum()).append(" lignes, ")
                .append(methodsWithBody()).append(" methodes avec corps)\n");
        out.append("  7. attributs / classe      : ").append(formatAverage(fieldsPerClass())).append('\n');
        out.append("  8. 10% classes (methodes)  : ").append(names(topMethods())).append('\n');
        out.append("  9. 10% classes (attributs) : ").append(names(topFields())).append('\n');
        out.append(" 10. intersection 8 et 9     : ").append(names(intersection())).append('\n');
        out.append(" 11. classes avec > ").append(thresholdX).append(" methodes : ")
                .append(names(moreThanXMethods())).append('\n');
        out.append(" 12. 10% methodes (LOC) par classe :\n");
        for (TypeFact type : facts.types()) {
            List<MethodFact> top = topMethodsOf(type);
            out.append("     - ").append(type.qualifiedName()).append(" : ");
            if (top.isEmpty()) {
                out.append("(aucune)\n");
            } else {
                List<String> items = new ArrayList<>();
                for (MethodFact method : top) {
                    items.add(method.name() + " (" + method.bodyLoc() + ")");
                }
                out.append(String.join(", ", items)).append('\n');
            }
        }
        out.append(" 13. max parametres          : ").append(maxParameters()).append(" — ");
        out.append(methodsWithMaxParameters().stream().map(MethodFact::id).collect(Collectors.joining(", ")));
        out.append('\n');
        return out.toString();
    }

    private static String names(List<TypeFact> types) {
        if (types.isEmpty()) {
            return "(aucune)";
        }
        return types.stream()
                .map(t -> t.qualifiedName() + " (" + t.methodCount() + " meth., " + t.fieldCount() + " attr.)")
                .collect(Collectors.joining(", "));
    }
}
