package hai913i.tp1.extract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.CallKind;
import hai913i.tp1.model.CompilationIssue;
import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.model.TypeKind;
import hai913i.tp1.model.Visibility;
import hai913i.tp1.parse.ProjectSources;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Extracteur bonus (C1) : mêmes faits, autres conventions brutes de Spoon.
 * Les règles du TP (pas de constantes d'énumération, pas de constructeurs implicites)
 * sont appliquées pour rester comparable ; le rapport signale les écarts restants.
 */
public final class SpoonFactExtractor {

    private SpoonFactExtractor() {
    }

    public static ProjectFacts extract(ProjectSources sources, boolean noClasspath) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(sources.sourceRoot().toString());
        launcher.getEnvironment().setNoClasspath(noClasspath);
        launcher.getEnvironment().setComplianceLevel(17);
        launcher.getEnvironment().setCommentEnabled(false);
        launcher.getEnvironment().setShouldCompile(false);
        Path out = Path.of(System.getProperty("java.io.tmpdir"), "hai913i-spooned");
        launcher.setSourceOutputDirectory(out.toString());
        launcher.getEnvironment().setOutputType(spoon.OutputType.NO_OUTPUT);
        launcher.buildModel();
        CtModel model = launcher.getModel();

        List<TypeFact> types = new ArrayList<>();
        List<CtType<?>> counted = new ArrayList<>();
        for (CtType<?> type : model.getElements(new TypeFilter<>(CtType.class))) {
            if (isCounted(type)) {
                counted.add(type);
            }
        }

        Set<String> methodIds = counted.stream()
                .flatMap(t -> methodIdsOf(t).stream())
                .collect(Collectors.toSet());

        for (CtType<?> type : counted) {
            types.add(toTypeFact(type, methodIds));
        }

        int loc = 0;
        try {
            for (var path : sources.javaFiles()) {
                loc += Files.readAllLines(path).size();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        List<CompilationIssue> issues = List.of();
        return new ProjectFacts(sources.sourceRoot(), sources.javaFiles(), loc, types, issues);
    }

    private static boolean isCounted(CtType<?> type) {
        if (type instanceof CtAnnotationType || type.isAnonymous() || type.isLocalType()) {
            return false;
        }
        return type.getSimpleName() != null && !type.getSimpleName().isEmpty();
    }

    private static TypeKind kind(CtType<?> type) {
        if (type instanceof CtEnum) {
            return TypeKind.ENUM;
        }
        if (type instanceof CtRecord) {
            return TypeKind.RECORD;
        }
        if (type instanceof CtInterface) {
            return TypeKind.INTERFACE;
        }
        return TypeKind.CLASS;
    }

    private static String dots(String qualified) {
        return qualified.replace('$', '.');
    }

    private static String packageName(CtType<?> type) {
        CtPackage pkg = type.getPackage();
        if (pkg == null || pkg.isUnnamedPackage()) {
            return "(défaut)";
        }
        return pkg.getQualifiedName();
    }

    private static TypeFact toTypeFact(CtType<?> type, Set<String> projectMethodIds) {
        String qualified = dots(type.getQualifiedName());
        List<String> superclasses = new ArrayList<>();
        CtTypeReference<?> current = type.getSuperclass();
        while (current != null && current.getQualifiedName() != null
                && !"java.lang.Object".equals(current.getQualifiedName())) {
            superclasses.add(dots(current.getQualifiedName()));
            current = current.getSuperclass();
        }
        List<String> interfaces = type.getSuperInterfaces().stream()
                .map(ref -> dots(ref.getQualifiedName()))
                .sorted()
                .toList();

        List<FieldFact> fields = new ArrayList<>();
        for (CtField<?> field : type.getFields()) {
            if (field instanceof CtEnumValue) {
                continue;
            }
            String declared = field.getType() == null ? "?" : dots(field.getType().getQualifiedName());
            fields.add(new FieldFact(field.getSimpleName(), declared, visibility(field, type instanceof CtInterface)));
        }

        List<MethodFact> methods = new ArrayList<>();
        if (type instanceof CtClass<?> clazz) {
            for (CtConstructor<?> constructor : clazz.getConstructors()) {
                if (constructor.isImplicit()) {
                    continue;
                }
                String ctorName = Bindings.simpleName(qualified);
                methods.add(toMethod(qualified, ctorName, constructor.getParameters(), true,
                        constructor, projectMethodIds, type instanceof CtInterface));
            }
        }
        for (CtMethod<?> method : type.getMethods()) {
            if (method.isImplicit()) {
                continue;
            }
            methods.add(toMethod(qualified, method.getSimpleName(), method.getParameters(), false, method,
                    projectMethodIds, type instanceof CtInterface));
        }
        return new TypeFact(qualified, packageName(type), kind(type), superclasses, interfaces, fields, methods);
    }

    private static List<String> methodIdsOf(CtType<?> type) {
        List<String> ids = new ArrayList<>();
        String qualified = dots(type.getQualifiedName());
        if (type instanceof CtClass<?> clazz) {
            for (CtConstructor<?> constructor : clazz.getConstructors()) {
                if (!constructor.isImplicit()) {
                    ids.add(Bindings.methodId(qualified, Bindings.simpleName(qualified),
                            paramNames(constructor.getParameters())));
                }
            }
        }
        for (CtMethod<?> method : type.getMethods()) {
            if (!method.isImplicit()) {
                ids.add(Bindings.methodId(qualified, method.getSimpleName(), paramNames(method.getParameters())));
            }
        }
        return ids;
    }

    private static MethodFact toMethod(String typeName, String name, List<CtParameter<?>> parameters,
            boolean constructor, CtElement executable, Set<String> projectMethodIds, boolean interfaceType) {
        List<String> params = paramNames(parameters);
        String id = Bindings.methodId(typeName, name, params);
        int bodyLoc = 0;
        if (executable.getPosition() != null && executable.getPosition().isValidPosition()) {
            // Spoon donne la position de tout l'exécutable ; on approxime par le corps s'il existe.
            bodyLoc = Math.max(0, executable.getPosition().getEndLine() - executable.getPosition().getLine() + 1);
        }
        if (executable instanceof CtMethod<?> method && method.getBody() != null
                && method.getBody().getPosition().isValidPosition()) {
            bodyLoc = method.getBody().getPosition().getEndLine() - method.getBody().getPosition().getLine() + 1;
        }
        if (executable instanceof CtConstructor<?> ctor && ctor.getBody() != null
                && ctor.getBody().getPosition().isValidPosition()) {
            bodyLoc = ctor.getBody().getPosition().getEndLine() - ctor.getBody().getPosition().getLine() + 1;
        }
        if (executable instanceof CtMethod<?> method && method.getBody() == null) {
            bodyLoc = 0;
        }
        List<CallFact> calls = callsOf(id, executable, projectMethodIds);
        Visibility visibility = executable instanceof CtModifiable modifiable
                ? visibility(modifiable, interfaceType)
                : Visibility.PACKAGE;
        return new MethodFact(id, name, params, constructor, bodyLoc, visibility, calls);
    }

    private static List<String> paramNames(List<CtParameter<?>> parameters) {
        List<String> names = new ArrayList<>();
        for (CtParameter<?> parameter : parameters) {
            CtTypeReference<?> type = parameter.getType();
            names.add(type == null ? "?" : dots(type.getQualifiedName()));
        }
        return names;
    }

    private static List<CallFact> callsOf(String callerId, CtElement executable, Set<String> projectMethodIds) {
        List<CallFact> calls = new ArrayList<>();
        for (CtInvocation<?> invocation : executable.getElements(new TypeFilter<>(CtInvocation.class))) {
            if (invocation.isImplicit()) {
                continue;
            }
            CallKind kind = invocation.getTarget() instanceof CtSuperAccess
                    ? CallKind.SUPER
                    : CallKind.INVOCATION;
            calls.add(toCall(callerId, invocation, projectMethodIds, kind));
        }
        for (CtConstructorCall<?> creation : executable.getElements(new TypeFilter<>(CtConstructorCall.class))) {
            if (creation.isImplicit()) {
                continue;
            }
            calls.add(toCall(callerId, creation, projectMethodIds, CallKind.CONSTRUCTOR));
        }
        return calls;
    }

    private static CallFact toCall(String callerId, CtAbstractInvocation<?> invocation, Set<String> projectMethodIds,
            CallKind kind) {
        CtExecutableReference<?> executable = invocation.getExecutable();
        boolean resolved = executable != null && executable.getDeclaringType() != null
                && executable.getSimpleName() != null;
        String targetName = executable == null ? "?" : executable.getSimpleName();
        Optional<String> targetId = Optional.empty();
        boolean internal = false;
        if (resolved) {
            String typeName = dots(executable.getDeclaringType().getQualifiedName());
            String name = executable.isConstructor()
                    ? Bindings.simpleName(typeName)
                    : executable.getSimpleName();
            List<String> params = executable.getParameters().stream()
                    .map(ref -> dots(ref.getQualifiedName()))
                    .toList();
            String id = Bindings.methodId(typeName, name, params);
            targetId = Optional.of(id);
            internal = projectMethodIds.contains(id);
        }
        String receiver = receiverType(invocation, executable, kind);
        int line = invocation.getPosition() != null && invocation.getPosition().isValidPosition()
                ? invocation.getPosition().getLine()
                : 0;
        return new CallFact(callerId, targetId, targetName, receiver, line, resolved, internal, kind);
    }

    private static String receiverType(CtAbstractInvocation<?> invocation, CtExecutableReference<?> executable,
            CallKind kind) {
        if (kind == CallKind.SUPER || kind == CallKind.SUPER_CONSTRUCTOR) {
            if (executable != null && executable.getDeclaringType() != null) {
                return dots(executable.getDeclaringType().getQualifiedName());
            }
        }
        if (invocation instanceof CtInvocation<?> methodCall && methodCall.getTarget() != null
                && methodCall.getTarget().getType() != null) {
            return dots(methodCall.getTarget().getType().getQualifiedName());
        }
        if (executable != null && executable.getDeclaringType() != null) {
            return dots(executable.getDeclaringType().getQualifiedName());
        }
        return "?";
    }

    private static Visibility visibility(CtModifiable element, boolean interfaceMember) {
        if (element.hasModifier(ModifierKind.PUBLIC)) {
            return Visibility.PUBLIC;
        }
        if (element.hasModifier(ModifierKind.PROTECTED)) {
            return Visibility.PROTECTED;
        }
        if (element.hasModifier(ModifierKind.PRIVATE)) {
            return Visibility.PRIVATE;
        }
        if (interfaceMember) {
            return Visibility.PUBLIC_IMPLICIT;
        }
        return Visibility.PACKAGE;
    }

    public static String compare(ProjectFacts jdt, ProjectFacts spoon) {
        StringBuilder out = new StringBuilder();
        out.append("COMPARAISON JDT / SPOON (conventions du TP appliquees cote Spoon autant que possible)\n");
        out.append(String.format("  types     JDT=%d  Spoon=%d%n", jdt.types().size(), spoon.types().size()));
        out.append(String.format("  methodes  JDT=%d  Spoon=%d%n", jdt.methods().size(), spoon.methods().size()));
        out.append(String.format("  attributs JDT=%d  Spoon=%d%n",
                jdt.types().stream().mapToInt(TypeFact::fieldCount).sum(),
                spoon.types().stream().mapToInt(TypeFact::fieldCount).sum()));
        out.append(String.format("  paquetages JDT=%d Spoon=%d%n", jdt.packageCount(), spoon.packageCount()));
        long jdtCalls = jdt.invocationCalls().size();
        long spoonCalls = spoon.invocationCalls().size();
        out.append(String.format("  appels (invocation+super) JDT=%d  Spoon=%d%n", jdtCalls, spoonCalls));
        out.append("""
                Ecarts typiques, meme apres filtrage :
                  1. Spoon.getFields() compte les constantes d'enumeration (CtEnumValue) — filtrees ici.
                  2. Spoon expose les appels implicites super() des constructeurs — ignores via isImplicit().
                  3. Spoon.getAllTypes() ignore les types membres ; getElements(CtType) les retrouve.
                  4. Les identifiants Spoon utilisent '$' pour les types imbriques ; convertis ici en '.'.
                  5. noClasspath=true devine des cibles absentes (ex. StringUtils) que JDT laisse non resolues.
                """);
        return out.toString();
    }
}
