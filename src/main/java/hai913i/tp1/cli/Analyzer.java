package hai913i.tp1.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.extract.AstPrinter;
import hai913i.tp1.extract.FactExtractor;
import hai913i.tp1.extract.SpoonFactExtractor;
import hai913i.tp1.metrics.ClassCoupling;
import hai913i.tp1.metrics.MetricsReport;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.report.HtmlReport;
import hai913i.tp1.report.JsonReport;
import hai913i.tp1.report.TextReport;

public final class Analyzer {

    private Analyzer() {
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            Arguments arguments = Arguments.parse(args);
            if (arguments.help()) {
                out.print(Arguments.usage());
                return 0;
            }
            Path project = arguments.project().toAbsolutePath().normalize();
            if (!Files.exists(project)) {
                err.println("Erreur : chemin inexistant : " + project);
                return Arguments.EXIT_BAD_PATH;
            }
            ProjectSources sources;
            try {
                sources = ProjectSources.of(project);
            } catch (IllegalArgumentException e) {
                err.println("Erreur : " + e.getMessage());
                return Arguments.EXIT_BAD_PATH;
            } catch (IOException e) {
                err.println("Erreur : impossible de lire " + project + " (" + e.getMessage() + ")");
                return Arguments.EXIT_BAD_PATH;
            }
            if (sources.javaFiles().isEmpty()) {
                err.println("Erreur : aucun fichier .java dans " + sources.sourceRoot());
                return Arguments.EXIT_NO_JAVA;
            }
            if (("metrics".equals(arguments.action()) || "all".equals(arguments.action()))
                    && arguments.threshold() == null) {
                err.println("Le seuil X est obligatoire pour les metriques (exemple : -x 5). Une valeur entiere >= 0 est exigee.");
                return Arguments.EXIT_BAD_X;
            }

            List<ParsedFile> parsed = JdtParser.parse(sources, arguments.classpath());
            ProjectFacts jdtFacts = FactExtractor.extract(sources, parsed);

            if ("ast".equals(arguments.action()) || arguments.astFile() != null) {
                printAst(arguments, sources, parsed, out, err);
                if ("ast".equals(arguments.action())) {
                    return 0;
                }
            }

            ProjectFacts facts = "spoon".equals(arguments.engine())
                    ? SpoonFactExtractor.extract(sources, true)
                    : jdtFacts;

            MetricsReport metrics = new MetricsReport(facts, arguments.thresholdOrDefault());
            CallGraph graph = CallGraph.build(facts, arguments.constructors(), arguments.cha())
                    .restrictToPackage(arguments.packageFilter());
            ClassCoupling coupling = ClassCoupling.of(graph);

            switch (arguments.action()) {
                case "structure" -> out.print(TextReport.environment(facts) + TextReport.issues(facts)
                        + TextReport.structure(facts));
                case "calls" -> out.print(TextReport.environment(facts) + TextReport.issues(facts)
                        + TextReport.calls(facts));
                case "metrics" -> out.print(TextReport.environment(facts) + metrics.asText());
                case "graph" -> {
                    out.print(TextReport.environment(facts) + graph.asText());
                    if (arguments.methodId() != null) {
                        out.print('\n' + graph.query(arguments.methodId()));
                    }
                }
                case "coupling" -> {
                    out.print(TextReport.environment(facts));
                    out.print(coupling.asText());
                    out.print(ClassCoupling.inheritanceDepths(facts.types()));
                }
                case "html" -> {
                    Path html = arguments.htmlFile() != null ? arguments.htmlFile() : Path.of("analyse.html");
                    Files.writeString(html, HtmlReport.render(facts, metrics, graph));
                    out.println("Rapport HTML ecrit dans " + html.toAbsolutePath());
                }
                case "spoon" -> {
                    ProjectFacts spoon = SpoonFactExtractor.extract(sources, true);
                    out.print(TextReport.environment(jdtFacts));
                    out.print(SpoonFactExtractor.compare(jdtFacts, spoon));
                    out.print('\n');
                    out.print(TextReport.structure(spoon));
                }
                default -> out.print(TextReport.full(facts, metrics, graph, arguments.methodId())
                        + "\n" + coupling.asText()
                        + ClassCoupling.inheritanceDepths(facts.types()));
            }

            if (arguments.dotFile() != null) {
                Files.writeString(arguments.dotFile(), graph.toDot());
                out.println("Graphe DOT ecrit dans " + arguments.dotFile().toAbsolutePath());
            }
            if (arguments.htmlFile() != null && !"html".equals(arguments.action())) {
                Files.writeString(arguments.htmlFile(), HtmlReport.render(facts, metrics, graph));
                out.println("Rapport HTML ecrit dans " + arguments.htmlFile().toAbsolutePath());
            }
            if (arguments.jsonFile() != null) {
                Files.writeString(arguments.jsonFile(), JsonReport.render(facts, metrics, graph));
                out.println("JSON ecrit dans " + arguments.jsonFile().toAbsolutePath());
            }
            return 0;
        } catch (UsageException e) {
            err.println(e.getMessage());
            return e.exitCode();
        } catch (IOException e) {
            err.println("Erreur d'entree/sortie : " + e.getMessage());
            return Arguments.EXIT_BAD_PATH;
        }
    }

    private static void printAst(Arguments arguments, ProjectSources sources, List<ParsedFile> parsed,
            PrintStream out, PrintStream err) {
        Path wanted = arguments.astFile();
        if (wanted == null) {
            return;
        }
        Path normalized = wanted.isAbsolute() ? wanted.normalize() : sources.projectDirectory().resolve(wanted).normalize();
        ParsedFile match = null;
        for (ParsedFile file : parsed) {
            if (file.path().normalize().equals(normalized)
                    || file.path().endsWith(wanted)
                    || file.path().getFileName().equals(wanted.getFileName())) {
                match = file;
                break;
            }
        }
        if (match == null) {
            err.println("Fichier introuvable pour --ast : " + wanted);
            return;
        }
        AstPrinter printer = new AstPrinter();
        match.unit().accept(printer);
        out.println("AST de " + match.path());
        out.print(printer.text());
    }
}
