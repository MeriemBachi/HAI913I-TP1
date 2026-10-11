package hai913i.tp1.extract;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.compiler.IProblem;

import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.CompilationIssue;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;

/**
 * Orchestre les visiteurs JDT et produit un {@link ProjectFacts} indépendant de JDT.
 */
public final class FactExtractor {

    private FactExtractor() {
    }

    public static ProjectFacts extract(ProjectSources sources, List<ParsedFile> parsed) {
        StructureVisitor structure = new StructureVisitor();
        for (ParsedFile file : parsed) {
            file.unit().accept(structure);
        }
        List<TypeFact> typesWithoutCalls = structure.types();
        Set<String> methodIds = typesWithoutCalls.stream()
                .flatMap(t -> t.methods().stream())
                .map(MethodFact::id)
                .collect(Collectors.toSet());

        Map<String, List<CallFact>> calls = new LinkedHashMap<>();
        List<CompilationIssue> issues = new ArrayList<>();
        for (ParsedFile file : parsed) {
            CallVisitor visitor = new CallVisitor(file.unit(), methodIds);
            file.unit().accept(visitor);
            visitor.callsByCaller().forEach((id, list) ->
                    calls.computeIfAbsent(id, key -> new ArrayList<>()).addAll(list));
            for (IProblem problem : file.unit().getProblems()) {
                issues.add(new CompilationIssue(
                        file.path().getFileName().toString(),
                        problem.getSourceLineNumber(),
                        problem.getMessage(),
                        problem.isError()));
            }
        }

        List<TypeFact> types = new ArrayList<>();
        for (TypeFact type : typesWithoutCalls) {
            List<MethodFact> methods = new ArrayList<>();
            for (MethodFact method : type.methods()) {
                List<CallFact> methodCalls = calls.getOrDefault(method.id(), List.of());
                methods.add(new MethodFact(method.id(), method.name(), method.parameterTypes(),
                        method.constructor(), method.bodyLoc(), method.visibility(), methodCalls));
            }
            types.add(new TypeFact(type.qualifiedName(), type.packageName(), type.kind(),
                    type.superclasses(), type.interfaces(), type.fields(), methods));
        }

        int loc = 0;
        try {
            for (var path : sources.javaFiles()) {
                loc += Files.readAllLines(path).size();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de compter les lignes de " + sources.sourceRoot(), e);
        }
        return new ProjectFacts(sources.sourceRoot(), sources.javaFiles(), loc, types, issues);
    }
}
