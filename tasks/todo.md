# Todo

## Epic E1 - build foundation (Maven + JUnit 5 + CI), no behaviour change

Skills considered: none applies (no Maven/JUnit/CI skill available); working
from the Epic brief and the project CLAUDE.md. Branch: `epic/e1-build-foundation`.

- [ ] 1. Layout: `git mv` Main.java, Calcul/, Graphique/ -> src/main/java/; Tests/ -> src/test/java/Tests/
- [ ] 2. pom.xml (jar, finalName calc, release 17, UTF-8, -Xlint:all, JUnit 5, Surefire, Main-Class) + Maven Wrapper
- [ ] 3. Convert 4 test classes to JUnit 5 (23 @Test; @TempDir; Assumptions for UI tests; calc.ui.required via Surefire)
- [ ] 4. Scripts: check.ps1, run.ps1, package.ps1 (jar-only input dir, jpackage as before)
- [ ] 5. .gitattributes + renormalize; .gitignore target/
- [ ] 6. .github/workflows/ci.yml
- [ ] 7. README: Prerequisites + commands
- [ ] 8. Validation (a)-(f) with real output
- [ ] 9. Tick E1 criteria in BACKLOG.md, review section here, commit, clean tree

## Review

_To fill after validation._
