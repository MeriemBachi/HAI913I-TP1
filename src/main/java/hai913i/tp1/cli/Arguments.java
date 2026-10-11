package hai913i.tp1.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Arguments {

    public static final int EXIT_USAGE = 2;
    public static final int EXIT_BAD_PATH = 3;
    public static final int EXIT_NO_JAVA = 4;
    public static final int EXIT_BAD_X = 5;

    private Path project;
    private String action = "all";
    private Integer threshold;
    private String methodId;
    private boolean constructors;
    private boolean cha;
    private Path dotFile;
    private Path astFile;
    private Path htmlFile;
    private Path jsonFile;
    private String packageFilter;
    private final List<String> classpath = new ArrayList<>();
    private String engine = "jdt";
    private boolean help;

    public Path project() {
        return project;
    }

    public String action() {
        return action;
    }

    public Integer threshold() {
        return threshold;
    }

    public int thresholdOrDefault() {
        return threshold == null ? 5 : threshold;
    }

    public String methodId() {
        return methodId;
    }

    public boolean constructors() {
        return constructors;
    }

    public boolean cha() {
        return cha;
    }

    public Path dotFile() {
        return dotFile;
    }

    public Path astFile() {
        return astFile;
    }

    public Path htmlFile() {
        return htmlFile;
    }

    public Path jsonFile() {
        return jsonFile;
    }

    public String packageFilter() {
        return packageFilter;
    }

    public List<String> classpath() {
        return List.copyOf(classpath);
    }

    public String engine() {
        return engine;
    }

    public boolean help() {
        return help;
    }

    public static Arguments parse(String[] args) {
        Arguments parsed = new Arguments();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h", "--help" -> parsed.help = true;
                case "-a", "--action" -> parsed.action = requireValue(args, ++i, arg).toLowerCase(Locale.ROOT);
                case "-x", "--threshold" -> parsed.threshold = parseX(requireValue(args, ++i, arg));
                case "-m", "--method" -> parsed.methodId = requireValue(args, ++i, arg);
                case "--constructors" -> parsed.constructors = true;
                case "--cha" -> parsed.cha = true;
                case "--dot" -> parsed.dotFile = Path.of(requireValue(args, ++i, arg));
                case "--html" -> parsed.htmlFile = Path.of(requireValue(args, ++i, arg));
                case "--json" -> parsed.jsonFile = Path.of(requireValue(args, ++i, arg));
                case "--package" -> parsed.packageFilter = requireValue(args, ++i, arg);
                case "--ast" -> parsed.astFile = Path.of(requireValue(args, ++i, arg));
                case "--classpath" -> parsed.classpath.addAll(splitClasspath(requireValue(args, ++i, arg)));
                case "--engine" -> parsed.engine = requireValue(args, ++i, arg).toLowerCase(Locale.ROOT);
                default -> {
                    if (arg.startsWith("-")) {
                        throw new UsageException(EXIT_USAGE, "Option inconnue : " + arg + "\n" + usage());
                    }
                    if (parsed.project != null) {
                        throw new UsageException(EXIT_USAGE, "Un seul chemin de projet est attendu.\n" + usage());
                    }
                    parsed.project = Path.of(arg);
                }
            }
        }
        if (parsed.help) {
            return parsed;
        }
        if (parsed.project == null) {
            throw new UsageException(EXIT_USAGE, "Le chemin du projet a analyser est obligatoire.\n" + usage());
        }
        if (!List.of("all", "structure", "calls", "metrics", "graph", "ast", "spoon", "html", "coupling")
                .contains(parsed.action)) {
            throw new UsageException(EXIT_USAGE, "Action inconnue : " + parsed.action + "\n" + usage());
        }
        if (!List.of("jdt", "spoon").contains(parsed.engine)) {
            throw new UsageException(EXIT_USAGE, "Moteur inconnu : " + parsed.engine + " (jdt ou spoon)");
        }
        if ("ast".equals(parsed.action) && parsed.astFile == null) {
            throw new UsageException(EXIT_USAGE, "L'action ast exige --ast FICHIER.java");
        }
        return parsed;
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new UsageException(EXIT_USAGE, "Valeur manquante pour " + option);
        }
        return args[index];
    }

    private static int parseX(String raw) {
        try {
            int value = Integer.parseInt(raw);
            if (value < 0) {
                throw new UsageException(EXIT_BAD_X, "Le seuil X doit etre un entier positif ou nul, recu : " + raw);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new UsageException(EXIT_BAD_X, "Le seuil X doit etre un entier, recu : " + raw);
        }
    }

    private static List<String> splitClasspath(String raw) {
        String[] parts = raw.split("[;,]");
        List<String> jars = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                jars.add(part.trim());
            }
        }
        return jars;
    }

    public static String usage() {
        return """
                Usage : java -jar hai913i-tp1-analyzer.jar [options] <projet>

                Options :
                  -h, --help                 affiche cette aide
                  -a, --action <nom>         all | structure | calls | metrics | graph | ast | spoon
                                             | html | coupling
                  -x, --threshold <n>        seuil X (obligatoire pour all et metrics, entier >= 0)
                  -m, --method <id>          interroge appelantes / appelees (impact)
                  --constructors             ajoute les arcs vers les constructeurs
                  --cha                      expansion des appels virtuels (redéfinitions du projet)
                  --package <nom>            filtre le graphe (visualisation C2)
                  --dot <fichier>            exporte le graphe au format DOT
                  --html <fichier>           rapport HTML autonome (graphe SVG cliquable)
                  --json <fichier>           export JSON du modele
                  --ast <fichier.java>       affiche l'AST d'un fichier (etape A1)
                  --classpath <jars>         JAR du projet analyse, separes par ;
                  --engine jdt|spoon         extracteur (defaut : jdt)

                Exemples :
                  java -jar target/hai913i-tp1-analyzer.jar -x 5 resources/validation
                  java -jar target/hai913i-tp1-analyzer.jar --action structure resources/validation
                  java -jar target/hai913i-tp1-analyzer.jar --action ast --ast src/library/model/Dvd.java resources/validation
                """;
    }
}
