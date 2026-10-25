# BACKLOG — Corrections issues de l'audit de « Calc »

> Document de pilotage de l'implémentation. Il couvre **l'intégralité** des constats de l'audit (problèmes avérés, risques, préférences d'architecture, points non vérifiés) et exclut explicitement les trois idées d'évolution (voir la section « Évolutions futures »).

## 1. Contexte

| Élément | Valeur |
|---|---|
| Projet | « Calc » — calculatrice desktop Java/Swing en français |
| Dépôt | `TGDev2/Calculatrice`, branche `main`, 3 commits (dernier : `46ffb7d`) |
| Dossier local | `C:\Users\DIEUTAXI\Documents\Code\Calc_portfolio` |
| Code audité | 25 fichiers, ≈ 2 200 lignes : `Calcul/` (moteur), `Graphique/` (présentateur et vue Swing), `Tests/`, 3 scripts PowerShell, README |
| Vérification de correspondance | Le contenu du dépôt cloné est identique à l'archive auditée (hash identique sur les 25 fichiers, fins de ligne ignorées) |
| Référence de non-régression | 4 classes de test, **23 méthodes de test, 92 assertions** (moteur 14, présentateur 33, persistance 17, interface 28), toutes vertes sous JDK 21 |
| Note sur les fins de ligne | L'index Git est en LF et le dossier de travail en CRLF. Un Git sans `autocrlf` (shell Linux, CI) affiche donc les 25 fichiers comme « modifiés » alors que seules les fins de ligne diffèrent. L'Epic E1 ajoute un `.gitattributes` pour l'éviter. |

## 2. Mode d'emploi

1. **Avant l'Epic E0**, committe ce fichier (il est créé sans être suivi par Git) :
   ```powershell
   cd C:\Users\DIEUTAXI\Documents\Code\Calc_portfolio
   git add BACKLOG.md
   git commit -m "docs: add correction backlog"
   ```
2. Ouvre Claude Code dans le dossier du projet (`claude`).
3. Suis les Epics **dans l'ordre numérique** (c'est l'ordre recommandé : priorité d'abord, dépendances respectées).
4. Pour chaque Epic : sélectionne le modèle indiqué avec `/model`, vérifie que les prérequis sont remplis, colle la commande, relis le plan que Claude Code écrit dans `tasks/todo.md` avant qu'il ne code, puis **lance toi-même la validation** de l'Epic.
5. Si tout est vert, fusionne la branche de l'Epic dans `main` (`git switch main` puis `git merge --ff-only <branche>`), fais `/clear`, et passe à l'Epic suivant. Chaque commande est autonome : elle n'a pas besoin du contexte des Epics précédents.
6. Si un Epic échoue, supprime simplement sa branche : `main` n'a pas bougé.

> Chaque Epic travaille sur sa propre branche `epic/eN-…`, ce qui rend la validation et le retour arrière sans risque. Claude Code ne fait jamais de `push`.

## 3. Vue d'ensemble

| Epic | Titre | Priorité | Modèle | Dépend de | Effort estimé* |
|---|---|---|---|---|---|
| E0 | Baseline et vérification de l'environnement | Prérequis | Haiku | — | 15 min |
| E1 | Socle de build, de tests et d'intégration continue | Haute (facilitateur) | Sonnet | E0 | ½–1 j |
| E2 | Historique : ne plus jamais rien perdre | **Critique** | Sonnet | E1 | ½ j |
| E3 | Erreurs typées : champ fautif explicite | Moyenne | Haiku | E1 | 1 h |
| E4 | Bornes de taille : fin du gel de l'interface | **Haute** | Sonnet | E2, E3 | 3–4 h |
| E5 | Politique d'arrondi : plus de résultat non nul affiché « 0 » | Moyenne | **Opus** | E3, E4 | ½ j |
| E6 | Persistance : écriture atomique et format versionné | Moyenne | Sonnet | E2 | 2–3 h |
| E7 | Saisie numérique : espaces, milliers, ambiguïté du point | Basse | Haiku | E3, E4 | 1–2 h |
| E8 | Interface : accents, apparence système, accessibilité | Basse | Sonnet | E2 à E7 | 3–4 h |
| E9 | Nettoyage structurel | Basse | Sonnet | E8 | ½–1 j |
| E10 | Documentation finale et vérification de bout en bout | Basse | Haiku | E1 à E9 | 1 h |

\* Durée de travail supervisée, ordre de grandeur et non engagement.

```text
E0 → E1 ─┬→ E2 ─┬→ E4 → E5 ──┐
         │      └→ E6        ├→ E8 → E9 → E10
         └→ E3 ─┬→ E4        │
                └→ E7 ───────┘
```

Lecture : E4 exige E2 **et** E3 ; E5 exige E3 et E4 ; E7 exige E3 et E4 ; E8 exige que toutes les chaînes d'interface soient figées (E2 à E7) ; E9 touche tous les fichiers et passe donc en dernier ; E10 clôt le tout.

## 4. Décisions de conception figées

Ces décisions ont été prises lors de l'audit pour que Claude Code n'ait **rien à réinterpréter**. Ne les remets pas en cause pendant l'implémentation.

| # | Décision | Détail |
|---|---|---|
| D1 | Outillage | Maven (avec Maven Wrapper), JDK **17** minimum (`maven.compiler.release=17`), JUnit Jupiter 5. Scripts PowerShell conservés comme enveloppes Windows. |
| D2 | Bornes de taille | Chiffres affichables : `max(precision, scale) ≤ 1000` pour tout résultat. Longueur maximale d'une saisie (après `trim`) : **1005** caractères. Pour une puissance, estimation avant calcul : `max(base.precision(), base.scale()) × |exposant| ≤ 1000`, sinon refus. La plage d'exposant ±999 est conservée. |
| D3 | Arrondi | Un résultat **exact** n'est jamais arrondi. Un résultat **approché** est arrondi (HALF_UP) à `échelle = max(12, 11 − e)` où `e` est l'exposant du premier chiffre significatif (`precision − scale − 1`) : au moins 12 décimales **et** au moins 12 chiffres significatifs. |
| D4 | Restauration partielle de l'historique | Les lignes invalides sont ignorées et comptées. Une copie horodatée du fichier d'origine est conservée. Aucun nouveau ton d'état : le ton `ERROR` signale une restauration partielle. |
| D5 | Format de fichier | En-tête `#calc-history v1` en première ligne. Un fichier sans en-tête reste lisible (ancien format). Une version inconnue est traitée comme illisible (jamais écrasée). |
| D6 | Instances multiples | Pas de verrouillage de fichier. Limite **documentée** : le dernier écrivain gagne. |
| D7 | Séparateur décimal | La virgule **et** le point restent des séparateurs décimaux (`1.000` vaut `1`). C'est documenté dans l'aide et le README. |
| D8 | Messages du moteur | Les messages d'erreur destinés à l'utilisateur restent dans le moteur. Migrer vers des codes d'erreur reviendrait à faire de l'internationalisation, non justifiée ici. |
| D9 | Accents | Les textes d'interface restent **sans accents jusqu'à E8**, pour ne pas modifier deux fois les tests qui comparent des chaînes. |
| D10 | Historique non borné | Aucune limite de taille d'historique (non-problème mesuré : 5 000 calculs en 3,9 s cumulées, rechargement en 19 ms). |

## 5. Bloc de règles de travail

Ce bloc est déjà inclus à la fin de chaque commande ci-dessous. Il est reproduit ici pour référence.

```text
RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français, SANS accents jusqu'à l'Epic E8 (cohérence avec le code et les tests existants).
```


---

## E0 — Baseline et vérification de l'environnement

**Priorité :** prérequis · **Modèle :** Haiku · **Dépend de :** `BACKLOG.md` committé · **Branche :** aucune (aucune modification de fichier)

**Pourquoi Haiku :** tâche purement exécutive (lancer des commandes, relever des résultats), sans raisonnement de conception.

### Objectif
Établir l'état de référence avant toute modification et lever les points « non vérifiés » de l'audit : le packaging Windows n'a jamais été exécuté, le JDK de la machine est inconnu, l'état Git était incertain.

### Constats couverts
NV-1 (packaging et `Calc.exe` jamais exécutés), RI-4 (volet vérification), NV-3 (état du dépôt).

### Prérequis
- Windows PowerShell, un JDK installé avec `javac`, `jar` et `jpackage` dans le `PATH`.
- Si `%APPDATA%\Calc\history.tsv` existe, il contient de **vrais** calculs : la commande en fait d'abord une copie.

### Travail
1. Relever `java -version`, `javac -version`, et la présence de `jar` et `jpackage`. **Le JDK doit être en version 17 ou plus** : sinon, s'arrêter et le signaler.
2. Relever `git status`, `git log --oneline`, `git config core.autocrlf`.
3. Si `%APPDATA%\Calc\history.tsv` existe, le copier en `history.tsv.manual-backup` dans le même dossier (sans jamais le modifier).
4. Lancer `check.ps1` et relever le résultat des 4 classes de test.
5. Lancer `package.ps1`, vérifier la présence de `dist\Calc\Calc.exe` et `dist\Calc-portable.zip`, démarrer `Calc.exe`, vérifier que le processus reste vivant 5 secondes, puis l'arrêter **sans effectuer de calcul**.

### Critères d'acceptation
- [ ] Versions de `java`, `javac`, `jar`, `jpackage` rapportées, JDK ≥ 17.
- [ ] Sortie exacte : `All tests passed (14 assertions).`, `All presenter tests passed (33 assertions).`, `All history persistence tests passed (17 assertions).`, `All frame tests passed (28 assertions).`
- [ ] `dist\Calc\Calc.exe` et `dist\Calc-portable.zip` produits ; l'exécutable démarre.
- [ ] `git status` identique avant et après (aucun fichier suivi modifié ; `out/` et `dist/` sont ignorés).
- [ ] Sauvegarde manuelle de l'historique faite si le fichier existait.

### Commande Claude Code
```text
Contexte : le dépôt courant est « Calc », une calculatrice Java/Swing en français (moteur dans Calcul/, présentateur et vue dans Graphique/, tests dans Tests/, scripts check.ps1 / run.ps1 / package.ps1). Un audit a été réalisé ; avant toute correction je veux un état de référence. Cette tâche NE DOIT MODIFIER AUCUN FICHIER du dépôt.

À faire :
1. Relève et rapporte les versions de java, javac, jar et jpackage. Le JDK doit être 17 ou plus ; si ce n'est pas le cas, arrête-toi et dis-le.
2. Relève git status, git log --oneline et git config core.autocrlf. Note que les fins de ligne peuvent faire apparaître des fichiers « modifiés » : ne corrige rien.
3. Si le fichier %APPDATA%\Calc\history.tsv existe, copie-le en %APPDATA%\Calc\history.tsv.manual-backup (sans jamais modifier l'original), sinon indique qu'il n'existe pas.
4. Exécute : powershell -ExecutionPolicy Bypass -File .\check.ps1 . Rapporte la sortie des 4 classes de test. Attendu : 14, 33, 17 et 28 assertions, toutes vertes.
5. Exécute : powershell -ExecutionPolicy Bypass -File .\package.ps1 . Vérifie que dist\Calc\Calc.exe et dist\Calc-portable.zip existent. Démarre Calc.exe, vérifie que le processus reste vivant 5 secondes, puis arrête-le. N'effectue AUCUN calcul dans l'application (elle lit mon vrai historique).
6. Termine par un rapport concis : versions, résultats des tests, résultat du packaging, état git avant/après (doit être identique), anomalies éventuelles.

Règles : n'écris aucun fichier dans le dépôt, ne crée aucune branche, ne commite rien. Si une commande échoue, rapporte l'erreur exacte sans tenter de contournement.
```

### Validation
Lis le rapport : les quatre compteurs d'assertions (14/33/17/28) et la présence des deux livrables de packaging. Lance toi-même `Calc.exe` pour un contrôle visuel de la fenêtre (le rendu visuel n'a jamais été vérifié). Ferme sans calculer.

---

## E1 — Socle de build, de tests et d'intégration continue

**Priorité :** haute (facilitateur de tous les Epics suivants) · **Modèle :** Sonnet · **Dépend de :** E0 · **Branche :** `epic/e1-build-foundation`

**Pourquoi Sonnet :** migration multi-fichiers (pom, tests, scripts, CI) à comportement constant : assez de jugement pour les détails d'outillage, aucun raisonnement algorithmique qui justifie Opus.

### Objectif
Passer d'un build à base de scripts PowerShell et de tests maison à un build Maven reproductible, avec de vrais tests JUnit nommés, exécutables sans interface graphique réelle, sans embarquer les tests dans l'application, et vérifiés par une intégration continue. **Aucun comportement de l'application ne change.**

