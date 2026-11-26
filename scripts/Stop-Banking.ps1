#requires -Version 7.4
[CmdletBinding()]
param([string[]]$Services)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$pidFile = Join-Path $projectRoot '.local\processes.json'
if (-not (Test-Path $pidFile)) { Write-Host 'No managed processes recorded.'; return }
$processes = Get-Content $pidFile -Raw | ConvertFrom-Json -AsHashtable
$targets = if ($Services) { $Services } else { @($processes.Keys) }
foreach ($service in $targets) {
    if (-not $processes.ContainsKey($service)) { continue }
    $entry = $processes[$service]
    $expectedJar = Join-Path $projectRoot "$service\target\$service-0.0.1-SNAPSHOT.jar"
    if ($entry.jar -ne $expectedJar) { throw "Unexpected recorded jar for $service; refusing to stop it." }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($entry.pid)"
    if ($process) {
        if ($process.Name -ne 'java.exe' -or -not $process.CommandLine -or -not $process.CommandLine.Contains($expectedJar)) {
            throw "PID $($entry.pid) no longer belongs to $service; refusing to stop it."
        }
        Stop-Process -Id $entry.pid
        Wait-Process -Id $entry.pid -Timeout 30 -ErrorAction SilentlyContinue
        Write-Host "Stopped $service."
    }
    $processes.Remove($service)
}
$processes | ConvertTo-Json -Depth 5 | Set-Content $pidFile
