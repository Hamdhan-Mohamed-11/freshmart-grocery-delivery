# Compiles the project and runs the automated tests twice:
#   1. JUnit 5 (lib/junit-platform-console-standalone) - one JUnit test per table row
#   2. The table runner, which saves the report's test table to docs/evidence/test-results.txt
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$junit = "lib/junit-platform-console-standalone-1.11.4.jar"
New-Item -ItemType Directory -Force out, docs/evidence | Out-Null
$sources = @(Get-ChildItem -Path "src/freshmart/*.java", "test/freshmart/*.java" | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 -Xlint:all -cp $junit -d out $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& java -jar $junit execute --class-path out --select-class freshmart.EUK00361749_JUnitTests --details=tree --disable-banner
$junitExitCode = $LASTEXITCODE

$results = & java -cp out freshmart.EUK00361749_Tests
$tableExitCode = $LASTEXITCODE
$results | Set-Content -Encoding UTF8 docs/evidence/test-results.txt
Write-Output ""
Write-Output "Test table saved to docs/evidence/test-results.txt"
Write-Output $results[-1]
if ($junitExitCode -ne 0) { exit $junitExitCode }
exit $tableExitCode
