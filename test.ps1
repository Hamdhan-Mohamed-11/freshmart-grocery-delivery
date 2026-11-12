# Compiles and runs the regression tests; results are also saved to docs/evidence/test-results.txt
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force out, docs/evidence | Out-Null
$sources = @(Get-ChildItem -Path "src/freshmart/*.java", "test/freshmart/*.java" | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 -Xlint:all -d out $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$results = & java -cp out freshmart.EUK00361749_Tests
$testExitCode = $LASTEXITCODE
$results | Set-Content -Encoding UTF8 docs/evidence/test-results.txt
$results
exit $testExitCode
