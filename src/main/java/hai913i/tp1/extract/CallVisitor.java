package hai913i.tp1.extract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.CallKind;

/**
 * Collecte les sites d'appel (MethodInvocation, SuperMethodInvocation, et optionnellement
 * les constructeurs) et les rattache à la méthode comptée englobante.
 */
public final class CallVisitor extends ASTVisitor {

    private final CompilationUnit unit;
    private final Set<String> projectMethodIds;
    private final Map<String, List<CallFact>> callsByCaller = new LinkedHashMap<>();

    public CallVisitor(CompilationUnit unit, Set<String> projectMethodIds) {
        this.unit = unit;
        this.projectMethodIds = projectMethodIds;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        addInvocation(node, node.resolveMethodBinding(), node.getExpression(), node.getName().getIdentifier(),
                CallKind.INVOCATION);
        return true;
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        addInvocation(node, node.resolveMethodBinding(), null, node.getName().getIdentifier(), CallKind.SUPER);
        return true;
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        String name = node.getType().toString();
        addInvocation(node, node.resolveConstructorBinding(), node.getType(), name, CallKind.CONSTRUCTOR);
        return true;
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        addInvocation(node, node.resolveConstructorBinding(), null, "this", CallKind.THIS_CONSTRUCTOR);
        return true;
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        addInvocation(node, node.resolveConstructorBinding(), null, "super", CallKind.SUPER_CONSTRUCTOR);
        return true;
    }

    private void addInvocation(ASTNode node, IMethodBinding binding, ASTNode receiverExpression, String fallbackName,
            CallKind kind) {
        MethodDeclaration enclosing = enclosingCountedMethod(node);
        if (enclosing == null) {
            return;
        }
        String callerId = callerId(enclosing);
        boolean resolved = binding != null && !binding.isRecovered() && binding.getDeclaringClass() != null;
        Optional<String> targetId = Optional.empty();
        boolean internal = false;
        String targetName = fallbackName;
        if (resolved) {
            targetId = Optional.of(Bindings.methodId(binding));
            targetName = binding.getName();
            internal = projectMethodIds.contains(targetId.get());
        }
        String receiver = receiverType(node, kind, receiverExpression, binding, enclosing);
        CallFact fact = new CallFact(callerId, targetId, targetName, receiver, lineOf(node), resolved, internal, kind);
        callsByCaller.computeIfAbsent(callerId, key -> new ArrayList<>()).add(fact);
    }

    private String receiverType(ASTNode node, CallKind kind, ASTNode receiverExpression, IMethodBinding binding,
            MethodDeclaration enclosing) {
        if (kind == CallKind.SUPER) {
            ITypeBinding enclosingType = enclosingType(enclosing);
            if (enclosingType != null && enclosingType.getSuperclass() != null) {
                return Bindings.typeName(enclosingType.getSuperclass());
            }
            return "?";
        }
        if (kind == CallKind.SUPER_CONSTRUCTOR) {
            ITypeBinding enclosingType = enclosingType(enclosing);
            if (enclosingType != null && enclosingType.getSuperclass() != null) {
                return Bindings.typeName(enclosingType.getSuperclass());
            }
            return "?";
        }
        if (kind == CallKind.THIS_CONSTRUCTOR) {
            ITypeBinding enclosingType = enclosingType(enclosing);
            return enclosingType == null ? "?" : Bindings.typeName(enclosingType);
        }
        if (receiverExpression != null && kind == CallKind.INVOCATION) {
            ITypeBinding type = ((org.eclipse.jdt.core.dom.Expression) receiverExpression).resolveTypeBinding();
            return type == null ? "?" : Bindings.typeName(type);
        }
        if (receiverExpression != null && kind == CallKind.CONSTRUCTOR) {
            if (binding != null && binding.getDeclaringClass() != null) {
                return Bindings.typeName(binding.getDeclaringClass());
            }
            return receiverExpression.toString();
        }
        // Sans receveur : this implicite, ou type déclarant si la cible est statique.
        if (binding != null && Modifier.isStatic(binding.getModifiers()) && binding.getDeclaringClass() != null) {
            return Bindings.typeName(binding.getDeclaringClass());
        }
        ITypeBinding enclosingType = enclosingType(enclosing);
        return enclosingType == null ? "?" : Bindings.typeName(enclosingType);
    }

    private static ITypeBinding enclosingType(MethodDeclaration method) {
        ASTNode parent = method.getParent();
        if (parent instanceof AbstractTypeDeclaration type) {
            return type.resolveBinding();
        }
        return null;
    }

    private String callerId(MethodDeclaration method) {
        var binding = method.resolveBinding();
        if (binding != null && !binding.isRecovered()) {
            return Bindings.methodId(binding);
        }
        AbstractTypeDeclaration type = (AbstractTypeDeclaration) method.getParent();
        String packageName = unit.getPackage() == null
                ? "(défaut)"
                : unit.getPackage().getName().getFullyQualifiedName();
        String typeName = CountedTypes.qualifiedName(type, packageName);
        return Bindings.methodId(typeName, method);
    }

    private static MethodDeclaration enclosingCountedMethod(ASTNode node) {
        ASTNode current = node.getParent();
        while (current != null) {
            if (current instanceof MethodDeclaration method && CountedTypes.isCountedMethod(method)) {
                return method;
            }
            current = current.getParent();
        }
        return null;
    }

    private int lineOf(ASTNode node) {
        return unit.getLineNumber(node.getStartPosition());
    }

    public Map<String, List<CallFact>> callsByCaller() {
        return callsByCaller;
    }
}
