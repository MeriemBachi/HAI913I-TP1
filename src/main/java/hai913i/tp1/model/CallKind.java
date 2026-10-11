package hai913i.tp1.model;

/** Nature du site d'appel. Seuls INVOCATION et SUPER entrent dans le graphe par défaut. */
public enum CallKind {
    INVOCATION,
    SUPER,
    CONSTRUCTOR,
    THIS_CONSTRUCTOR,
    SUPER_CONSTRUCTOR
}
