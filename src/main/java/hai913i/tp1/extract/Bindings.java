package hai913i.tp1.extract;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

/**
 * Noms stables (identifiants de méthodes, types effacés) à partir des bindings JDT.
 */
public final class Bindings {

    private Bindings() {
    }

    public static String typeName(ITypeBinding binding) {
        if (binding == null) {
            return "?";
        }
        ITypeBinding current = binding.getErasure();
        StringBuilder arrays = new StringBuilder();
        while (current.isArray()) {
            arrays.append("[]");
            current = current.getComponentType().getErasure();
        }
        String name = current.getQualifiedName();
        if (name == null || name.isEmpty()) {
            name = current.getName();
        }
        if (name == null || name.isEmpty()) {
            name = "?";
        }
        return name + arrays;
    }

    public static String methodId(IMethodBinding binding) {
        IMethodBinding declaration = binding.getMethodDeclaration();
        ITypeBinding declaring = declaration.getDeclaringClass();
        if (declaring == null) {
            return "?#" + declaration.getName();
        }
        ITypeBinding type = declaring.getErasure();
        String typeName = typeName(type);
        String name = declaration.isConstructor() ? simpleName(typeName) : declaration.getName();
        StringBuilder id = new StringBuilder();
        id.append(typeName).append('#').append(name).append('(');
        ITypeBinding[] params = declaration.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                id.append(',');
            }
            id.append(typeName(params[i]));
        }
        id.append(')');
        return id.toString();
    }

    public static String methodId(String typeName, MethodDeclaration node) {
        String name = node.isConstructor() ? simpleName(typeName) : node.getName().getIdentifier();
        List<String> params = parameterTypeNames(node);
        return methodId(typeName, name, params);
    }

    public static String methodId(String typeName, String name, List<String> parameterTypes) {
        return typeName + "#" + name + "(" + String.join(",", parameterTypes) + ")";
    }

    public static List<String> parameterTypeNames(MethodDeclaration node) {
        List<String> names = new ArrayList<>();
        for (Object parameter : node.parameters()) {
            SingleVariableDeclaration variable = (SingleVariableDeclaration) parameter;
            ITypeBinding binding = variable.getType().resolveBinding();
            String type = binding != null ? typeName(binding) : variable.getType().toString();
            if (variable.isVarargs()) {
                if (!type.endsWith("[]")) {
                    type = type + "[]";
                }
            }
            names.add(type);
        }
        return names;
    }

    public static String simpleName(String qualifiedName) {
        int last = Math.max(qualifiedName.lastIndexOf('.'), qualifiedName.lastIndexOf('$'));
        return last < 0 ? qualifiedName : qualifiedName.substring(last + 1);
    }
}
