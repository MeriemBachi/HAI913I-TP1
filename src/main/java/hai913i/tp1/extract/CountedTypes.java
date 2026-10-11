package hai913i.tp1.extract;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import hai913i.tp1.model.TypeKind;

/** Décide quels types et quelles méthodes comptent selon le § 4.1. */
public final class CountedTypes {

    private CountedTypes() {
    }

    public static boolean isCounted(AbstractTypeDeclaration type) {
        if (type instanceof AnnotationTypeDeclaration) {
            return false;
        }
        if (type instanceof TypeDeclaration declaration && declaration.isLocalTypeDeclaration()) {
            return false;
        }
        return type.getName() != null && !type.getName().getIdentifier().isEmpty();
    }

    public static boolean isCountedMethod(MethodDeclaration method) {
        ASTNode parent = method.getParent();
        if (parent instanceof AnonymousClassDeclaration) {
            return false;
        }
        if (parent instanceof AbstractTypeDeclaration type) {
            return isCounted(type);
        }
        return false;
    }

    public static TypeKind kind(AbstractTypeDeclaration type) {
        if (type instanceof EnumDeclaration) {
            return TypeKind.ENUM;
        }
        if (type instanceof RecordDeclaration) {
            return TypeKind.RECORD;
        }
        if (type instanceof TypeDeclaration declaration && declaration.isInterface()) {
            return TypeKind.INTERFACE;
        }
        return TypeKind.CLASS;
    }

    public static String qualifiedName(AbstractTypeDeclaration type, String packageName) {
        ITypeBinding binding = type.resolveBinding();
        if (binding != null) {
            String name = binding.getQualifiedName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        String simple = type.getName().getIdentifier();
        ASTNode parent = type.getParent();
        if (parent instanceof AbstractTypeDeclaration enclosing && isCounted(enclosing)) {
            return qualifiedName(enclosing, packageName) + "." + simple;
        }
        if (packageName.isEmpty() || "(défaut)".equals(packageName)) {
            return simple;
        }
        return packageName + "." + simple;
    }

    public static boolean isInterface(AbstractTypeDeclaration type) {
        return type instanceof TypeDeclaration declaration && declaration.isInterface();
    }
}
