$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    docker compose up
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
