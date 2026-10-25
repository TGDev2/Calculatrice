# Lessons

Every correction from the user becomes a rule here, so the same mistake is not
repeated. Loaded automatically at the start of each session.

Format: `- <trigger> -> <rule>`

- _no lessons yet_- Writing file content via an inline Python heredoc with non-raw strings -> backslash sequences (`\r`, `\t`) are interpreted; write files with a raw string or the Write/Edit tools, then grep the result.
- `package.ps1` Compress-Archive can fail once with a lock on `dist\Calc\app\calc.jar` right after jpackage (transient, passed on rerun) -> if it recurs, investigate antivirus/handle locks; out of scope for E1.
- `mvn` installed by scoop is not on the PATH of already-open shells -> use `mvnw.cmd` (wrapper) or add `~/scoop/apps/maven/current/bin` to PATH.
