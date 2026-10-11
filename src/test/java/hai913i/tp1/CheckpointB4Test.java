package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import hai913i.tp1.cli.Analyzer;
import hai913i.tp1.cli.Arguments;

class CheckpointB4Test {

    @Test
    void missingPath() {
        Result result = run("C:\\chemin\\qui\\nexiste\\pas\\tp1");
        assertEquals(Arguments.EXIT_BAD_PATH, result.code);
        assertTrue(result.err.contains("inexistant"), result.err);
        assertFalse(result.err.contains("Exception"), result.err);
    }

    @Test
    void noJavaFiles() {
        Result result = run(Fixtures.robustness("no-java-files").toString());
        assertEquals(Arguments.EXIT_NO_JAVA, result.code);
        assertTrue(result.err.toLowerCase().contains("aucun fichier"), result.err);
    }

    @Test
    void malformedX() {
        Result result = run("-x", "abc", Fixtures.validation().toString());
        assertEquals(Arguments.EXIT_BAD_X, result.code);
        assertTrue(result.err.contains("entier"), result.err);
    }

    @Test
    void missingXForMetrics() {
        Result result = run("--action", "metrics", Fixtures.validation().toString());
        assertEquals(Arguments.EXIT_BAD_X, result.code);
    }

    @Test
    void syntaxErrorIsReportedAndHelperIsAnalysed() {
        Result result = run("-x", "5", "--action", "structure", Fixtures.robustness("syntax-error").toString());
        assertEquals(0, result.code);
        assertTrue(result.out.contains("Broken.java") || result.out.contains("Erreurs"), result.out);
        assertTrue(result.out.contains("demo.Helper") || result.out.contains("Helper"), result.out);
    }

    @Test
    void helpDoesNotNeedProject() {
        Result result = run("--help");
        assertEquals(0, result.code);
        assertTrue(result.out.contains("Usage"));
    }

    private static Result run(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int code = Analyzer.run(args, new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(code, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private record Result(int code, String out, String err) {
    }
}
