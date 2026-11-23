$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$classes = Join-Path $repo 'build/preset-check'
$block = Join-Path $repo 'src/client/java/hu/pelyh/aoptools/WorkflowBlock.java'
$preset = Join-Path $repo 'src/client/java/hu/pelyh/aoptools/WorkflowPreset.java'
$test = Join-Path $PSScriptRoot 'PresetCheck.java'
$jdk = $env:JAVA_HOME
if (-not $jdk -and $env:APPDATA) {
    $prismJdk = Join-Path $env:APPDATA 'PrismLauncher/java/java-runtime-delta'
    if (Test-Path (Join-Path $prismJdk 'bin/javac.exe')) { $jdk = $prismJdk }
}
$javac = if ($jdk) { Join-Path $jdk 'bin/javac' } else { 'javac' }
$java = if ($jdk) { Join-Path $jdk 'bin/java' } else { 'java' }
New-Item -ItemType Directory -Path $classes -Force | Out-Null
& $javac --release 21 -d $classes $block $preset $test
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp $classes hu.pelyh.aoptools.PresetCheck
exit $LASTEXITCODE
