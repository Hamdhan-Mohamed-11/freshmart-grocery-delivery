$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force out | Out-Null
$sources = @(Get-ChildItem src/freshmart/*.java | ForEach-Object {$_.FullName})
& java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -Xlint:all -d out @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp out freshmart.EUK00361749_Main @args
exit $LASTEXITCODE
