$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force out | Out-Null
$sources = @(Get-ChildItem -Path "src/freshmart/*.java","test/freshmart/*.java" | ForEach-Object {$_.FullName})
& java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -Xlint:all -d out @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp out freshmart.EUK00361749_Tests @args
exit $LASTEXITCODE
