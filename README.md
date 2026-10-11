# HAI913I — TP1 Analyse statique

Analyseur statique Java en ligne de commande. Il lit un projet source, en extrait un **modèle de faits** (indépendant de JDT), calcule les **13 métriques** du sujet et construit le **graphe d’appel**.

## Projet

| Élément | Détail |
| --- | --- |
| Modules | un seul artefact Maven `hai913i-tp1-analyzer` |
| Entrée | un dossier de projet Java (racine `src/main/java`, sinon `src`, sinon le dossier lui-même) |
| Sorties | texte (structure, appels, métriques, graphe), DOT, interrogation appelantes/appelées |
| Analyse requise | Eclipse JDT Core 3.46.0 |
| Bonus | Spoon (`--action spoon`), DOT, HTML SVG cliquable (`--html`), JSON, CHA, filtre `--package`, couplage inter-classes, constructeurs |
| Limites | pas d’expansion virtuelle par défaut ; pas de réflexion / DI ; références de méthode (`Type::m`) ignorées ; appels hors méthode ignorés |

Paquetages :

```
hai913i.tp1.cli         arguments, codes de sortie, orchestration
hai913i.tp1.parse       racine des sources, ASTParser
hai913i.tp1.extract     visiteurs JDT (et Spoon) → modèle
hai913i.tp1.model       faits (aucune classe org.eclipse.jdt)
hai913i.tp1.metrics     questions 1–13
hai913i.tp1.callgraph   graphe, requêtes, DOT, option CHA
hai913i.tp1.report      mise en forme texte
```

## Environnement

| Outil | Version utilisée lors du développement |
| --- | --- |
| JDK | OpenJDK 17.0.16 (Temurin) — minimum 17 |
| Maven | 3.9.6 |
| JDT Core | 3.46.0 |
| Spoon | 11.5.0 |
| JUnit Jupiter | 5.14.4 |
| OS | Windows 10 |
| Éditeur | Cursor + terminal `mvn` / `java` |

Aucune interface graphique n’est requise. Graphviz est facultatif pour transformer un fichier DOT en image.

## Construction

Depuis une copie fraîche du dépôt (sans IDE) :

```text
mvn clean test
mvn clean package
```

Le JAR exécutable est `target/hai913i-tp1-analyzer.jar` (dépendances incluses ; signatures Eclipse exclues).

Si Maven n’est pas installé globalement, le binaire Apache Maven 3.9.6 peut être placé dans `.tools/apache-maven-3.9.6`.

## Exécution

```text
java -jar target/hai913i-tp1-analyzer.jar --help
```

Syntaxe :

```text
java -jar target/hai913i-tp1-analyzer.jar [options] <projet>
```

| Option | Rôle |
| --- | --- |
| `-a`, `--action` | `all` (défaut) · `structure` · `calls` · `metrics` · `graph` · `ast` · `spoon` |
| `-x`, `--threshold` | seuil X (obligatoire pour `all` et `metrics`, entier ≥ 0) |
| `-m`, `--method` | identifiant d’une méthode pour ses appelantes / appelées |
| `--constructors` | arcs `new` / `this(...)` / `super(...)` |
| `--cha` | ajoute les redéfinitions connues du projet (bonus C3) |
| `--dot fichier.dot` | export Graphviz |
| `--ast fichier.java` | dump de l’AST (étape A1) |
| `--classpath jars` | JAR du projet analysé, séparés par `;` |
| `--engine jdt\|spoon` | extracteur (défaut `jdt`) |

Sous Windows, entourez les chemins contenant des espaces de guillemets.

### Exemples

Point de contrôle A0 + métriques + graphe :

```text
java -jar target/hai913i-tp1-analyzer.jar -x 5 resources/validation
```

Structure seule :

```text
java -jar target/hai913i-tp1-analyzer.jar --action structure resources/validation
```

AST de `Dvd.java` :

```text
java -jar target/hai913i-tp1-analyzer.jar --action ast --ast src/library/model/Dvd.java resources/validation
```

Graphe + interrogation :

```text
java -jar target/hai913i-tp1-analyzer.jar --action graph --dot callgraph.dot -m "library.model.Loanable#checkOut(library.model.Member)" resources/validation
```

Bonus Spoon, HTML, couplage, filtre paquetage :

```text
java -jar target/hai913i-tp1-analyzer.jar --action spoon resources/validation
java -jar target/hai913i-tp1-analyzer.jar -x 5 --html analyse.html --json analyse.json resources/validation
java -jar target/hai913i-tp1-analyzer.jar --action coupling resources/validation
java -jar target/hai913i-tp1-analyzer.jar --action graph --package library.service --dot service.dot resources/validation
java -jar target/hai913i-tp1-analyzer.jar --action html --html graphe.html --package library.model resources/validation
```

Ouvrez `analyse.html` dans un navigateur : le graphe est un SVG autonome (sans Graphviz ni Internet). Un clic sur un nœud allume ses appelantes et appelées.

## Validation

Projet d’entrée : `resources/validation` (14 fichiers, médiathèque).

| Étape | Attendu (extrait) |
| --- | --- |
| A0 | 14 unités, 0 erreur |
| A2 | 15 types, 59 méthodes (9 constructeurs), 21 attributs |
| A3 | 94 appels (46 internes, 48 externes, 0 non résolu) |
| B2 | 493 LOC, 4 paquetages, moyennes 3,93 / 4,64 / 1,40 |
| B3 | 59 nœuds, 44 arcs, 46 sites internes |
| B3 + `--constructors` | 52 arcs, 56 sites |

Les tests JUnit reproduisent ces points : `mvn test`.

Robustesse :

```text
java -jar target/hai913i-tp1-analyzer.jar resources/robustness/no-java-files
java -jar target/hai913i-tp1-analyzer.jar --action structure resources/robustness/syntax-error
java -jar target/hai913i-tp1-analyzer.jar --action calls resources/robustness/missing-dep
java -jar target/hai913i-tp1-analyzer.jar -x abc resources/validation
```

Codes de sortie : `0` succès, `2` usage, `3` chemin invalide, `4` aucun `.java`, `5` seuil X illisible. Aucune pile d’exception n’est affichée pour ces cas.

## Dépannage

| Symptôme | Cause probable | Remède |
| --- | --- | --- |
| `Missing system library` | `setEnvironment` absent | conserver `JdtParser` du projet |
| ~50 % d’appels non résolus | racine = dossier projet au lieu de `src` | `ProjectSources` choisit `src/main/java` puis `src` |
| `SecurityException: Invalid signature file digest` | JAR Eclipse signés fusionnés | filtres shade dans `pom.xml` |
| `Unites de compilation : 0` | mauvais chemin | passer le dossier du projet, pas un fichier |
| Spoon affiche plus d’appels | appels implicites / conventions différentes | `--action spoon` imprime la comparaison |
| Graphviz introuvable | visualisation facultative | le fichier DOT reste lisible tel quel |

## Identifiants de méthodes

Format stable, y compris pour les surcharges :

```text
library.service.Catalog#add(library.model.Item)
library.service.Catalog#add(library.model.Item,library.model.Item)
```

Les types imbriqués utilisent des points : `library.service.LoanService.Loan#getItem()`.
