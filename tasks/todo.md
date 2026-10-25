# Todo

## Epic E1 - build foundation (Maven + JUnit 5 + CI), no behaviour change

Skills considered: none applies (no Maven/JUnit/CI skill available); working
from the Epic brief and the project CLAUDE.md. Branch: `epic/e1-build-foundation`.

- [x] 1. Layout: `git mv` Main.java, Calcul/, Graphique/ -> src/main/java/; Tests/ -> src/test/java/Tests/
- [x] 2. pom.xml (jar, finalName calc, release 17, UTF-8, -Xlint:all, JUnit 5, Surefire, Main-Class) + Maven Wrapper
- [x] 3. Convert 4 test classes to JUnit 5 (23 @Test; @TempDir; Assumptions for UI tests; calc.ui.required via Surefire)
- [x] 4. Scripts: check.ps1, run.ps1, package.ps1 (jar-only input dir, jpackage as before)
- [x] 5. .gitattributes + renormalize; .gitignore target/
- [x] 6. .github/workflows/ci.yml
- [x] 7. README: Prerequisites + commands
- [x] 8. Validation (a)-(f) with real output
- [x] 9. Tick E1 criteria in BACKLOG.md, review section here, commit, clean tree

## Review

Layout moved with `git mv` (history kept); pom + wrapper (Maven 3.9.11, JUnit 5.14.4,
Surefire 3.6.0); 23 `@Test` with original names (engine 10, presenter 7, persistence 4,
UI 2); scripts, .gitattributes, .gitignore, CI and README updated.

Validation (real output):
- (a) `.\mvnw.cmd -B verify`: BUILD SUCCESS, Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
- (b) `test -Djava.awt.headless=true`: BUILD SUCCESS, 23 run, Skipped: 2 (CalculatorFrameTest)
- (c) same + `-Dcalc.ui.required=true`: BUILD FAILURE, Failures: 2
- (d) `jar tf target\calc.jar`: 0 entries under Tests/
- (e) check.ps1 exit 0 (23 tests); run.ps1 started java -jar calc.jar (process seen, then stopped);
  package.ps1 exit 0 on rerun (first run: transient zip lock, see lessons) -> dist\Calc\Calc.exe, dist\Calc-portable.zip; dist\Calcpp holds only calc.jar
- (f) fresh clone with autocrlf=false: git status empty
- Not done by design: CI run on GitHub (user pushes), no git push.
