package hai913i.tp1.extract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.model.TypeKind;
import hai913i.tp1.model.Visibility;

/**
 * Extrait la structure (types, attributs, méthodes) en lisant {@code bodyDeclarations()}
 * plutôt que par une visite récursive, pour ne pas attribuer les membres imbriqués au type parent.
 */
public final class StructureVisitor extends ASTVisitor {

    private final Map<String, TypeDraft> drafts = new LinkedHashMap<>();

    @Override
    public boolean visit(CompilationUnit unit) {
        String packageName = unit.getPackage() == null
                ? "(défaut)"
                : unit.getPackage().getName().getFullyQualifiedName();
        for (Object type : unit.types()) {
            if (type instanceof AbstractTypeDeclaration declaration) {
                extractType(declaration, packageName, unit);
            }
        }
        return false;
    }

    private void extractType(AbstractTypeDeclaration type, String packageName, CompilationUnit unit) {
        if (!CountedTypes.isCounted(type)) {
            return;
        }
        String qualifiedName = CountedTypes.qualifiedName(type, packageName);
        TypeKind kind = CountedTypes.kind(type);
        List<String> superclasses = superclasses(type);
        List<String> interfaces = interfaces(type);
        List<FieldFact> fields = new ArrayList<>();
        List<MethodFact> methods = new ArrayList<>();

        for (Object declaration : type.bodyDeclarations()) {
            if (declaration instanceof FieldDeclaration field) {
                fields.addAll(fieldsOf(field, CountedTypes.isInterface(type)));
            } else if (declaration instanceof MethodDeclaration method) {
                methods.add(methodOf(method, qualifiedName, unit, CountedTypes.isInterface(type)));
            } else if (declaration instanceof AbstractTypeDeclaration nested) {
                extractType(nested, packageName, unit);
            }
        }
        drafts.put(qualifiedName, new TypeDraft(qualifiedName, packageName, kind, superclasses, interfaces, fields,
                methods));
    }

    private static List<FieldFact> fieldsOf(FieldDeclaration field, boolean interfaceMember) {
        String declaredType;
        ITypeBinding binding = field.getType().resolveBinding();
        declaredType = binding != null ? Bindings.typeName(binding) : field.getType().toString();
        Visibility visibility = visibility(field.getModifiers(), interfaceMember);
        List<FieldFact> facts = new ArrayList<>();
        for (Object fragment : field.fragments()) {
            VariableDeclarationFragment variable = (VariableDeclarationFragment) fragment;
            facts.add(new FieldFact(variable.getName().getIdentifier(), declaredType, visibility));
        }
        return facts;
    }

    private static MethodFact methodOf(MethodDeclaration method, String typeName, CompilationUnit unit,
            boolean interfaceMember) {
        List<String> parameters;
        String id;
        var binding = method.resolveBinding();
        if (binding != null && !binding.isRecovered()) {
            id = Bindings.methodId(binding);
            parameters = new ArrayList<>();
            for (var param : binding.getMethodDeclaration().getParameterTypes()) {
                parameters.add(Bindings.typeName(param));
            }
        } else {
            parameters = Bindings.parameterTypeNames(method);
            id = Bindings.methodId(typeName, method);
        }
        String name = method.isConstructor() ? Bindings.simpleName(typeName) : method.getName().getIdentifier();
        int bodyLoc = bodyLoc(method, unit);
        Visibility visibility = visibility(method.getModifiers(), interfaceMember);
        return new MethodFact(id, name, parameters, method.isConstructor(), bodyLoc, visibility, List.of());
    }

    static int bodyLoc(MethodDeclaration method, CompilationUnit unit) {
        if (method.getBody() == null) {
            return 0;
        }
        int start = method.getBody().getStartPosition();
        int end = start + method.getBody().getLength() - 1;
        int startLine = unit.getLineNumber(start);
        int endLine = unit.getLineNumber(end);
        if (startLine < 0 || endLine < 0) {
            return 0;
        }
        return endLine - startLine + 1;
    }

    private static List<String> superclasses(AbstractTypeDeclaration type) {
        ITypeBinding binding = type.resolveBinding();
        List<String> chain = new ArrayList<>();
        if (binding == null) {
            if (type instanceof TypeDeclaration declaration && declaration.getSuperclassType() != null) {
                chain.add(declaration.getSuperclassType().toString());
            }
            return chain;
        }
        ITypeBinding current = binding.getSuperclass();
        while (current != null) {
            String name = Bindings.typeName(current);
            if ("java.lang.Object".equals(name)) {
                break;
            }
            chain.add(name);
            current = current.getSuperclass();
        }
        return chain;
    }

    private static List<String> interfaces(AbstractTypeDeclaration type) {
        ITypeBinding binding = type.resolveBinding();
        List<String> names = new ArrayList<>();
        if (binding == null) {
            if (type instanceof TypeDeclaration declaration) {
                for (Object superInterface : declaration.superInterfaceTypes()) {
                    names.add(superInterface.toString());
                }
            } else if (type instanceof EnumDeclaration enumeration) {
                for (Object superInterface : enumeration.superInterfaceTypes()) {
                    names.add(superInterface.toString());
                }
            }
            return names;
        }
        for (ITypeBinding superInterface : binding.getInterfaces()) {
            names.add(Bindings.typeName(superInterface));
        }
        return names;
    }

    private static Visibility visibility(int modifiers, boolean interfaceMember) {
        if (Modifier.isPublic(modifiers)) {
            return Visibility.PUBLIC;
        }
        if (Modifier.isProtected(modifiers)) {
            return Visibility.PROTECTED;
        }
        if (Modifier.isPrivate(modifiers)) {
            return Visibility.PRIVATE;
        }
        if (interfaceMember) {
            return Visibility.PUBLIC_IMPLICIT;
        }
        return Visibility.PACKAGE;
    }

    public List<TypeFact> types() {
        List<TypeFact> result = new ArrayList<>();
        for (TypeDraft draft : drafts.values()) {
            result.add(new TypeFact(draft.qualifiedName, draft.packageName, draft.kind, draft.superclasses,
                    draft.interfaces, draft.fields, draft.methods));
        }
        return result;
    }

    private record TypeDraft(String qualifiedName, String packageName, TypeKind kind, List<String> superclasses,
            List<String> interfaces, List<FieldFact> fields, List<MethodFact> methods) {
    }
}
