package hai913i.tp1;

import hai913i.tp1.cli.Analyzer;

/**
 * Point d'entrée : lit les arguments et délègue. Aucun calcul métier ici.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(Analyzer.run(args, System.out, System.err));
    }
}
