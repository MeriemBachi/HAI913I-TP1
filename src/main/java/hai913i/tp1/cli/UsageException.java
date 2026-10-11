package hai913i.tp1.cli;

/** Erreur d'utilisation de la ligne de commande, avec code de sortie. */
public final class UsageException extends RuntimeException {

    private final int exitCode;

    public UsageException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
