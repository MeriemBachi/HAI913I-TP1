package hai913i.tp1.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modèle de faits du projet analysé. Aucune classe de {@code org.eclipse.jdt}
 * n'apparaît ici : métriques et graphe ne dépendent que de ces objets.
 */
public final class ProjectFacts {

    private final Path sourceRoot;
    private final List<Path> sourceFiles;
    private final int applicationLoc;
    private final List<TypeFact> types;
    private final List<CompilationIssue> issues;

    public ProjectFacts(Path sourceRoot, List<Path> sourceFiles, int applicationLoc,
            List<TypeFact> types, List<CompilationIssue> issues) {
        this.sourceRoot = sourceRoot;
        List<Path> files = new ArrayList<>(sourceFiles);
        Collections.sort(files);
        this.sourceFiles = List.copyOf(files);
        this.applicationLoc = applicationLoc;
        List<TypeFact> copy = new ArrayList<>(types);
        Collections.sort(copy);
        this.types = List.copyOf(copy);
        this.issues = List.copyOf(issues);
    }

    public Path sourceRoot() {
        return sourceRoot;
    }

    public List<Path> sourceFiles() {
        return sourceFiles;
    }

    public int compilationUnitCount() {
        return sourceFiles.size();
    }

    public int applicationLoc() {
        return applicationLoc;
    }

    public List<TypeFact> types() {
        return types;
    }

    public List<CompilationIssue> issues() {
        return issues;
    }

    public long errorCount() {
        return issues.stream().filter(CompilationIssue::error).count();
    }

    public Optional<TypeFact> type(String qualifiedName) {
        return types.stream().filter(t -> t.qualifiedName().equals(qualifiedName)).findFirst();
    }

    public Optional<MethodFact> method(String id) {
        return types.stream()
                .flatMap(t -> t.methods().stream())
                .filter(m -> m.id().equals(id))
                .findFirst();
    }

    public List<MethodFact> methods() {
        return types.stream().flatMap(t -> t.methods().stream()).sorted().toList();
    }

    public Set<String> methodIds() {
        return methods().stream().map(MethodFact::id).collect(Collectors.toUnmodifiableSet());
    }

    public List<CallFact> invocationCalls() {
        return methods().stream()
                .flatMap(m -> m.calls().stream())
                .filter(CallFact::countedAsInvocation)
                .sorted()
                .toList();
    }

    public List<CallFact> allCalls() {
        return methods().stream().flatMap(m -> m.calls().stream()).sorted().toList();
    }

    public long packageCount() {
        return types.stream().map(TypeFact::packageName).distinct().count();
    }

    public long constructorCount() {
        return methods().stream().filter(MethodFact::constructor).count();
    }
}
