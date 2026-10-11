package hai913i.tp1.extract;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Affiche tous les nœuds d'un AST, indentés selon la profondeur (étape A1).
 * Une étiquette courte (nom de type, de méthode, d'appel) rend l'arbre lisible
 * sans changer le parcours : {@code visit} n'élague rien.
 */
public final class AstPrinter extends ASTVisitor {

    private final StringBuilder out = new StringBuilder();
    private int depth;

    public AstPrinter() {
        super(true);
    }

    @Override
    public boolean preVisit2(ASTNode node) {
        out.append("  ".repeat(depth))
                .append(node.getClass().getSimpleName())
                .append(label(node))
                .append('\n');
        depth++;
        return true;
    }

    @Override
    public void postVisit(ASTNode node) {
        depth--;
    }

    private static String label(ASTNode node) {
        if (node instanceof TypeDeclaration type) {
            return " " + type.getName().getIdentifier()
                    + (type.isInterface() ? " [interface]" : " [classe]");
        }
        if (node instanceof EnumDeclaration enumeration) {
            return " " + enumeration.getName().getIdentifier() + " [enumeration]";
        }
        if (node instanceof MethodDeclaration method) {
            return " " + method.getName().getIdentifier()
                    + (method.isConstructor() ? " [constructeur]" : "")
                    + (method.getBody() == null ? " [sans corps]" : "");
        }
        if (node instanceof FieldDeclaration field) {
            String names = "";
            for (Object fragment : field.fragments()) {
                names += ((VariableDeclarationFragment) fragment).getName().getIdentifier() + " ";
            }
            return " " + names.trim();
        }
        if (node instanceof MethodInvocation invocation) {
            return " " + invocation.getName().getIdentifier() + "()";
        }
        if (node instanceof SuperMethodInvocation invocation) {
            return " super." + invocation.getName().getIdentifier() + "()";
        }
        if (node instanceof SuperConstructorInvocation) {
            return " super(...)";
        }
        if (node instanceof SimpleName name) {
            return " " + name.getIdentifier();
        }
        if (node instanceof SimpleType type) {
            return " " + type.getName();
        }
        return "";
    }

    public String text() {
        return out.toString();
    }
}
