param(
    [string[]]$MavenArgs = @()
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Import-KeyValueFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            return
        }

        $key = $parts[0].Trim()
        $value = $parts[1].Trim()

        if (-not $key) {
            return
        }

        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
}

Import-KeyValueFile -Path ".env.local"
Import-KeyValueFile -Path "secrets/application-secrets.properties"

if (-not $env:JOOQ_DB_PASSWORD -and $env:DB_PASSWORD) {
    # Fallback util cuando se reutiliza la misma credencial para runtime y jOOQ.
    $env:JOOQ_DB_PASSWORD = $env:DB_PASSWORD
}

$missing = @()
if (-not $env:JOOQ_DB_URL) { $missing += "JOOQ_DB_URL" }
if (-not $env:JOOQ_DB_USER) { $missing += "JOOQ_DB_USER" }
if (-not $env:JOOQ_DB_PASSWORD) { $missing += "JOOQ_DB_PASSWORD" }

if ($missing.Count -gt 0) {
    throw "Faltan variables para jOOQ: $($missing -join ', '). Configuralas en .env.local o secrets/application-secrets.properties"
}

$args = @("generate-sources") + $MavenArgs
& ".\mvnw.cmd" @args
exit $LASTEXITCODE
