package hai913i.tp1.model;

/** Erreur (ou avertissement) signalée par le parseur, indépendante de JDT. */
public record CompilationIssue(String fileName, int line, String message, boolean error) {
}
