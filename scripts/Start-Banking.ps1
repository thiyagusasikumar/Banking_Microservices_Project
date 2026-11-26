#requires -Version 7.4
[CmdletBinding()]
param(
    [string[]]$Services = @('eureka-server', 'auth-service', 'account-service', 'transaction-service',
        'ledger-service', 'notification-service', 'approval-service', 'reconciliation-service', 'api-gateway'),
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$runtimeDir = Join-Path $projectRoot '.local'
$logDir = Join-Path $runtimeDir 'logs'
New-Item -ItemType Directory -Force $logDir | Out-Null
$ports = @{'eureka-server'=8761; 'api-gateway'=8090; 'auth-service'=8081; 'account-service'=8082;
    'transaction-service'=8083; 'ledger-service'=8084; 'notification-service'=8085;
    'approval-service'=8086; 'reconciliation-service'=8087}
foreach ($service in $Services) {
    if (-not $ports.ContainsKey($service)) { throw "Unknown service: $service" }
}
$environmentPath = Join-Path $runtimeDir 'service-environment.json'
$environments = if (Test-Path $environmentPath) { Get-Content $environmentPath -Raw | ConvertFrom-Json -AsHashtable } else { @{} }
$pidFile = Join-Path $runtimeDir 'processes.json'
$processes = if (Test-Path $pidFile) { Get-Content $pidFile -Raw | ConvertFrom-Json -AsHashtable } else { @{} }
# Resolve the real JVM, avoiding Windows javapath launchers that spawn an untracked child.
$javaSettings = & java -XshowSettings:properties -version 2>&1 | Out-String
$javaHomeMatch = [regex]::Match($javaSettings, '(?m)^\s*java.home\s*=\s*(.+)$')
if (-not $javaHomeMatch.Success) { throw 'Install JDK 21 and add java to PATH.' }
$javaExe = Join-Path $javaHomeMatch.Groups[1].Value.Trim() 'bin\java.exe'
foreach ($service in $Services) {
    $jar = Join-Path $projectRoot "$service\target\$service-0.0.1-SNAPSHOT.jar"
    if ($processes.ContainsKey($service)) {
        $existing = Get-CimInstance Win32_Process -Filter "ProcessId = $($processes[$service].pid)"
        if ($existing -and $existing.Name -eq 'java.exe' -and $existing.CommandLine -and $existing.CommandLine.Contains($jar)) {
            Write-Host "$service already running (PID $($existing.ProcessId))."
            continue
        }
    }
    $serviceEnvironment = if ($environments.ContainsKey($service)) { $environments[$service] } else { @{} }
    $port = if ($serviceEnvironment.ContainsKey('SERVER_PORT')) { [int]$serviceEnvironment.SERVER_PORT } else { $ports[$service] }
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        throw "Port $port is already occupied. Resolve it before starting $service."
    }
    if (-not $SkipBuild) {
        $buildLog = Join-Path $logDir "$service-build.log"
        & (Join-Path $projectRoot "$service\mvnw.cmd") -B -ntp -f (Join-Path $projectRoot "$service\pom.xml") -DskipTests package *> $buildLog
        if ($LASTEXITCODE -ne 0) { throw "Build failed for $service. See $buildLog" }
    }
    if (-not (Test-Path $jar)) { throw "Missing $jar. Run without -SkipBuild first." }
    $process = Start-Process -FilePath $javaExe -ArgumentList @('-jar', ('"' + $jar + '"')) `
        -WorkingDirectory $projectRoot -Environment $serviceEnvironment -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logDir "$service.log") `
        -RedirectStandardError (Join-Path $logDir "$service-error.log")
    $processes[$service] = @{ pid=$process.Id; jar=$jar; port=$port }
    $processes | ConvertTo-Json -Depth 5 | Set-Content $pidFile
    $deadline = (Get-Date).AddSeconds(120)
    do {
        $process.Refresh()
        if ($process.HasExited) { throw "$service exited. See $logDir\$service.log and $service-error.log" }
        $listening = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($listening) { break }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    if (-not $listening) { throw "$service has not opened port $port. See its logs in $logDir." }
    Write-Host "$service listening on port $port (PID $($process.Id))."
}
Write-Host 'Service processes started. Allow Eureka time to refresh its registry.'
