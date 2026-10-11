package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.jdt.core.compiler.IProblem;
import org.junit.jupiter.api.Test;

import hai913i.tp1.extract.AstPrinter;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;

class CheckpointA0A1Test {

    @Test
    void a0_validationHas14UnitsAndNoError() throws Exception {
        ProjectSources sources = ProjectSources.of(Fixtures.validation());
        List<ParsedFile> files = JdtParser.parse(sources, List.of());
        assertEquals(14, files.size());
        long errors = files.stream()
                .flatMap(f -> java.util.Arrays.stream(f.unit().getProblems()))
                .filter(IProblem::isError)
                .count();
        assertEquals(0, errors);
    }

    @Test
    void a1_dvdTreeContainsExpectedNodes() throws Exception {
        String tree = astNamed("Dvd.java");
        assertTrue(tree.contains("TypeDeclaration"), tree);
        assertTrue(tree.contains("FieldDeclaration"), tree);
        assertEquals(3, count(tree, "MethodDeclaration"));
        assertTrue(tree.contains("SuperConstructorInvocation"), tree);
    }

    @Test
    void a1_categoryIsEnumDeclaration() throws Exception {
        String tree = astNamed("Category.java");
        assertTrue(tree.contains("EnumDeclaration"), tree);
        assertTrue(!tree.trim().startsWith("TypeDeclaration"),
                "visit(TypeDeclaration) ne voit pas une enumeration");
    }

    private static String astNamed(String fileName) throws Exception {
        ProjectSources sources = ProjectSources.of(Fixtures.validation());
        for (ParsedFile file : JdtParser.parse(sources, List.of())) {
            if (file.path().getFileName().toString().equals(fileName)) {
                AstPrinter printer = new AstPrinter();
                file.unit().accept(printer);
                return printer.text();
            }
        }
        throw new AssertionError("fichier introuvable : " + fileName);
    }

    private static int count(String tree, String node) {
        int n = 0;
        int from = 0;
        while (true) {
            int at = tree.indexOf(node, from);
            if (at < 0) {
                return n;
            }
            n++;
            from = at + node.length();
        }
    }
}
