param([switch]$Checks)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if (Test-Path -LiteralPath '.tools/java') {
    $env:JAVA_HOME = (Get-ChildItem -LiteralPath '.tools/java' -Directory | Select-Object -First 1).FullName
}
if (Test-Path -LiteralPath '.tools/sdk') { $env:ANDROID_HOME = Join-Path $projectRoot '.tools/sdk' }
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.build-cache'
$localGradle = Join-Path $projectRoot '.tools/gradle/gradle-8.13/bin/gradle.bat'
$gradleCommand = if (Test-Path -LiteralPath $localGradle) { $localGradle } else { 'gradle' }
$tasks = if ($Checks) { @('testDebugUnitTest','lintDebug','assembleDebug') } else { @('assembleDebug') }
& $gradleCommand --no-daemon --console=plain @tasks
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$outputDir = Join-Path $projectRoot 'outputs'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
Copy-Item -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk' -Destination (Join-Path $outputDir 'Crossing-Block-Seeds.apk') -Force
Write-Output (Join-Path $outputDir 'Crossing-Block-Seeds.apk')
