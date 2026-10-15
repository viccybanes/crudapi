$ErrorActionPreference = 'Stop'
# Use the JDK on PATH, even if JAVA_HOME still points to an older installation.
$settings = & java -XshowSettings:properties -version 2>&1
$env:JAVA_HOME = (($settings | Select-String '^\s*java.home = ').Line -split '=', 2)[1].Trim()
Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd -q compile exec:java
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
