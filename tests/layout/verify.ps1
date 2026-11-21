$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$classes = Join-Path $repo 'build/layout-check'
$layout = Join-Path $repo 'src/client/java/hu/pelyh/aoptools/EditorLayout.java'
$test = Join-Path $PSScriptRoot 'EditorLayoutCheck.java'
$jdk = $env:JAVA_HOME
if (-not $jdk -and $env:APPDATA) {
    $prismJdk = Join-Path $env:APPDATA 'PrismLauncher/java/java-runtime-delta'
    if (Test-Path (Join-Path $prismJdk 'bin/javac.exe')) { $jdk = $prismJdk }
}
$javac = if ($jdk) { Join-Path $jdk 'bin/javac' } else { 'javac' }
$java = if ($jdk) { Join-Path $jdk 'bin/java' } else { 'java' }
New-Item -ItemType Directory -Path $classes -Force | Out-Null
& $javac --release 21 -d $classes $layout $test
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp $classes hu.pelyh.aoptools.EditorLayoutCheck
exit $LASTEXITCODE
