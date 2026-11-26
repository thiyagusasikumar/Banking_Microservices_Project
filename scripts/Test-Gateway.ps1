#requires -Version 7.4
[CmdletBinding()]
param(
    [string]$GatewayUrl = 'http://localhost:8090',
    [string]$EurekaUrl = 'http://localhost:8761',
    [int]$TimeoutSeconds = 90
)
$ErrorActionPreference = 'Stop'
$expected = @('API-GATEWAY','AUTH-SERVICE','ACCOUNT-SERVICE','TRANSACTION-SERVICE',
    'LEDGER-SERVICE','NOTIFICATION-SERVICE','APPROVAL-SERVICE','RECONCILIATION-SERVICE')
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
do {
    $missing = @($expected)
    try {
        $registry = Invoke-RestMethod "$EurekaUrl/eureka/apps" -Headers @{Accept='application/json'} -TimeoutSec 5
        $applications = @($registry.applications.application)
        $missing = @($expected | Where-Object {
            $name = $_
            -not ($applications | Where-Object { $_.name -eq $name -and 'UP' -in $_.instance.status })
        })
    } catch { Write-Verbose $_.Exception.Message }
    if ($missing.Count -eq 0) { break }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)
if ($missing.Count -gt 0) { throw "Missing UP registrations: $($missing -join ', ')" }
foreach ($url in @("$EurekaUrl/actuator/health", "$GatewayUrl/actuator/health")) {
    $health = Invoke-RestMethod $url -TimeoutSec 10
    if ($health.status -ne 'UP') { throw "Health check failed: $url" }
}
$protected = Invoke-WebRequest "$GatewayUrl/api/v1/accounts/me" -SkipHttpErrorCheck -TimeoutSec 10
if ($protected.StatusCode -ne 401) { throw "Expected 401 without a token; got $($protected.StatusCode)" }
$applications | ForEach-Object {
    [pscustomobject]@{Service=$_.name;Status=($_.instance.status -join ',');Port=($_.instance.port.'$' -join ',')}
} | Sort-Object Service | Format-Table
Write-Host 'PASS: all eight clients registered UP; registry and gateway healthy; anonymous account access rejected.'