### Constats couverts
AV-6 (tests embarqués dans le JAR ; test d'interface qui échoue hors affichage et bloque tout `check.ps1` ; moteur de test maison sans noms de tests et s'arrêtant à la première erreur), PR-5 (pas d'outil de build ni de JUnit, JDK minimal non documenté), RI-4 (scripts réservés à Windows, pas de CI).

### Prérequis
- E0 terminé, JDK ≥ 17.
- Maven installé. S'il est absent, Claude Code doit te demander de lancer `scoop install maven` (ne le laisse pas installer sans te le dire).

### Fichiers concernés
Création : `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/*`, `.gitattributes`, `.github/workflows/ci.yml`, `src/main/java/**`, `src/test/java/**`. Déplacement (avec `git mv`) : `Main.java`, `Calcul/`, `Graphique/`, `Tests/`. Modification : `check.ps1`, `run.ps1`, `package.ps1`, `.gitignore`, `README.md`.

### Travail
1. `pom.xml` : packaging `jar`, `finalName` `calc`, `release` 17, encodage UTF-8, `-Xlint:all` **sans** `-Werror` (les 6 avertissements existants sont traités en E9), JUnit Jupiter 5 (dernière 5.x stable), Surefire récent, manifeste `Main-Class: Main`.
2. Disposition standard : `src/main/java` (`Main.java` dans le package par défaut, `Calcul/`, `Graphique/`) et `src/test/java/Tests/`. **Les noms de packages restent inchangés** (leur renommage est l'objet de E9).
3. Convertir les 4 classes de test en JUnit 5 : **une méthode `@Test` par ancienne méthode `test…`** (23 au total, noms conservés), mêmes assertions, `@TempDir` pour les fichiers temporaires.
4. Test d'interface : s'il n'y a pas d'affichage, il est **ignoré** (`Assumptions`) et signalé comme tel ; si la propriété système `calc.ui.required=true` est positionnée, il **échoue** à la place.
5. Scripts : `check.ps1` appelle `mvnw.cmd -B verify` ; `run.ps1` construit puis lance `java -jar target\calc.jar` ; `package.ps1` exécute `verify`, prépare un dossier d'entrée ne contenant que `calc.jar`, puis appelle `jpackage` comme avant (sortie `dist\Calc\Calc.exe` et `dist\Calc-portable.zip`).
6. `.gitattributes` : `* text=auto`, `mvnw text eol=lf`, `*.cmd text eol=crlf`, `*.ps1 text eol=crlf`. Vérifier que `git status` reste propre.
7. `.gitignore` : ajouter `target/`.
8. CI GitHub Actions (`push` et `pull_request` sur `main`) : `ubuntu-latest` (JDK 17 et 21, `xvfb-run -a ./mvnw -B verify -Dcalc.ui.required=true`) et `windows-latest` (JDK 17, `mvnw.cmd -B verify -Dcalc.ui.required=true`), Temurin.
9. README : section Prérequis (JDK 17+, Maven Wrapper), commandes de build, de test, de lancement.

### Hors périmètre de l'Epic
Aucune correction fonctionnelle, aucun changement de messages, aucun renommage de package. Pas de tests par propriétés, de fuzzing, d'installeurs multiplateformes ni de rapport de conformité (voir « Évolutions futures »).

### Critères d'acceptation
- [ ] `.\mvnw.cmd -B verify` : `BUILD SUCCESS`, **au moins 23 tests, 0 échec, 0 erreur, 0 ignoré** (sous Windows avec affichage).
- [ ] Les 23 anciens noms de méthodes existent à l'identique dans les classes JUnit.
- [ ] `.\mvnw.cmd -B test "-Djava.awt.headless=true"` : réussi, avec exactement les tests d'interface signalés « ignorés ».
- [ ] `.\mvnw.cmd -B test "-Djava.awt.headless=true" "-Dcalc.ui.required=true"` : **échoue** (le test d'interface refuse d'être ignoré).
- [ ] `jar tf target\calc.jar` ne contient aucune entrée `Tests/`.
- [ ] `powershell -File .\check.ps1` retourne 0 ; `run.ps1` lance l'application ; `package.ps1` produit `Calc.exe` et le zip.
- [ ] `git status` propre juste après un `git checkout` frais (pas de fichiers fantômes « modifiés »).
- [ ] Le workflow CI s'exécute avec succès sur GitHub (à vérifier par toi après un `push` de la branche).

### Commande Claude Code
```text
Contexte : dépôt « Calc », calculatrice Java/Swing en français. Aujourd'hui : sources en vrac (Main.java à la racine, Calcul/ = moteur BigDecimal, Graphique/ = présentateur MVP + fenêtre Swing, Tests/ = 4 classes de test maison avec main()), scripts PowerShell uniquement (check.ps1, run.ps1, package.ps1), aucun outil de build, aucune CI. Un audit a relevé : les classes de test sont embarquées dans le JAR de production (package.ps1 jar tout le dossier out) ; le test d'interface (CalculatorFrameTest) lève une AssertionError en environnement sans affichage, ce qui fait échouer tout check.ps1 ; le moteur de test maison s'arrête à la première assertion fausse et ne nomme pas les tests ; aucun JDK minimal documenté ; build Windows uniquement ; pas de CI.

Mission de l'Epic E1 : migrer vers Maven + JUnit 5 + CI SANS CHANGER AUCUN COMPORTEMENT de l'application. Ne renomme AUCUN package (Calcul, Graphique, Tests restent tels quels).

Crée la branche epic/e1-build-foundation depuis main. Prérequis : JDK 17+ et Maven. Si mvn est absent, demande-moi de lancer « scoop install maven » ; ne l'installe pas sans mon accord.

À faire :
1. pom.xml : packaging jar, finalName « calc », maven.compiler.release 17, encodage source UTF-8, option de compilation -Xlint:all SANS -Werror (6 avertissements existants, traités plus tard), dépendance JUnit Jupiter 5 (dernière 5.x stable), Surefire récent, manifeste Main-Class = Main. Génère le Maven Wrapper (mvnw, mvnw.cmd, .mvn/wrapper).
2. Disposition standard avec git mv (historique conservé) : Main.java, Calcul/, Graphique/ -> src/main/java/ ; Tests/ -> src/test/java/Tests/ (le package Java « Tests » est conservé).
3. Convertis les 4 classes de test en JUnit 5 : UNE méthode @Test par ancienne méthode test… (il y en a 23 : moteur 10, présentateur 7, persistance 4, interface 2), en CONSERVANT les noms et la sémantique des assertions (Assertions.assertEquals/assertTrue/assertFalse/assertThrows ; supprime les compteurs et helpers maison devenus inutiles). Utilise @TempDir à la place de Files.createTempDirectory pour les fichiers temporaires. Pour les tests d'interface (CalculatorFrameTest) : s'il n'y a pas d'affichage (GraphicsEnvironment.isHeadless()), ils doivent être IGNORÉS via Assumptions (visible comme « skipped »), SAUF si la propriété système calc.ui.required=true est positionnée, auquel cas ils doivent ÉCHOUER. Transmets cette propriété à la JVM de test par la configuration Surefire (valeur par défaut false).
4. Scripts : check.ps1 -> exécute « mvnw.cmd -B verify » et propage le code retour. run.ps1 -> construit sans tests puis lance java -jar target\calc.jar. package.ps1 -> exécute verify, prépare un dossier d'entrée contenant UNIQUEMENT calc.jar (les tests ne doivent plus être dans l'application), puis appelle jpackage (type app-image, nom Calc, version 1.0.0, vendor et description inchangés, main-class Main) et produit dist\Calc\Calc.exe et dist\Calc-portable.zip comme avant. Conserve les garde-fous de suppression de dossiers existants quand ils restent pertinents.
5. .gitattributes : « * text=auto », « mvnw text eol=lf », « *.cmd text eol=crlf », « *.ps1 text eol=crlf ». Vérifie qu'après cela git status ne signale aucun fichier modifié par les fins de ligne (git add --renormalize . si nécessaire, sans changer le contenu).
6. .gitignore : ajoute target/ (garde dist/ ; out/ peut rester).
7. .github/workflows/ci.yml : déclenché sur push et pull_request vers main ; job ubuntu-latest avec Temurin 17 et 21 (matrice) exécutant « xvfb-run -a ./mvnw -B verify -Dcalc.ui.required=true » ; job windows-latest avec Temurin 17 exécutant « mvnw.cmd -B verify -Dcalc.ui.required=true » ; utilise actions/checkout et actions/setup-java en version majeure stable.
8. README : section Prérequis (JDK 17 minimum, Maven Wrapper fourni) et commandes de build / test / lancement / packaging à jour. Ne modifie pas les autres sections.

Critères de validation que tu dois exécuter et prouver : (a) « .\mvnw.cmd -B verify » -> BUILD SUCCESS avec au moins 23 tests, 0 échec, 0 erreur, 0 ignoré ; (b) « .\mvnw.cmd -B test "-Djava.awt.headless=true" » -> réussi, avec les tests d'interface signalés ignorés ; (c) la même commande avec « "-Dcalc.ui.required=true" » -> ÉCHEC attendu ; (d) « jar tf target\calc.jar » ne contient aucune entrée Tests/ ; (e) check.ps1, run.ps1 (lancement bref puis arrêt) et package.ps1 fonctionnent ; (f) git status propre. Ne fais PAS de git push : je pousserai moi-même pour déclencher la CI.

RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français, SANS accents jusqu'à l'Epic E8 (cohérence avec le code et les tests existants).
```

### Validation
Exécute toi-même `.\mvnw.cmd -B verify` puis `powershell -ExecutionPolicy Bypass -File .\check.ps1` et `.\package.ps1`. Compare à la référence : 23 tests, 0 échec. Pousse la branche (`git push -u origin epic/e1-build-foundation`) et vérifie que la CI est verte sur Linux **et** Windows avant de fusionner dans `main`.

---

## E2 — Historique : ne plus jamais rien perdre

**Priorité :** **critique** · **Modèle :** Sonnet · **Dépend de :** E1 · **Branche :** `epic/e2-history-safety`

**Pourquoi Sonnet :** changement d'interface (type de retour du chargement) qui traverse magasin, présentateur et tests, avec plusieurs cas limites ; la conception est entièrement fournie, il n'y a pas de zone d'ambiguïté qui justifie Opus.

### Objectif
Garantir qu'aucun scénario ne détruit un fichier d'historique existant sans qu'une copie fidèle octet pour octet n'ait été conservée, que le chargement tolère les lignes invalides, et que toute défaillance soit diagnostiquable.

### Constats couverts
AV-1 (une seule ligne invalide, ou un BOM, fait refuser tout l'historique ; le calcul suivant réécrit le fichier avec une seule entrée : reproduit, un fichier de 4 lignes en contient 1 après un calcul), AV-5 (chargement tout-ou-rien, aucun diagnostic car les causes sont jetées et rien n'est journalisé, entrée `abc + 2 = 5` acceptée à cause du repli sur le résultat stocké).

### Prérequis
E1 terminé et fusionné (tests JUnit en place).

### Fichiers concernés
`Calcul/CalculationHistoryStore.java`, `Calcul/FileCalculationHistoryStore.java`, `Calcul/CalculationHistoryException.java`, `Calcul/DecimalPolicy.java` (suppression de `restorePersistedValue`), nouveau `Calcul/HistoryLoadResult.java`, `Graphique/CalculatorPresenter.java`, tests de persistance et de présentateur.

### Travail
1. **Type de retour** : `loadHistory()` renvoie `HistoryLoadResult` (`record` : liste des enregistrements, nombre de lignes ignorées, chemin de la copie de sauvegarde éventuelle). `noop()` est adapté. `CalculationHistoryException` ne sert plus qu'aux échecs d'entrée/sortie.
2. **Lecture ligne par ligne** : une ligne invalide (nombre de colonnes différent de 4, opération inconnue, opérande refusé par le moteur) est **ignorée et comptée**. Un BOM UTF-8 en début de fichier est ignoré. Les fins de ligne CRLF restent acceptées (déjà le cas, à couvrir par un test).
3. **Fin du repli sur le résultat stocké** : si le recalcul d'une ligne échoue, la ligne est invalide. La colonne « résultat » reste écrite (lisibilité) mais n'est plus lue. `DecimalPolicy.restorePersistedValue` devient inutile et est supprimé.
4. **Sauvegarde de sécurité** : quand le fichier existe et qu'au moins une ligne est ignorée, ou qu'il est illisible, une copie nommée `history.tsv.<yyyyMMdd-HHmmss>.bak` est créée dans le même dossier. Elle n'écrase jamais une copie existante (suffixe numérique en cas de collision). Un fichier entièrement valide ne génère pas de copie.
5. **Mode protégé** : si la copie de sécurité échoue, ou si la lecture a échoué, toute sauvegarde ultérieure **est refusée** (`CalculationHistoryException`) jusqu'au prochain démarrage. Le présentateur affiche alors le message existant « Calcul effectue, mais l'historique n'a pas pu etre enregistre. ».
6. **Messages du présentateur** (ton `ERROR`, pas de nouveau ton) :
   - aucune ligne ignorée, au moins une entrée : message de restauration existant (inchangé) ;
   - fichier absent ou vide : message d'historique vide existant (inchangé) ;
   - restauration partielle : `Historique restaure partiellement : N entree(s) ignoree(s). Une copie du fichier d'origine a ete conservee.` (pluriel géré : « 1 entree ignoree » / « N entrees ignorees ») ;
   - rien de restaurable, ou lecture impossible : le message existant « L'historique precedent n'a pas pu etre restaure. Vous pouvez continuer sans blocage. » suivi de « Une copie du fichier d'origine a ete conservee. » lorsqu'une copie existe. Le test existant qui compare ce message doit être mis à jour en conséquence.
7. **Journalisation** (`java.util.logging`) : le magasin et le présentateur journalisent au niveau `WARNING` chaque cause d'échec (lecture, écriture, copie), avec le chemin du fichier, les numéros des lignes ignorées et le motif. Les messages d'exception incluent le chemin. Rien de technique n'est affiché dans l'interface.

### Hors périmètre de l'Epic
Écriture atomique et en-tête de version (E6). Bornes de taille (E4).

### Critères d'acceptation
- [ ] Reproduction de l'anomalie d'origine corrigée : un fichier de 4 lignes dont 1 invalide, après chargement puis un calcul, contient **3 + 1 = 4 lignes valides** et une copie de sauvegarde **identique octet pour octet** à l'original existe.
- [ ] Fichier entièrement invalide : 0 entrée restaurée, copie conservée, le premier calcul suivant ne détruit pas la copie.
- [ ] Fichier avec BOM, fichier CRLF : chargés correctement.
- [ ] `ADDITION<TAB>abc<TAB>2<TAB>5` est **ignorée** (plus jamais affichée « abc + 2 = 5 »).
- [ ] `MULTIPLICATION<TAB>2<TAB>3<TAB>999` est restaurée comme `2 * 3 = 6` (la valeur stockée est ignorée).
- [ ] Ligne à 5 colonnes ou 3 colonnes : ignorée.
- [ ] Échec de la copie de sécurité (ou de lecture) : aucune écriture ne peut remplacer le fichier d'origine.
- [ ] Un fichier entièrement valide ne produit aucune copie de sauvegarde.
- [ ] Les causes d'échec apparaissent dans le journal avec le chemin ; aucune trace technique dans l'interface.
- [ ] Les 23 tests de référence passent (le seul test modifié est celui du message d'historique irrécupérable).

### Commande Claude Code
```text
Contexte : dépôt « Calc », calculatrice Java/Swing en français (Maven, JUnit 5 ; moteur BigDecimal dans Calcul/, présentateur MVP et fenêtre dans Graphique/). L'historique est persisté en TSV par Calcul/FileCalculationHistoryStore (colonnes : OPERATION, opérande 1, opérande 2, résultat) et chargé par le constructeur de Graphique/CalculatorPresenter.

Anomalie avérée à corriger (Epic E2, critique) : si UNE seule ligne de history.tsv est invalide, ou si le fichier commence par un BOM UTF-8, loadHistory() lève une exception, le présentateur démarre avec un historique vide, puis le calcul suivant réécrit le fichier avec UNE seule entrée : tout l'ancien historique est détruit sans copie ni avertissement. Reproduit : fichier de 4 lignes dont 1 corrompue -> 1 ligne après un seul calcul. Autres défauts liés : chargement tout-ou-rien ; les causes des exceptions sont jetées sans journalisation (le présentateur avale CalculationHistoryException) ; restoreResult retombe sur le résultat stocké quand le recalcul échoue, ce qui accepte des entrées incohérentes (« ADDITION<TAB>abc<TAB>2<TAB>5 » s'affiche « abc + 2 = 5 »).

Crée la branche epic/e2-history-safety depuis main. Implémente :
1. Nouveau record public HistoryLoadResult(List<CalculationRecord> records, int skippedLines, Path backupFile) dans Calcul (backupFile nullable). CalculationHistoryStore.loadHistory() retourne HistoryLoadResult ; adapte noop() et tous les appelants. CalculationHistoryException ne sert plus qu'aux échecs d'entrée/sortie.
2. FileCalculationHistoryStore : lecture ligne par ligne. Une ligne invalide (colonnes != 4, opération inconnue, opérande refusé par CalculatorEngine, tout IllegalArgumentException / ArithmeticException / CalculationException) est IGNORÉE et comptée. Ignore un BOM UTF-8 (U+FEFF) en tête de fichier. Lignes vides ignorées ; CRLF accepté.
3. Supprime le repli sur le résultat stocké : si engine.calculate échoue pour une ligne, la ligne est invalide. La 4e colonne reste écrite à la sauvegarde mais n'est plus lue. Supprime DecimalPolicy.restorePersistedValue devenue inutile (ne touche à rien d'autre dans DecimalPolicy).
4. Sauvegarde de sécurité : si le fichier existe et (au moins une ligne ignorée OU lecture impossible), copie-le AVANT toute écriture en « history.tsv.<yyyyMMdd-HHmmss>.bak » dans le même dossier, sans jamais écraser une copie existante (suffixe numérique si collision). Un fichier entièrement valide ne crée AUCUNE copie. La copie doit être identique octet pour octet.
5. Mode protégé : si la copie échoue, ou si la lecture a échoué, saveHistory doit refuser d'écrire (lever CalculationHistoryException) pour la durée de vie de l'instance du magasin, y compris pour le cas « historique vide -> deleteIfExists ».
6. Présentateur (ton StatusTone.ERROR, ne crée pas de nouveau ton) : aucune ligne ignorée et >= 1 entrée -> message de restauration actuel inchangé ; fichier absent/vide -> message d'historique vide actuel inchangé ; restauration partielle (au moins 1 entrée restaurée ET au moins 1 ignorée) -> « Historique restaure partiellement : N entree(s) ignoree(s). Une copie du fichier d'origine a ete conservee. » avec accord singulier/pluriel (« 1 entree ignoree » / « N entrees ignorees ») ; rien de restaurable ou lecture impossible -> le message actuel « L'historique precedent n'a pas pu etre restaure. Vous pouvez continuer sans blocage. » suivi de « Une copie du fichier d'origine a ete conservee. » quand une copie existe (mets à jour le test existant qui compare ce message exact et justifie-le dans le commit). Les textes restent SANS accents.
7. Journalisation avec java.util.logging (Logger statique par classe) : niveau WARNING pour chaque cause d'échec (lecture, écriture, copie), avec chemin du fichier, numéros de lignes ignorées et motif ; les messages de CalculationHistoryException incluent le chemin ; le présentateur journalise aussi la cause quand persistHistory() échoue. Aucune trace technique dans l'interface.

Tests JUnit à ajouter (noms explicites) : fichier de 4 lignes dont 1 invalide -> 3 restaurées, 1 ignorée, copie identique octet pour octet, puis un calcul -> fichier à 4 lignes valides et copie intacte ; fichier entièrement invalide -> 0 entrée + copie + calcul suivant n'affecte pas la copie ; BOM ; CRLF ; « ADDITION\tabc\t2\t5 » ignorée ; « MULTIPLICATION\t2\t3\t999 » restaurée comme « 2 * 3 = 6 » ; 3 et 5 colonnes ignorées ; fichier valide -> aucune copie ; mode protégé (introduis une indirection minimale pour simuler l'échec de copie si c'est simple, sinon couvre-le autrement) ; les 3 messages du présentateur (partiel, irrécupérable, vide) ; noop() renvoie un résultat vide.

Hors périmètre : écriture atomique et en-tête de version (Epic E6), bornes de taille (Epic E4), accents (Epic E8). Validation à exécuter et prouver : « .\mvnw.cmd -B verify » vert, avec au moins les 23 tests de référence plus les nouveaux ; démonstration reproductible de l'anomalie corrigée (4 lignes dont 1 invalide -> après un calcul : 4 lignes valides + copie identique).

RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français, SANS accents jusqu'à l'Epic E8 (cohérence avec le code et les tests existants).
```

### Validation
1. `.\mvnw.cmd -B verify` : tous les tests passent (23 de référence + nouveaux).
2. Test manuel : prends la copie `history.tsv.manual-backup` faite en E0, corromps une ligne, lance `run.ps1`, vérifie le message de restauration partielle, effectue un calcul, puis contrôle que le dossier `%APPDATA%\Calc` contient la copie `.bak` identique à l'original corrompu.
3. Puis fusionne dans `main`.


---

## E3 — Erreurs typées : champ fautif explicite

**Priorité :** moyenne · **Modèle :** Haiku · **Dépend de :** E1 · **Branche :** `epic/e3-typed-errors`

**Pourquoi Haiku :** refactoring petit, mécanique et entièrement spécifié (une énumération, une dizaine de sites d'appel, quelques tests). Aucune ambiguïté de conception.

### Objectif
Remplacer la décision de focus fondée sur la présence du mot « second » dans le texte du message d'erreur par une information explicite portée par l'exception, et corriger le message trompeur sur les exposants hors plage.

### Constats couverts
AV-4 : (a) `resolveFocusTargetForError` cherche le mot « second » dans le message ; les erreurs d'exposant (« L'exposant doit etre un entier… ») ne le contiennent pas, donc le focus va au premier champ alors que l'erreur concerne le second (reproduit pour `1,5`, `5000` et `0 ^ -1`) ; (b) `2 ^ 99999999999` affiche « L'exposant doit etre un entier. » alors que c'est un entier hors plage (le `catch (ArithmeticException)` de `applyPower` masque l'erreur de plage).

### Prérequis
E1 terminé et fusionné.

### Fichiers concernés
`Calcul/CalculationException.java`, nouveau `Calcul/InputField.java`, `Calcul/CalculatorEngine.java`, `Graphique/CalculatorPresenter.java`, tests du moteur et du présentateur.

### Travail
1. Nouvelle énumération publique `InputField { FIRST, SECOND, NONE }`.
2. `CalculationException(String message, InputField field)` ; le constructeur à un seul argument est supprimé pour forcer un choix explicite ; accesseur `getField()`.
3. Affectation dans le moteur : premier nombre manquant ou invalide → `FIRST` ; second nombre manquant ou invalide → `SECOND` ; division par zéro → `SECOND` ; racine d'un négatif → `FIRST` ; exposant non entier → `SECOND` ; exposant hors plage ±999 → `SECOND` ; `0` puissance négative → `FIRST` (comportement actuel conservé) ; opération absente ou non prise en charge → `NONE`.
4. Exposant : tester l'entier **puis** la plage en comparant la valeur absolue à 999 **avant** `intValueExact`. Un entier hors plage (ex. `99999999999`) renvoie le message de plage « L'exposant doit etre un entier compris entre -999 et 999. ». Supprimer le `catch (ArithmeticException)` désormais sans objet.
5. Présentateur : `FIRST` ou `NONE` → focus premier champ ; `SECOND` → second champ si l'opération en a un, sinon premier champ (garde actuelle conservée). Supprimer le test sur le texte et l'import `Locale` devenu inutile.

### Critères d'acceptation
- [ ] Erreurs d'exposant (`1,5`, `5000`, `99999999999`) : focus sur le **second** champ.
- [ ] `0 ^ -1` : focus sur le premier champ (inchangé).
- [ ] `2 ^ 99999999999` : message « L'exposant doit etre un entier compris entre -999 et 999. ».
- [ ] Un moteur factice (sous-classe de `CalculatorEngine`) qui lève `new CalculationException("texte quelconque", InputField.SECOND)` donne un focus sur le second champ : le focus ne dépend plus du texte.
- [ ] Plus aucune occurrence de `toLowerCase` / `contains("second")` dans le présentateur.
- [ ] Les tests de référence existants passent sans modification de leurs attentes.

### Commande Claude Code
```text
Contexte : dépôt « Calc », calculatrice Java/Swing en français (Maven, JUnit 5). Le moteur est dans Calcul/CalculatorEngine ; le présentateur Graphique/CalculatorPresenter décide du champ à focaliser après une erreur via resolveFocusTargetForError(String message), qui cherche le mot « second » dans le TEXTE du message.

Anomalies avérées (Epic E3) : (a) les erreurs d'exposant (« L'exposant doit etre un entier. », « …compris entre -999 et 999. ») ne contiennent pas « second » : le focus part sur le premier champ alors que l'erreur concerne le second (reproduit avec exposants 1,5 et 5000) ; de plus tout changement de libellé casse silencieusement ce comportement. (b) 2 ^ 99999999999 affiche « L'exposant doit etre un entier. » alors que c'est un entier hors plage : le catch (ArithmeticException) de applyPower masque la vérification de plage car intValueExact lève avant.

Crée la branche epic/e3-typed-errors depuis main. Implémente :
1. Énumération publique Calcul.InputField { FIRST, SECOND, NONE }.
2. CalculationException(String message, InputField field) + getField(). Supprime le constructeur à un seul argument et corrige TOUS les sites de levée dans CalculatorEngine : premier nombre manquant/invalide -> FIRST ; second manquant/invalide -> SECOND ; division par zéro -> SECOND ; racine d'un négatif -> FIRST ; exposant non entier -> SECOND ; exposant hors plage -> SECOND ; 0 puissance négative -> FIRST ; opération absente ou non prise en charge -> NONE.
3. applyPower : vérifie d'abord que l'exposant est entier, puis que sa valeur absolue est <= DecimalPolicy.MAX_ABSOLUTE_INTEGER_EXPONENT en comparant en BigDecimal AVANT d'appeler intValueExact ; un entier hors plage lève le message de plage existant « L'exposant doit etre un entier compris entre -999 et 999. » (construit à partir de la constante). Supprime le catch (ArithmeticException) devenu sans objet. Ne change aucun texte de message existant par ailleurs.
4. Présentateur : remplace resolveFocusTargetForError(String) par une version qui lit exception.getField() : FIRST ou NONE -> FocusTarget.FIRST_NUMBER ; SECOND -> FocusTarget.SECOND_NUMBER si l'opération sélectionnée requiert un second opérande, sinon FIRST_NUMBER. Supprime la recherche textuelle et l'import Locale.

Tests JUnit à ajouter : focus second champ pour exposants « 1,5 », « 5000 », « 99999999999 » ; focus premier champ pour 0 ^ -1 ; message de plage pour 99999999999 ; test d'indépendance au texte : sous-classe anonyme de CalculatorEngine dont calculate() lève new CalculationException("texte quelconque", InputField.SECOND) -> focus SECOND_NUMBER ; opération nulle -> InputField.NONE -> focus premier champ.

Hors périmètre : bornes de taille (E4), arrondi (E5), accents (E8). Validation à exécuter et prouver : « .\mvnw.cmd -B verify » vert ; les 23 tests de référence existants passent sans que leurs attentes soient modifiées ; grep confirmant l'absence de « contains("second") » dans le présentateur.

RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français, SANS accents jusqu'à l'Epic E8 (cohérence avec le code et les tests existants).
```

### Validation
`.\mvnw.cmd -B verify`. Contrôle manuel : sélectionne « Puissance », saisis `2` et `1,5`, appuie sur Entrée : le curseur doit atterrir dans le **second** champ. Puis fusionne.

---

## E4 — Bornes de taille : fin du gel de l'interface

**Priorité :** **haute** · **Modèle :** Sonnet · **Dépend de :** E2 (les lignes d'historique devenues trop grosses sont ignorées et sauvegardées au lieu de figer le démarrage) et E3 (les nouvelles erreurs portent un champ) · **Branche :** `epic/e4-size-limits`

**Pourquoi Sonnet :** les seuils et les formules sont fixés (décision D2) ; le travail est de les appliquer proprement dans le moteur et de les prouver par tests. Pas d'enjeu de conception ouverte.

### Objectif
Rendre impossible qu'une saisie ou un enchaînement normal de calculs fasse figer l'application, en bornant la taille des nombres traités, y compris au rechargement de l'historique.

### Constats couverts
AV-2 : aucune borne sur la taille des opérandes ni des résultats, tout s'exécute sur le thread d'interface, et le résultat est réinjecté dans le champ. Mesures de l'audit : `2^999` puis `^999` donne 300 429 chiffres en 0,5 s ; la troisième puissance ne se termine pas en 30 s ; l'analyse d'un nombre est quadratique (10 000 chiffres : 0,2 s ; 60 000 : 5 s ; 200 000 : environ 56 s). Risque dérivé du code : au rechargement, chaque ligne est recalculée dans le constructeur du présentateur, donc sur le thread d'interface, avant l'affichage de la fenêtre.

### Prérequis
E2 et E3 terminés et fusionnés.

### Fichiers concernés
`Calcul/DecimalPolicy.java` (constantes et contrôle), `Calcul/CalculatorEngine.java`, tests du moteur, du présentateur et de la persistance, `README.md` (mise à jour finale en E10).

### Travail (valeurs de la décision D2)
1. Constantes : `MAX_DISPLAY_DIGITS = 1000` et `MAX_INPUT_LENGTH = 1005` (1000 chiffres, signe, virgule et marge pour le « 0, » initial).
2. Saisie : après `trim`, toute saisie de plus de 1005 caractères est refusée **avant** l'analyse, avec le champ concerné (`FIRST` ou `SECOND`) et un message du type « Le premier nombre est trop long (1005 caracteres maximum). ».
3. Résultat : après chaque opération, si `max(precision, scale) > 1000`, refuser avec « Le resultat depasse 1000 chiffres (reduisez les nombres saisis). » (champ `NONE`).
4. Puissance : **avant** de calculer, si `max(base.precision(), base.scale()) × |exposant| > 1000`, refuser avec « Le resultat depasse 1000 chiffres (reduisez l'exposant ou la base). » (champ `SECOND`). Cette estimation s'applique aussi aux exposants négatifs.
5. Tout `ArithmeticException` inattendu levé par `BigDecimal` pendant un calcul est converti en `CalculationException` (« Le calcul n'a pas pu etre effectue sur des nombres de cette taille. », champ `NONE`) plutôt que de remonter jusqu'au thread d'interface.
6. Ne touche pas à la plage d'exposant ±999, ni aux textes d'aide (`getInputHint`) déjà comparés par des tests.

### Hors périmètre de l'Epic
Calcul en arrière-plan (`SwingWorker`) : inutile une fois les bornes en place. Politique d'arrondi (E5). Limite du nombre d'entrées d'historique (décision D10 : aucune).

### Critères d'acceptation
- [ ] Saisie de 1005 caractères acceptée par le contrôle de longueur (utiliser un nombre complété de zéros en tête, par ex. 1004 zéros puis `1`, qui vaut `1`), de 1006 refusée avec le bon champ, en moins de 100 ms.
- [ ] `2 ^ 999` accepté (301 chiffres) ; **le résultat réinjecté** peut être réutilisé dans un calcul suivant (ex. `+ 1`) : le chaînage normal fonctionne.
- [ ] `(2^999) ^ 999` refusé **immédiatement** (moins de 1 s) avec le message de dépassement : plus de gel.
- [ ] `99 ^ 999` refusé ; `10 ^ 999` refusé (estimation 2 × 999 > 1000) ; `0,0001 ^ 999` refusé ; `0,5 ^ 999` et `9 ^ 999` acceptés ; `999…9` (999 chiffres) `+ 1` accepté (résultat de 1000 chiffres exactement) et `999…9` (1000 chiffres) `+ 1` refusé (1001 chiffres).
- [ ] Multiplication de deux nombres de 600 chiffres refusée.
- [ ] Les cas limites acceptés se terminent chacun en moins de 5 secondes (`assertTimeoutPreemptively`), c'est-à-dire sans régression de performance.
- [ ] Un fichier d'historique contenant une ligne trop volumineuse : la ligne est ignorée, une copie est conservée (comportement de E2), et le démarrage n'est pas bloqué.
- [ ] Le présentateur affiche l'erreur et place le focus sur le bon champ ; l'historique n'est pas modifié.
- [ ] Les 23 tests de référence passent.

### Commande Claude Code
```text
Contexte : dépôt « Calc », calculatrice Java/Swing en français (Maven, JUnit 5). Moteur BigDecimal dans Calcul/ (CalculatorEngine, DecimalPolicy), présentateur et fenêtre dans Graphique/. Epics E2 (historique sûr) et E3 (CalculationException porte un InputField FIRST/SECOND/NONE) sont déjà en place.

Anomalie avérée à corriger (Epic E4, haute) : aucune borne sur la taille des opérandes ni des résultats, et tout est exécuté sur le thread d'interface. Mesures : 2^999 puis ^999 produit 300 429 chiffres en 0,5 s ; la 3e puissance consécutive ne se termine pas en 30 s ; l'analyse puis la normalisation d'un nombre est quadratique (10 000 chiffres = 0,2 s, 60 000 = 5 s, 200 000 = environ 56 s). Le résultat est réinjecté dans le premier champ (chaînage), donc un utilisateur peut figer l'application en enchaînant trois fois « Calculer » avec une puissance. De plus, au rechargement de l'historique, chaque ligne est recalculée dans le constructeur du présentateur (thread d'interface).

Crée la branche epic/e4-size-limits depuis main. Implémente (valeurs fixées, ne les change pas) :
1. Dans DecimalPolicy : MAX_DISPLAY_DIGITS = 1000 et MAX_INPUT_LENGTH = 1005.
2. Saisie : après trim, une saisie de plus de 1005 caractères est refusée AVANT toute analyse (regex, BigDecimal) avec CalculationException portant le bon InputField et le message « Le premier nombre est trop long (1005 caracteres maximum). » / « Le second nombre est trop long (1005 caracteres maximum). » (construis le nombre à partir de la constante).
3. Résultat : après chaque opération (addition, soustraction, multiplication, division, puissance, racine), si max(result.precision(), result.scale()) > 1000, lever « Le resultat depasse 1000 chiffres (reduisez les nombres saisis). » avec InputField.NONE. (Un résultat de 1000 chiffres exactement, comme 999 chiffres « 9 » + 1, reste accepté ; avec 1000 chiffres « 9 » + 1 le résultat a 1001 chiffres et est refusé.)
4. Puissance : AVANT tout calcul de base.pow, si max(base.precision(), base.scale()) * |exposant| > 1000, lever « Le resultat depasse 1000 chiffres (reduisez l'exposant ou la base). » avec InputField.SECOND ; valable aussi pour les exposants négatifs. La plage d'exposant ±999 et ses messages restent inchangés. Utilise la forme normalisée de la base (DecimalPolicy.normalize) pour precision/scale.
5. Convertis tout ArithmeticException inattendu de BigDecimal dans le moteur en CalculationException « Le calcul n'a pas pu etre effectue sur des nombres de cette taille. » (InputField.NONE) afin qu'aucune exception non gérée n'atteigne le thread d'interface.
6. Ne change aucun texte d'aide (Operation.getInputHint) ni aucun message existant : des tests les comparent. Textes SANS accents.

Tests JUnit à ajouter : saisie de 1005 caractères acceptée / 1006 refusée (utilise un nombre complété de zéros en tête, par ex. 1004 zéros puis « 1 », pour que seule la longueur soit en jeu ; champ FIRST puis SECOND, rapide) ; 2^999 accepté (301 chiffres) puis résultat réutilisé comme premier opérande pour « + 1 » ; (2^999)^999 refusé en moins de 1 s ; 99^999 refusé ; 10^999 refusé (l'estimation vaut 2*999 car normalize(10) a 2 chiffres) ; 0,0001^999 refusé ; 0,5^999 et 9^999 acceptés ; addition 999 chiffres « 9 » + 1 acceptée (1000 chiffres) et 1000 chiffres « 9 » + 1 refusée (1001 chiffres) ; produit de deux nombres de 600 chiffres refusé ; cas limites acceptés (addition de deux nombres de 1000 chiffres « 1 » chacun, soit un résultat de 1000 chiffres ; 2^999 ; 9^999) terminés en moins de 5 s avec assertTimeoutPreemptively ; présentateur : état d'erreur, focus correct, historique inchangé ; persistance : une ligne d'historique démesurée est ignorée (comportement E2) avec copie de sauvegarde, sans bloquer le chargement des autres lignes.

Hors périmètre : SwingWorker / calcul asynchrone (inutile avec les bornes), politique d'arrondi (Epic E5), limite du nombre d'entrées d'historique (volontairement aucune), accents (Epic E8). Validation à exécuter et prouver : « .\mvnw.cmd -B verify » vert (au moins les 23 tests de référence plus les nouveaux) ; chronométrage rapporté de (2^999)^999 refusé et de 2^999 accepté.

RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français, SANS accents jusqu'à l'Epic E8 (cohérence avec le code et les tests existants).
```

### Validation
1. `.\mvnw.cmd -B verify`.
2. Manuel (le comportement de l'interface réelle n'est pas couvert par les tests) : lance `run.ps1`, choisis « Puissance », calcule `2 ^ 999`, puis `999` en second nombre et Entrée, puis encore `999` : le deuxième enchaînement est refusé instantanément avec un message, sans gel. Vérifie aussi que la fenêtre reste utilisable avec un résultat de ~300 chiffres : si la fenêtre s'élargit ou que le résultat déborde de façon inacceptable, note-le (correctif d'affichage à traiter en E8).
3. Fusionne.

---

## E5 — Politique d'arrondi : plus de résultat non nul affiché « 0 »

**Priorité :** moyenne · **Modèle :** **Opus** · **Dépend de :** E3, E4 · **Branche :** `epic/e5-rounding-policy`

**Pourquoi Opus :** c'est le seul Epic qui change la **sémantique numérique** du produit. Il faut raisonner sur l'exactitude, la détection de racine exacte, le double arrondi, l'interaction avec les bornes de E4 et la cohérence avec l'historique rechargé. Une erreur ici est silencieuse et passe les tests superficiels, d'où le choix du modèle le plus fort.

### Objectif
Supprimer l'arrondi à échelle absolue qui transforme des résultats non nuls en `0`, rendre le comportement cohérent entre résultats exacts et approchés, et le documenter.

### Constats couverts
AV-3 (reproduit) : `1 / 3 000 000 000 000` → `0` ; `1 / 7 000 000 000 000` → `0` ; `3 ^ -30` → `0` ; `√(0,00000000000000000000000001)` → `0` (la racine exacte vaut `1E-13`) ; alors que `10 ^ -13` est conservé : comportement incohérent. En chaînage, ce `0` provoque ensuite de fausses erreurs de « division par zéro ». La règle des 12 décimales était documentée, mais pas la disparition des petites valeurs.

### Prérequis
E3 et E4 terminés et fusionnés.

### Fichiers concernés
`Calcul/DecimalPolicy.java` (`divide`, `sqrt`, `pow`, `roundApproximation`, constantes), `Calcul/Operation.java` (textes d'aide des opérations de division et de racine), `Graphique/CalculatorFrame.java` (texte d'aide du panneau résultat), tests du moteur, du présentateur et de l'interface, `README.md`.

### Règle (décision D3)
- Un résultat **exact** n'est **jamais** arrondi : division terminante même avec plus de 12 décimales (comportement actuel conservé), racine exacte (test `r × r == valeur`), puissance exacte.
- Un résultat **approché** est arrondi (HALF_UP) à `échelle = max(12, 11 − e)`, avec `e = precision − scale − 1` (exposant du premier chiffre significatif) : au moins 12 décimales **et** au moins 12 chiffres significatifs.
- Constantes renommées : `MIN_APPROXIMATION_DECIMALS = 12` et `MIN_SIGNIFICANT_DIGITS = 12`.
- Racine : calculer avec une précision supérieure à celle nécessaire (`value.precision() + 16` chiffres au moins) pour éviter tout double arrondi, détecter l'exactitude, puis appliquer la règle.

### Valeurs de référence (calculées indépendamment)
| Calcul | Résultat attendu |
|---|---|
| `1 / 3` | `0,333333333333` |
| `2 / 3` | `0,666666666667` |
| `1 / 30` | `0,0333333333333` |
| `1 / 3000000000000` | `0,000000000000333333333333` |
| `1 / 7000000000000` | `0,000000000000142857142857` |
| `3 ^ -30` | `0,00000000000000485693574962` |
| `√2` | `1,414213562373` |
| `√0,5` | `0,707106781187` |
| `√(2E-20)` (saisi `0,00000000000000000002`) | `0,000000000141421356237` |
| `√(0,00000000000000000000000001)` | `0,0000000000001` (exact) |
| `1000000000000000000000 / 3` | `333333333333333333333,333333333333` |
| `1 / 1048576` | `0,00000095367431640625` (exact, inchangé) |

### Critères d'acceptation
- [ ] Les 12 valeurs du tableau sont obtenues exactement.
- [ ] Pour une série déterministe (graine fixe) d'au moins 2 000 divisions `a / b` avec `b` jusqu'à `1E30` : le résultat n'est **jamais** `0` quand `a ≠ 0`, et l'erreur relative est au plus `5E-12` pour les résultats approchés (comparaison avec un calcul de référence à 60 chiffres).
- [ ] Idempotence : recalculer depuis les opérandes affichés d'une entrée d'historique redonne exactement la même valeur (aller-retour de persistance).
- [ ] Les 23 tests de référence passent ; seuls les tests qui comparent les **textes d'aide** modifiés sont adaptés.
- [ ] Les textes d'aide des opérations division et racine, l'aide du panneau résultat et le README décrivent la règle réelle.

### Commande Claude Code
```text
Contexte : dépôt « Calc », calculatrice Java/Swing en français (Maven, JUnit 5). Toute la politique numérique est centralisée dans Calcul/DecimalPolicy (normalize, divide, sqrt, pow, roundApproximation, APPROXIMATION_SCALE = 12, HALF_UP) et appliquée par CalculatorEngine. Les Epics E3 (InputField dans CalculationException) et E4 (bornes de taille : max(precision, scale) <= 1000, estimation des puissances) sont en place ; respecte-les.

Anomalie avérée (Epic E5) : l'arrondi se fait à 12 décimales ABSOLUES, donc des résultats non nuls disparaissent : 1/3000000000000 -> 0 ; 1/7000000000000 -> 0 ; 3^-30 -> 0 ; sqrt(0,00000000000000000000000001) -> 0 alors que la racine exacte vaut 1E-13 (sqrt applique toujours roundApproximation, même sur un résultat exact) ; alors que 10^-13 est conservé (résultat exact). Incohérent, et en chaînage ce 0 provoque de fausses erreurs « division par zéro ».

Crée la branche epic/e5-rounding-policy depuis main. Implémente exactement cette règle (décidée, ne la change pas) :
- Un résultat EXACT n'est JAMAIS arrondi : division terminante (y compris avec plus de 12 décimales, comportement actuel : 1/1048576 = 0,00000095367431640625), racine exacte (détecte-la en vérifiant r*r == valeur sur une racine calculée avec assez de précision), puissance exacte.
- Un résultat APPROCHÉ (division non terminante, racine non exacte, puissance négative non terminante) est arrondi HALF_UP à l'échelle max(12, 11 - e) où e = precision - scale - 1 (exposant du premier chiffre significatif), c'est-à-dire au moins 12 décimales ET au moins 12 chiffres significatifs.
- Renomme les constantes : APPROXIMATION_SCALE -> MIN_APPROXIMATION_DECIMALS (12) et ajoute MIN_SIGNIFICANT_DIGITS (12). Une seule fonction d'arrondi approché, utilisée par divide, sqrt et pow ; supprime les chemins redondants.
- sqrt : calcule avec une précision d'au moins value.precision() + 16 chiffres significatifs pour éviter tout double arrondi, détecte l'exactitude, puis applique la règle ci-dessus.
- Les résultats restent soumis aux bornes de l'Epic E4 (max(precision, scale) <= 1000).

Valeurs de référence que tes tests doivent reproduire EXACTEMENT : 1/3 = 0,333333333333 ; 2/3 = 0,666666666667 ; 1/30 = 0,0333333333333 ; 1/3000000000000 = 0,000000000000333333333333 ; 1/7000000000000 = 0,000000000000142857142857 ; 3^-30 = 0,00000000000000485693574962 ; sqrt(2) = 1,414213562373 ; sqrt(0,5) = 0,707106781187 ; sqrt(0,00000000000000000002) = 0,000000000141421356237 ; sqrt(0,00000000000000000000000001) = 0,0000000000001 (exact) ; 1000000000000000000000/3 = 333333333333333333333,333333333333 ; 1/1048576 = 0,00000095367431640625 (exact, inchangé).

Tests JUnit à ajouter : le tableau ci-dessus ; une série déterministe (java.util.Random avec graine fixe) d'au moins 2 000 divisions a/b avec b allant jusqu'à 1E30 : le résultat n'est jamais 0 quand a != 0 et l'erreur relative des résultats approchés est <= 5E-12 par rapport à un calcul de référence BigDecimal à 60 chiffres ; aller-retour : recalculer une entrée à partir des opérandes affichés redonne la même valeur (via FileCalculationHistoryStore avec @TempDir) ; le chaînage 1/3000000000000 puis division du résultat par lui-même ne produit plus de fausse erreur de division par zéro.

Mets à jour de façon cohérente les textes d'aide qui décrivent l'arrondi : Operation (aide de DIVISION et de ROOT, par ex. « Les divisions non terminantes sont arrondies a 12 decimales minimum (et 12 chiffres significatifs minimum). »), le texte d'aide du panneau résultat dans CalculatorFrame, et le README (section « Precision numerique »). Adapte UNIQUEMENT les tests qui comparent ces textes. Textes SANS accents.

Hors périmètre : nombres rationnels / exactitude symbolique (hors backlog), accents (Epic E8). Validation à exécuter et prouver : « .\mvnw.cmd -B verify » vert, 23 tests de référence conservés (hors attentes de textes d'aide) plus les nouveaux ; cite la valeur obtenue pour chaque ligne du tableau de référence.

RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français, SANS accents jusqu'à l'Epic E8 (cohérence avec le code et les tests existants).
```

### Validation
`.\mvnw.cmd -B verify`. Manuel : calcule `1 / 3000000000000` et vérifie que le résultat n'est plus `0`. Rappelle-toi qu'après redémarrage, les anciennes entrées d'historique valant `0` sont recalculées avec la nouvelle règle (comportement de normalisation déjà existant). Fusionne.


---

## E6 — Persistance : écriture atomique et format versionné

**Priorité :** moyenne · **Modèle :** Sonnet · **Dépend de :** E2 · **Branche :** `epic/e6-durable-format`

**Pourquoi Sonnet :** deux changements couplés dans une seule classe (chemin d'écriture, en-tête de format) avec des cas d'erreur à bien traiter, mais une spécification complète.

### Objectif
Éviter qu'un arrêt brutal en cours d'écriture ne laisse un fichier vide ou tronqué, rendre le format évolutif, et découpler les valeurs écrites sur disque des noms des constantes Java.

### Constats couverts
RI-1 (`Files.write` tronque puis écrit : un crash laisse un fichier vide ou partiel, ce qui ramène au problème AV-1 ; deux instances ouvertes simultanément s'écrasent, le dernier écrivain gagne), RI-2 (format TSV positionnel, non versionné, dépendant de `Operation.name()` : renommer une constante casse tous les historiques existants), RI-3 (évolutivité du modèle : le format versionné est le levier qui permettra une migration future ; le modèle à deux opérandes lui-même n'est **pas** modifié ici).

### Prérequis
E2 terminé et fusionné (copie de sauvegarde, mode protégé).

### Fichiers concernés
`Calcul/FileCalculationHistoryStore.java`, tests de persistance, `README.md` (limite d'instances multiples, finalisée en E10).

### Travail
1. **Écriture atomique** : écrire dans `history.tsv.tmp` (même dossier), forcer l'écriture disque (fsync), puis `Files.move` avec `ATOMIC_MOVE` et `REPLACE_EXISTING`. Si `ATOMIC_MOVE` n'est pas supporté, retomber sur `REPLACE_EXISTING`. En cas d'échec, supprimer le fichier temporaire et laisser l'original intact. Un `.tmp` orphelin est simplement écrasé à la sauvegarde suivante.
2. **Version** : la première ligne écrite est `#calc-history v1`. À la lecture : en-tête `v1` accepté ; **absence d'en-tête** = ancien format, accepté tel quel (la première sauvegarde ajoute l'en-tête) ; toute autre version (`vN`, N ≠ 1) = format plus récent, **non lu, non écrasé** (copie de sauvegarde et mode protégé de E2) avec le message d'historique irrécupérable.
3. **Clés de persistance explicites** : deux fonctions du magasin (`switch` exhaustif, sans `default`) convertissent `Operation` ↔ chaîne écrite (`ADDITION`, `SUBTRACTION`, `MULTIPLICATION`, `DIVISION`, `POWER`, `ROOT`). Ajouter une opération sans clé doit provoquer une **erreur de compilation**. Les valeurs écrites restent identiques à celles d'aujourd'hui.
4. **Instances multiples** : aucune action dans le code (décision D6). Le comportement « le dernier écrivain gagne » est documenté en E10.

### Critères d'acceptation
- [ ] Après une sauvegarde réussie, aucun fichier `.tmp` ne subsiste ; le fichier commence par `#calc-history v1`.
- [ ] Échec simulé de l'écriture (ex. `history.tsv.tmp` est un dossier) : l'original est intact et lisible.
- [ ] Un fichier sans en-tête (ancien format, avec la ligne `0.30000000000000004` de l'ancien test) est lu correctement ; il est réécrit avec l'en-tête à la sauvegarde suivante.
- [ ] En-tête `#calc-history v2` : aucune entrée lue, copie de sauvegarde créée, aucune écriture ne remplace le fichier.
- [ ] Test de contrat : pour chaque `Operation.values()`, la chaîne écrite correspond à la table ci-dessus et se relit en la même opération.
- [ ] Les tests de référence et ceux de E2 passent sans modification de leurs attentes.

### Commande Claude Code
```text
Contexte : dépôt « Calc », calculatrice Java/Swing en français (Maven, JUnit 5). Calcul/FileCalculationHistoryStore persiste l'historique en TSV (OPERATION, opérande 1, opérande 2, résultat). L'Epic E2 a déjà ajouté : chargement tolérant ligne par ligne (HistoryLoadResult), copie de sauvegarde horodatée .bak, mode protégé (saveHistory refuse d'écrire après un chargement problématique), journalisation java.util.logging.

Défauts à corriger (Epic E6) : (1) saveHistory utilise Files.write qui tronque puis écrit : un crash ou une coupure pendant l'écriture laisse un fichier vide ou partiel ; (2) le format n'a pas de version et dépend de Operation.name() : renommer une constante Java casse tous les historiques existants ; il n'existe aucun moyen de faire évoluer le format.

Crée la branche epic/e6-durable-format depuis main. Implémente :
1. Écriture atomique : écris dans « history.tsv.tmp » (même dossier), force l'écriture disque (FileChannel.force ou option SYNC), puis Files.move(tmp, fichier, ATOMIC_MOVE, REPLACE_EXISTING) ; si AtomicMoveNotSupportedException, retombe sur REPLACE_EXISTING seul. En cas d'échec : supprime le .tmp, laisse l'original INTACT, lève CalculationHistoryException (avec le chemin) et journalise la cause.
2. Version de format : la première ligne écrite est exactement « #calc-history v1 ». Lecture : en-tête v1 -> accepté ; AUCUN en-tête -> ancien format, accepté tel quel (la prochaine sauvegarde ajoute l'en-tête) ; toute autre version (« #calc-history vN », N != 1) -> format plus récent : n'en lis rien, n'écrase JAMAIS le fichier, réutilise la copie de sauvegarde et le mode protégé de l'Epic E2, et le présentateur affiche le message d'historique irrécupérable existant. Le BOM éventuel est ignoré avant l'en-tête.
3. Clés de persistance explicites : dans le magasin, deux méthodes privées (Operation -> String et String -> Operation) basées sur un switch exhaustif SANS default (nouvelle syntaxe switch d'expression Java 17), de sorte qu'ajouter une Operation sans clé casse la compilation. Valeurs écrites identiques à aujourd'hui : ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, POWER, ROOT. Une clé inconnue à la lecture rend la ligne invalide (comportement E2). N'utilise plus Operation.name() ni valueOf pour la persistance.
4. NE FAIS RIEN pour l'accès concurrent de plusieurs instances (pas de verrou de fichier) : décision actée, elle sera documentée plus tard.

Tests JUnit à ajouter : après sauvegarde réussie, pas de .tmp et première ligne = en-tête ; échec simulé d'écriture (par ex. en créant un DOSSIER nommé history.tsv.tmp) -> fichier original intact et lisible ; ancien fichier sans en-tête (inclure « ADDITION\t0,1\t0,2\t0.30000000000000004 ») lu correctement puis réécrit avec en-tête ; en-tête « #calc-history v2 » -> 0 entrée lue, copie .bak créée identique à l'original, aucune écriture ne remplace le fichier ; test de contrat itérant sur Operation.values() : la chaîne écrite correspond à la table ci-dessus et se relit en la même opération ; BOM + en-tête v1.

Hors périmètre : verrouillage inter-processus, changement du modèle de données (deux opérandes), bornes de taille déjà faites en E4. Validation à exécuter et prouver : « .\mvnw.cmd -B verify » vert, avec les tests de référence et ceux de l'Epic E2 inchangés dans leurs attentes ; montre le contenu des premières lignes d'un fichier produit.

RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français, SANS accents jusqu'à l'Epic E8 (cohérence avec le code et les tests existants).
```

### Validation
`.\mvnw.cmd -B verify`. Manuel : lance l'application, effectue un calcul, ouvre `%APPDATA%\Calc\history.tsv` et vérifie la première ligne `#calc-history v1`. Fusionne.

---

## E7 — Saisie numérique : espaces, milliers, ambiguïté du point

**Priorité :** basse · **Modèle :** Haiku · **Dépend de :** E3, E4 · **Branche :** `epic/e7-input-parsing`

**Pourquoi Haiku :** règles de normalisation précises, fournies avec leurs cas de test, dans une seule méthode.

### Objectif
Accepter ce qu'un francophone colle ou tape naturellement (espaces insécables, groupement des milliers) et lever l'ambiguïté du point, sans rendre l'analyse laxiste.

### Constats couverts
AV-7 (volet saisie), reproduit : `1 000` et `1 000` (espace insécable, fréquent en copier-coller de texte français) sont refusés avec le message générique ; `1.000` est lu comme `1`, ambiguïté pour un lecteur francophone ou allemand ; `trim()` ne retire pas les espaces insécables.

### Prérequis
E3 (champ typé) et E4 (la méthode `parseNumber` a été modifiée pour la longueur maximale) terminés.

### Fichiers concernés
`Calcul/CalculatorEngine.java` (`parseNumber`), `Calcul/DecimalPolicy.java`, `Calcul/Operation.java` (textes d'aide additions/soustraction/multiplication), tests du moteur, `README.md`.

### Règles (décision D7)
- Les espaces de bord sont retirés : espace, tabulation, espace insécable `U+00A0`, espace fine insécable `U+202F`.
- Séparateur de milliers accepté **uniquement** dans la partie entière et **uniquement** sous la forme `\d{1,3}([ U+00A0 U+202F]\d{3})+` : `1 000`, `12 345 678`, `1 000,5`. Les séparateurs sont supprimés avant l'analyse.
- Invalides : `1 00`, `1 0000`, `1 000 00`, `1,000.5`, `1.000.000`, `1e3`, chiffres non ASCII.
- Le point **reste** un séparateur décimal : `1.000` vaut `1` (documenté dans l'aide et le README).
- Le message d'invalidité existant reste inchangé (des tests le comparent). Le format de sortie ne change pas (pas de groupement), donc la réutilisation d'un résultat reste fiable.
- La limite de longueur de E4 s'applique avant tout traitement.

### Critères d'acceptation
- [ ] `1 000` → 1000 ; `1 000,5` → 1000,5 ; `1 000 000` → 1000000 ; ` 12 ` → 12.
- [ ] `1 00`, `1 0000`, `1,000.5`, `1.000.000`, `1e3`, `٣` refusés avec le message existant.
- [ ] `1.000` → 1.
- [ ] Aide des opérations arithmétiques de base : mentionne que le point est un séparateur décimal.
- [ ] Les tests de référence passent.

### Commande Claude Code
```text
Contexte : dépôt « Calc », calculatrice Java/Swing en français (Maven, JUnit 5). L'analyse des nombres est dans CalculatorEngine.parseNumber (trim, remplacement de la virgule par un point, regex DecimalPolicy.SUPPORTED_DECIMAL_INPUT). Les Epics E3 (InputField dans CalculationException) et E4 (longueur maximale de saisie 1005) sont en place ; respecte-les.

Défauts avérés (Epic E7) : « 1 000 » et « 1<NBSP>000 » (espace insécable U+00A0, courant en copier-coller de texte français, et U+202F) sont refusés avec le message générique ; trim() ne retire pas les espaces insécables ; « 1.000 » est lu comme 1, ce qui est ambigu pour un lecteur francophone.

Crée la branche epic/e7-input-parsing depuis main. Implémente dans parseNumber (après le contrôle de longueur de l'Epic E4, avant la regex) :
1. Retire en début et fin de saisie les espaces, tabulations, U+00A0 et U+202F (en plus de ce que fait déjà trim()).
2. Accepte un séparateur de milliers (espace, U+00A0 ou U+202F) UNIQUEMENT dans la partie entière et UNIQUEMENT sous la forme \d{1,3}([   ]\d{3})+ suivie éventuellement de [.,]\d* ; supprime alors ces séparateurs avant l'analyse. Tout autre espace interne reste invalide.
3. Reste invalide, avec le message d'invalidité EXISTANT inchangé (des tests le comparent) : « 1 00 », « 1 0000 », « 1 000 00 », « 1,000.5 », « 1.000.000 », « 1e3 », les chiffres non ASCII (par ex. U+0663).
4. Le point RESTE un séparateur décimal : « 1.000 » vaut 1. Documente-le : ajoute à l'aide (Operation.getInputHint) des ADDITION, SUBTRACTION et MULTIPLICATION la précision « Le point est un separateur decimal. » (vérifie qu'aucun test existant ne compare ces textes ; sinon adapte-le) et au README (section de précision numérique / saisie).
5. Ne change PAS le format de sortie (pas de groupement des milliers) : la réutilisation des résultats reste exacte. Textes SANS accents.

Tests JUnit à ajouter dans les tests du moteur : « 1 000 » -> 1000 ; « 1<NBSP>000,5 » -> 1000,5 ; « 1<U+202F>000<U+202F>000 » -> 1000000 ; « 12<NBSP> » (espace insécable final) -> 12 ; refus (message existant) de « 1 00 », « 1 0000 », « 1 000 00 », « 1,000.5 », « 1.000.000 », « 1e3 », « ٣ » ; « 1.000 » -> 1 ; l'historique d'une entrée saisie avec séparateurs affiche l'opérande normalisé sans espace.

Hors périmètre : groupement des milliers à l'affichage, notation scientifique, accents (Epic E8). Validation à exécuter et prouver : « .\mvnw.cmd -B verify » vert ; tests de référence inchangés.

RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français, SANS accents jusqu'à l'Epic E8 (cohérence avec le code et les tests existants).
```

### Validation
`.\mvnw.cmd -B verify`. Manuel : colle `1 000` (avec une espace insécable copiée d'une page web française) dans le premier champ et calcule. Fusionne.

---

## E8 — Interface : accents, apparence système, accessibilité

**Priorité :** basse · **Modèle :** Sonnet · **Dépend de :** E2 à E7 (toutes les chaînes d'interface doivent être figées avant d'être accentuées, sinon il faut tout retoucher deux fois) · **Branche :** `epic/e8-ui-polish`

**Pourquoi Sonnet :** changement large (toutes les chaînes visibles, plusieurs fichiers de tests) mais sans difficulté conceptuelle ; l'enjeu est l'exhaustivité, pour laquelle Sonnet suffit avec les contrôles prévus.

### Objectif
Faire de l'interface un produit qui paraît fini : français correct, apparence native de Windows, navigation clavier et technologies d'assistance.

### Constats couverts
AV-7 (volets interface) : tous les textes français sont sans accents alors que le code se compile en UTF-8 et que `√` est déjà écrit par échappement Unicode ; aucun look and feel système (apparence Metal par défaut) ; aucun `setLabelFor`, aucun mnémonique, aucun nom accessible. NV-2 (rendu visuel jamais vérifié). Suite de E4 : tenue de l'affichage avec un résultat très long.

### Prérequis
E2 à E7 terminés et fusionnés.

### Fichiers concernés
`Calcul/Operation.java`, `Calcul/CalculatorEngine.java` (messages), `Calcul/DecimalPolicy.java` (si messages), `Graphique/CalculatorFrame.java`, `Graphique/CalculatorPresenter.java`, `Main.java`, tous les tests qui comparent des chaînes, `README.md`.

### Travail
1. **Accents** : rétablir l'orthographe correcte dans **toutes** les chaînes visibles par l'utilisateur (libellés, aides, info-bulles, messages d'état et d'erreur du moteur et du présentateur, titre, texte d'aide des panneaux) ; le README suit. Écriture directe en UTF-8 (le `√` peut rester un échappement). Le journal reste en anglais ou français sans contrainte.
2. **Apparence** : `UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())` appelé au tout début du `invokeLater` de `Main`, dans un `try/catch` qui journalise l'échec et continue avec l'apparence par défaut. Vérifier que la taille minimale 940×450 de la fenêtre reste adaptée.
3. **Accessibilité clavier et lecteurs d'écran** : `setLabelFor` pour les trois libellés de saisie, mnémoniques **uniques** sur les libellés et boutons, `AccessibleContext` (nom et description) sur les champs, la liste d'historique, l'étiquette de résultat et le sélecteur d'opération. Ordre de tabulation vérifié.
4. **Résultat très long** (suite de E4) : le résultat peut atteindre 1000 chiffres. L'étiquette de résultat ne doit ni élargir la fenêtre ni être illisible : texte tronqué visuellement et **valeur complète en info-bulle**.
5. Mettre à jour toutes les attentes de tests qui comparent des textes.

### Critères d'acceptation
- [ ] Aucune chaîne visible sans accent quand l'orthographe en exige (revue exhaustive de la liste des littéraux, rapportée par Claude Code).
- [ ] Un test vérifie que les mnémoniques des boutons et libellés sont tous distincts, que chaque libellé a son `labelFor`, et que les composants clés ont un nom accessible non vide.
- [ ] Un test vérifie que la valeur complète d'un résultat long se retrouve dans l'info-bulle.
- [ ] Tests de référence adaptés **uniquement** sur les chaînes ; aucun changement de logique.
- [ ] Apparence système active (contrôle visuel) ; en cas d'échec de chargement, l'application démarre quand même.
- [ ] Le README et les messages concordent avec l'interface.

### Commande Claude Code
```text
Contexte : dépôt « Calc », calculatrice Java/Swing en français (Maven, JUnit 5). Les Epics E2 à E7 sont en place : les chaînes d'interface sont désormais figées. Jusqu'ici, par décision, TOUS les textes visibles sont écrits SANS accents (« Entree », « resultat », « operation », « reutiliser », « decimales », « carree », « negatif », « definie », « precedent »…). Le code se compile en UTF-8 et le « √ » est déjà écrit en échappement Unicode, donc l'absence d'accents n'a plus de raison d'être. Par ailleurs : aucun look and feel système (apparence Metal par défaut), aucun setLabelFor, aucun mnémonique, aucun nom accessible.

Mission de l'Epic E8 (qualité de l'interface). Crée la branche epic/e8-ui-polish depuis main. Implémente :
1. Rétablis l'orthographe française correcte (accents, cédilles, typographie) dans TOUTES les chaînes visibles par l'utilisateur : libellés et aides de Operation, messages d'erreur du moteur (CalculatorEngine, DecimalPolicy), messages d'état du présentateur, titre, libellés, info-bulles et textes d'aide de CalculatorFrame, ainsi que le README. Commence par produire la liste exhaustive des littéraux de chaîne concernés (grep de tous les littéraux dans src/main/java) et rapporte-la à la fin avec le statut de chacun. N'accentue pas les identifiants, ni les clés de persistance (ADDITION, POWER…), ni les valeurs écrites dans history.tsv.
2. Dans Main, au tout début du Runnable de SwingUtilities.invokeLater, applique UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) dans un try/catch qui journalise (java.util.logging) l'échec et continue avec l'apparence par défaut. Vérifie que la taille minimale 940x450 reste cohérente.
3. Accessibilité : setLabelFor pour « Premier nombre », « Operation/Opération » et le libellé du second nombre ; mnémoniques UNIQUES sur ces libellés et sur tous les boutons (Calculer, Reprendre dernier résultat, Effacer, Vers nombre 1, Vers nombre 2, Effacer historique — choisis des lettres distinctes et affiche-les soulignées) ; AccessibleContext (nom et description) sur les deux champs, le sélecteur d'opération, la liste d'historique et l'étiquette de résultat. Ordre de tabulation vérifié et logique.
4. Résultat très long (jusqu'à 1000 chiffres depuis l'Epic E4) : l'étiquette de résultat ne doit ni élargir la fenêtre ni se rendre illisible ; affiche-le tronqué visuellement (taille minimale à zéro / ellipsis) et place la valeur COMPLÈTE dans l'info-bulle.
5. Mets à jour les attentes de TOUS les tests qui comparent des chaînes (moteur, présentateur, persistance, interface). Ne modifie aucune logique ni aucun comportement.

Tests JUnit à ajouter : mnémoniques distincts (collecte sur les boutons et libellés) ; chaque libellé de saisie a un labelFor non nul pointant le bon champ ; noms accessibles non vides sur les composants clés ; l'info-bulle de l'étiquette de résultat contient la valeur complète d'un résultat long ; les tests d'interface restent ignorés sans affichage (mécanisme de l'Epic E1).

Hors périmètre : internationalisation / ResourceBundle, refonte graphique, nouveaux composants. Validation à exécuter et prouver : « .\mvnw.cmd -B verify » vert ; un grep des mots sans accents connus (resultat, etre, precedent, reutilis, decimale, carree, operation dans les littéraux) ne renvoie plus aucun littéral visible ; rapporte la liste des chaînes traitées.

RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français avec accents corrects (introduits par l'Epic E8).
```

### Validation
`.\mvnw.cmd -B verify`. **Contrôle visuel indispensable** (le rendu n'est pas vérifiable par test) : lance `run.ps1` et vérifie les accents, l'apparence native, les soulignements d'Alt+lettre, la tabulation, et le comportement avec un résultat de ~300 chiffres (`2 ^ 999`). Fusionne.


---

## E9 — Nettoyage structurel

**Priorité :** basse · **Modèle :** Sonnet · **Dépend de :** E8 (cet Epic touche tous les fichiers : il passe en dernier pour éviter les conflits) · **Branche :** `epic/e9-structure-cleanup`

**Pourquoi Sonnet :** refactoring large et mécanique, filet de sécurité de tests déjà en place ; chaque sous-étape est un commit vérifiable. Opus n'apporterait rien ici.

### Objectif
Aligner la structure sur les conventions Java et supprimer les redondances, **sans changer aucun comportement**. À faire par sous-étapes, un commit par sous-étape, tests verts après chacune.

### Constats couverts
PR-1 (packages `Calcul`, `Graphique`, `Tests` en majuscules contre la convention ; `Main` dans le package par défaut), PR-2 (texte d'interface dans l'enum de domaine `Operation` : nom affiché et aide ; `toString()` sert à l'affichage), PR-3 (`CalculatorViewModel` : 18 paramètres de constructeur, 4 booléens quasi identiques), PR-4 (`NumberFormatter` classe de simple délégation ; `getReusableValue()` doublon de `getFormattedResult()`), PR-7 (6 avertissements de compilation : `serialVersionUID` ×2 exceptions, `serialVersionUID` et champ non sérialisable de `CalculatorFrame`, `this-escape` ×2).

### Prérequis
E8 terminé et fusionné (chaînes finales, tests verts).

### Travail (une sous-étape = un commit)
1. **Packages** (avec `git mv`) : `Calcul` → `calc.engine`, `Graphique` → `calc.ui`, `Main` → `calc.Main`, tests dans les mêmes packages que ce qu'ils testent (`calc.engine`, `calc.ui`, ce qui permet de tester `DecimalPolicy` qui est local au package). Mettre à jour le pom (`Main-Class`), `package.ps1` (`--main-class calc.Main`), `run.ps1`, la CI, la section architecture du README. L'historique sur disque n'est pas affecté (les clés écrites sont découplées des noms de classes depuis E6).
2. **Texte hors du domaine** : `Operation` ne conserve que `historySymbol` et `requiresSecondOperand`. Le nom affiché et l'aide passent dans une classe de présentation de `calc.ui` (`switch` exhaustif sur `Operation`), le sélecteur utilise un `ListCellRenderer`. Les constantes de limites nécessaires aux aides sont exposées par une classe publique du moteur. **Les messages d'erreur du moteur restent dans le moteur (décision D8).**
3. **`CalculatorViewModel`** devient un `record`. Les booléens redondants (`reuseAsFirst`, `reuseLastResult`, `clearHistory`, tous égaux à « l'historique n'est pas vide ») sont dérivés d'une seule source ; adapter la vue et les tests.
4. **Suppressions** : `NumberFormatter` supprimée (appels redirigés vers `DecimalPolicy`) ; `getReusableValue()` supprimée au profit de `getFormattedResult()`.
5. **Avertissements** : `serialVersionUID` sur les exceptions et la fenêtre, `@SuppressWarnings("serial")` justifié pour le champ du présentateur de `CalculatorFrame`, classe `CalculatorFrame` déclarée `final` (supprime `this-escape`). Activer ensuite `-Xlint:all -Werror` dans le pom.

### Hors périmètre de l'Epic
Internationalisation (`ResourceBundle`), déplacement des messages d'erreur hors du moteur (D8), tout changement de comportement.

### Critères d'acceptation
- [ ] `.\mvnw.cmd -B verify` vert **après chaque sous-étape**, avec le même nombre de tests qu'avant l'Epic.
- [ ] Plus de package en majuscule ni de classe dans le package par défaut ; `git log --follow` retrouve l'historique des fichiers déplacés.
- [ ] `Operation` ne contient plus de texte d'interface.
- [ ] `NumberFormatter` et `getReusableValue()` n'existent plus ; `CalculatorViewModel` est un `record`.
- [ ] Compilation avec `-Xlint:all -Werror` : 0 avertissement.
- [ ] `run.ps1` et `package.ps1` fonctionnent ; `Calc.exe` démarre ; un historique existant se recharge à l'identique.

### Commande Claude Code
```text
Contexte : dépôt « Calc », calculatrice Java/Swing en français (Maven, JUnit 5, CI GitHub Actions). Les Epics E1 à E8 sont en place. Code actuel : Main (package par défaut), packages Calcul (moteur) et Graphique (présentateur + vue Swing), tests dans le package Tests. Mission de l'Epic E9 : nettoyage structurel SANS AUCUN CHANGEMENT DE COMPORTEMENT. Les tests existants sont ton filet : le nombre de tests doit rester identique avant/après, et « .\mvnw.cmd -B verify » doit être vert après CHAQUE sous-étape. Fais UN COMMIT par sous-étape.

Crée la branche epic/e9-structure-cleanup depuis main. Sous-étapes dans cet ordre :
1. Packages, avec git mv pour conserver l'historique : Calcul -> calc.engine ; Graphique -> calc.ui ; Main -> calc.Main ; les tests rejoignent le package de la classe testée (calc.engine, calc.ui). Mets à jour : pom.xml (Main-Class), package.ps1 (--main-class calc.Main), run.ps1, .github/workflows/ci.yml si nécessaire, la section « Architecture » du README. Vérifie qu'un fichier history.tsv existant se recharge à l'identique (les clés de persistance sont indépendantes des noms de classes).
2. Sors le texte d'interface du domaine : l'enum Operation ne garde que historySymbol et requiresSecondOperand. Le nom affiché et l'aide de saisie vont dans une classe de présentation de calc.ui (switch d'expression exhaustif sur Operation, sans default) ; le JComboBox utilise un ListCellRenderer à la place de toString(). Les valeurs de limites utilisées par les aides (exposant max, etc.) sont exposées par une classe publique du moteur. NE DÉPLACE PAS les messages d'erreur du moteur : ils restent dans le moteur (décision actée).
3. CalculatorViewModel devient un record Java. Dérive d'une seule source les booléens redondants (reuseAsFirstEnabled, reuseLastResultEnabled, clearHistoryEnabled valent tous « l'historique n'est pas vide » ; reuseAsSecondEnabled = historique non vide ET second opérande actif). Adapte la vue et les tests aux accesseurs de record.
4. Supprime NumberFormatter (redirige ses appels vers DecimalPolicy) et supprime CalculationRecord.getReusableValue() au profit de getFormattedResult() (adapte appelants et tests).
5. Corrige les 6 avertissements de compilation : serialVersionUID sur CalculationException et CalculationHistoryException ; sur CalculatorFrame ajoute serialVersionUID, @SuppressWarnings("serial") justifié pour le champ non sérialisable du présentateur, et déclare la classe final (supprime les avertissements this-escape). Active ensuite -Xlint:all -Werror dans le pom : la compilation doit être sans aucun avertissement.

Ne fais aucun autre changement : pas d'internationalisation, pas de nouvelle fonctionnalité, pas de modification des messages ni des comportements. Validation à exécuter et prouver : « .\mvnw.cmd -B verify » vert après chaque sous-étape avec le MÊME nombre de tests ; compilation sans avertissement ; run.ps1 lance l'application ; package.ps1 produit Calc.exe ; git log --follow sur un fichier déplacé montre l'historique antérieur.

RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français avec accents corrects (introduits par l'Epic E8).
```

### Validation
`.\mvnw.cmd -B verify`, `package.ps1`, lancement de `Calc.exe` et rechargement d'un historique existant. Pousse la branche pour vérifier que la CI reste verte sur les deux systèmes. Fusionne.

---

## E10 — Documentation finale et vérification de bout en bout

**Priorité :** basse · **Modèle :** Haiku · **Dépend de :** E1 à E9 · **Branche :** `epic/e10-final-docs`

**Pourquoi Haiku :** mise en cohérence documentaire guidée par une liste précise et vérification par exécution ; le contenu à écrire est déjà arrêté dans ce backlog.

### Objectif
Faire correspondre la documentation à la réalité du code corrigé, documenter les limites assumées, et exécuter une dernière validation de bout en bout couvrant chaque défaut de l'audit.

### Constats couverts
RI-1 (limite « dernier écrivain gagne » à documenter), RI-3 (limite d'évolutivité du modèle à deux opérandes), RI-4 (packaging Windows non signé, autres systèmes), PR-6 (historique non borné : décision de ne pas agir, à justifier), NV-1 et NV-2 (vérification finale du packaging et du rendu).

### Prérequis
E1 à E9 terminés et fusionnés.

### Travail
1. **README** : réécrire les sections obsolètes pour décrire exactement le comportement final : règle d'arrondi (exact jamais arrondi ; 12 décimales et 12 chiffres significatifs minimum, avec exemples du tableau d'E5) ; bornes de taille (1000 chiffres, 1005 caractères, estimation des puissances) ; règles de saisie (espaces et milliers, point décimal) ; historique (restauration partielle, copies `.bak`, écriture atomique, format `#calc-history v1`, mode protégé) ; architecture avec les packages `calc.engine` et `calc.ui` ; prérequis JDK 17 et commandes Maven ; CI.
2. **Limites connues** (à jour) : instances multiples (le dernier écrivain gagne, pas de verrou) ; historique non borné (mesuré : 5 000 calculs en 3,9 s cumulées, rechargement en 19 ms, donc non traité) ; messages d'erreur dans le moteur (pas d'internationalisation) ; modèle de calcul à deux opérandes (pas de saisie d'expression) ; packaging Windows uniquement, application non signée ; arithmétique décimale (pas de fractions exactes : un résultat approché peut être légèrement inexact après chaînage).
3. **Vérification de bout en bout** (scénarios ci-dessous) exécutée et rapportée.
4. Marquer tous les Epics comme terminés dans `BACKLOG.md`.

### Scénarios de vérification (un par défaut de l'audit)
| # | Scénario | Résultat attendu | Défaut |
|---|---|---|---|
| 1 | Historique de 4 lignes dont 1 corrompue, lancer, calculer | 3 lignes restaurées, message partiel, copie `.bak` identique | AV-1, AV-5 |
| 2 | `2 ^ 999`, puis `^ 999` | Refus immédiat, aucun gel | AV-2 |
| 3 | `1 / 3000000000000` | Résultat non nul | AV-3 |
| 4 | Puissance avec exposant `1,5` | Focus dans le second champ | AV-4 |
| 5 | Fichier d'historique commençant par un BOM | Chargé normalement | AV-1 |
| 6 | Saisie `1 000` avec espace insécable | Acceptée | AV-7 |
| 7 | Contrôle visuel : accents, apparence système, mnémoniques | Conforme | AV-7, NV-2 |
| 8 | `package.ps1` puis lancement de `Calc.exe` | Application fonctionnelle | NV-1, RI-4 |
| 9 | Fichier d'historique `#calc-history v2` | Non lu, non écrasé, copie créée | RI-2 |
| 10 | CI GitHub Actions | Verte sur Linux et Windows | AV-6 |

### Critères d'acceptation
- [ ] README cohérent avec le code (chaque affirmation vérifiée par lecture du code).
- [ ] Section « Limites connues » conforme à la liste ci-dessus.
- [ ] Les 10 scénarios exécutés, résultats rapportés (le n°10 et le n°7 nécessitent ton contrôle).
- [ ] `.\mvnw.cmd -B verify` vert ; aucun test ignoré sous Windows.
- [ ] `BACKLOG.md` : tous les Epics cochés.

### Commande Claude Code
```text
Contexte : dépôt « Calc », calculatrice Java/Swing en français (Maven, JUnit 5, CI). Les Epics E1 à E9 sont terminés : build Maven et CI, historique sûr (restauration partielle, copies .bak, mode protégé, écriture atomique, format « #calc-history v1 »), erreurs typées, bornes de taille (1000 chiffres affichables, saisie <= 1005 caractères, estimation des puissances), règle d'arrondi (résultat exact jamais arrondi ; approché arrondi à max(12 décimales, 12 chiffres significatifs)), saisie tolérante (espaces insécables, milliers, le point reste un séparateur décimal), interface accentuée et accessible, packages calc.engine / calc.ui / calc.Main. Mission de l'Epic E10 : documentation finale + vérification de bout en bout. AUCUNE modification de code de production.

Crée la branche epic/e10-final-docs depuis main. À faire :
1. Relis le code et mets le README en cohérence EXACTE avec le comportement réel (vérifie chaque affirmation dans le code, ne recopie pas ce prompt) : précision numérique avec exemples (1/3 = 0,333333333333 ; 1/3000000000000 = 0,000000000000333333333333 ; √2 = 1,414213562373 ; exact jamais arrondi) ; bornes de taille ; règles de saisie ; historique (restauration partielle, copies .bak, mode protégé, écriture atomique, en-tête de version) ; architecture avec les nouveaux packages ; prérequis JDK 17 et commandes Maven (verify, run, package) ; CI. Corrige toute affirmation devenue fausse.
2. Section « Limites connues », à jour : plusieurs instances ouvertes simultanément -> le dernier écrivain gagne (pas de verrou de fichier) ; historique non borné (mesuré : 5 000 calculs en 3,9 s cumulées, rechargement en 19 ms, donc volontairement non traité) ; messages d'erreur dans le moteur (pas d'internationalisation) ; modèle de calcul à deux opérandes sans saisie d'expression ; packaging Windows uniquement, application non signée ; arithmétique décimale sans fractions exactes (un résultat approché peut être légèrement inexact après chaînage).
3. Exécute et rapporte la vérification de bout en bout : « .\mvnw.cmd -B verify » ; puis, avec un fichier d'historique de TEST (jamais mon vrai fichier : pointe vers un dossier temporaire ou sauvegarde-le d'abord) les scénarios automatisables : (a) 4 lignes dont 1 corrompue -> 3 restaurées + copie identique ; (b) BOM ; (c) « #calc-history v2 » non lu, non écrasé ; (d) 2^999 puis ^999 refusé immédiatement ; (e) 1/3000000000000 non nul ; (f) exposant 1,5 -> focus second champ ; (g) saisie « 1<NBSP>000 » acceptée ; (h) package.ps1 puis démarrage de Calc.exe 5 secondes SANS calcul. Dis explicitement ce que tu n'as pas pu vérifier toi-même (rendu visuel, CI sur GitHub).
4. Dans BACKLOG.md, coche tous les critères et marque tous les Epics terminés.

Ne touche à aucun fichier de src/main/java. Ne modifie jamais le vrai %APPDATA%\Calc\history.tsv.

RÈGLES DE TRAVAIL (obligatoires) :
- Commence en mode plan : écris le plan dans tasks/todo.md (cases à cocher), puis implémente en cochant au fur et à mesure. Utilise des sous-agents pour les recherches et analyses parallèles quand c'est utile.
- Reste STRICTEMENT dans le périmètre de cet Epic : aucun refactoring ni correction hors de la liste. Si tu découvres un autre problème, note-le dans tasks/lessons.md sans le corriger.
- Cherche les causes racines, pas de contournement ; retiens la solution la plus simple qui satisfait les critères.
- Rien n'est « terminé » sans preuve : exécute la validation demandée et cite les résultats réels (nombre de tests, sorties).
- Commits : Conventional Commits, en anglais, courts, un commit par sous-étape logique ; aucune mention de Claude, d'une IA ou d'un assistant ; aucun trailer Co-Authored-By ni « Generated with ». Ne fais pas de git push.
- Termine avec un dépôt propre (git status vide) : tout est commité, y compris tasks/ ; coche les critères de l'Epic dans BACKLOG.md et inclus cette modification dans le dernier commit.
- Textes d'interface en français avec accents corrects (introduits par l'Epic E8).
```

### Validation
Exécute toi-même les scénarios 7 (contrôle visuel) et 10 (CI), que Claude Code ne peut pas confirmer. Fusionne : le backlog est terminé.

---

## 6. Matrice de traçabilité : audit → Epic

Chaque constat de l'audit est rattaché à au moins un Epic ou à une décision explicite. Statuts d'origine : **AV** = problème avéré (reproduit), **RI** = risque (déduit du code), **PR** = préférence d'architecture, **NV** = non vérifié.

| Constat de l'audit | Réf. | Epic(s) | Traitement |
|---|---|---|---|
| Perte de l'historique (une ligne invalide ou un BOM, puis réécriture) | AV-1 | E2 (E6 pour la corruption sur crash) | Corrigé |
| Gel de l'interface sur grands nombres (chaînage de puissances, analyse quadratique, rechargement) | AV-2 | E4 (+ E2) | Corrigé par bornes |
| Résultats non nuls affichés « 0 » (arrondi à échelle absolue, incohérence exact / approché) | AV-3 | E5 | Corrigé |
| Focus erroné sur les erreurs d'exposant (détection par texte) ; message trompeur d'exposant hors plage | AV-4 | E3 | Corrigé |
| Chargement tout-ou-rien ; aucun diagnostic ; entrées incohérentes acceptées | AV-5 | E2 | Corrigé |
| Tests embarqués dans le JAR ; test d'interface bloquant hors affichage ; moteur de test maison ; lacunes de couverture | AV-6 | E1 (+ tests ajoutés dans E2 à E8) | Corrigé |
| Textes sans accents | AV-7 | E8 | Corrigé |
| Saisie étroite (espaces insécables, milliers, ambiguïté du point) | AV-7 | E7 | Corrigé / documenté (D7) |
| Pas d'apparence système, pas d'accessibilité | AV-7 | E8 | Corrigé |
| Écriture non atomique ; instances multiples | RI-1 | E6 (atomique) ; E10 (limite documentée, D6) | Corrigé / assumé |
| Format non versionné, dépendant des noms d'enum | RI-2 | E6 | Corrigé |
| Évolutivité du modèle (deux opérandes figés) | RI-3 | Hors périmètre (relève de l'idée 2) ; levier posé par E6 ; limite documentée en E10 | Assumé |
| Packaging jamais exécuté, non signé, scripts réservés à Windows, pas de CI | RI-4 / NV-1 | E0 (vérification), E1 (Maven, CI), E10 (documentation) ; signature et installeurs multiplateformes exclus | Corrigé en partie / exclu |
| Packages en majuscules, `Main` en package par défaut | PR-1 | E9 | Corrigé |
| Texte d'interface dans l'enum de domaine ; messages dans le moteur | PR-2 | E9 (enum) ; D8 (messages conservés) | Corrigé / assumé |
| `CalculatorViewModel` à 18 paramètres et booléens redondants | PR-3 | E9 | Corrigé |
| `NumberFormatter` pass-through ; `getReusableValue()` doublon | PR-4 | E9 | Corrigé |
| Pas de Maven, pas de JUnit, JDK minimal non documenté | PR-5 | E1 (+ E10) | Corrigé |
| Historique non borné, réécriture complète à chaque calcul | PR-6 | Aucun (D10) ; justifié en E10 | **Non traité volontairement** (non-problème mesuré) |
| 6 avertissements de compilation | PR-7 | E9 | Corrigé |
| Internationalisation absente | PR-2 | Aucun | **Non traité volontairement** (déconseillé par l'audit) |
| Rendu visuel jamais vérifié | NV-2 | E4 (contrôle), E8, E10 | Contrôle humain |
| Historique Git de l'archive absent | NV-3 | E0 (dépôt cloné : 3 commits, vérifié) | Levé |

## 7. Non traité volontairement

| Sujet | Raison |
|---|---|
| Limite de taille de l'historique | Mesuré non problématique : 5 000 calculs en 3,9 s cumulées, 19 ms pour recharger 5 000 entrées. |
| Internationalisation (`ResourceBundle`) | Aucun besoin multilingue exprimé ; coût sans bénéfice à cette échelle. |
| Verrouillage inter-processus du fichier d'historique | Cas rare ; documenté plutôt que traité (D6). |
| Messages d'erreur hors du moteur (codes d'erreur) | Reviendrait à de l'internationalisation (D8). |
| Calcul en arrière-plan (`SwingWorker`) | Inutile une fois les tailles bornées (E4). |
| Framework MVVM, injection de dépendances, base de données | Non justifiés (conclusion de l'audit). |

## 8. Évolutions futures — hors périmètre de l'implémentation actuelle

> **Ces trois idées sont volontairement exclues.** Elles ne font partie d'aucun Epic. Elles sont conservées ici pour mémoire et bénéficieront du socle posé par les Epics ci-dessus.

1. **Moteur exact qui signale ses approximations.** Remplacer l'arrondi par une arithmétique de nombres rationnels exacts, chaque résultat portant un statut « exact » ou « approché » avec son erreur maximale (`(1 ÷ 3) × 3` redonnerait exactement `1`). *Socle posé :* E5 (règle d'arrondi documentée et centralisée).
2. **Carnet de calcul vivant.** Saisie d'expressions, variables nommées, références aux lignes précédentes, recalcul en cascade, annuler/rétablir, export Markdown/CSV/JSON. *Socle posé :* E6 (format versionné) ; c'est cette idée qui lèverait la limite du modèle à deux opérandes (RI-3).
3. **Fiabilité prouvée et livraison multiplateforme.** Tests par propriétés et différentiels contre un oracle indépendant, fuzzing du chargeur d'historique, installeurs pour plusieurs systèmes à partir d'un build reproductible et signé, rapport de conformité publié à chaque version. *Socle posé :* E1 (Maven, JUnit, CI Linux/Windows).
   **Frontière avec l'implémentation actuelle :** E1 inclut uniquement une CI de base (build et tests sur Linux et Windows). Restent exclus : tests par propriétés/différentiels, fuzzing, installeurs multiplateformes, signature du code, rapport de conformité. (Le test de série déterministe d'E5 est un contrôle de non-régression ponctuel, pas la démarche de l'idée 3.)

## 9. Annexe — Référence de non-régression (état initial)

Les 23 méthodes de test d'origine, à retrouver avec des noms identiques après la migration d'E1.

| Classe | Méthodes |
|---|---|
| `CalculatorEngineTest` (10) | `testAdditionSupportsCommaAndPoint`, `testDecimalAdditionAvoidsFloatingArtifacts`, `testRootUsesOnlyFirstOperand`, `testDivisionByZeroFailsClearly`, `testNegativeRootFailsClearly`, `testMissingSecondOperandFailsClearly`, `testInvalidFirstOperandFailsClearly`, `testFractionalPowerFailsClearly`, `testDivisionRoundingIsStableForReuse`, `testPowerCalculationIsFormattedForReuse` |
| `CalculatorPresenterTest` (7) | `testBinaryCalculationFeedsNextWorkflow`, `testChainedDecimalWorkflowKeepsVisiblePrecisionStable`, `testUnaryCalculationDisablesSecondOperandAndKeepsResultReusable`, `testInvalidInputReturnsActionableErrorState`, `testDivisionByZeroKeepsHistorySafe`, `testPowerHintExplainsSupportedScope`, `testHistoryReuseSupportsSelectionAndLatestResultShortcut` |
| `CalculatorHistoryPersistenceTest` (4) | `testHistoryPersistsAcrossSessions`, `testRoundedResultStaysStableAcrossPersistence`, `testLegacyFloatingHistoryIsNormalizedOnRestore`, `testInvalidPersistedHistoryDoesNotBlockStartup` |
| `CalculatorFrameTest` (2) | `testKeyboardShortcutsAndDefaultCalculationWorkflow`, `testUnaryModeAndHistoryReuseFlow` |

Total d'origine : 92 assertions (14 + 33 + 17 + 28). Les attentes de ces tests ne changent que dans les cas explicitement autorisés : message d'historique irrécupérable (E2), textes d'aide d'arrondi (E5), chaînes accentuées (E8), noms de packages et accesseurs de record (E9).
