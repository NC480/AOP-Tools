param([string] $Version)

# Uses a disposable APPDATA directory and an already-built JAR. No game launch.
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if (!$Version) { $Version = (Get-Content (Join-Path $repo 'gradle.properties') | ConvertFrom-StringData).mod_version }
$name = "aop-tools-$Version.jar"
$built = Get-Item -LiteralPath (Join-Path $repo "build/libs/$name")
$expected = (Get-FileHash -LiteralPath $built.FullName).Hash
$originalAppData = $env:APPDATA
$originalJava = $env:JAVA_HOME
$fixture = Join-Path ([IO.Path]::GetTempPath()) ("aop-deploy-test-" + [Guid]::NewGuid().ToString('N'))
$instance = Join-Path $fixture 'PrismLauncher/instances/huncut'
$mods = Join-Path $instance '.minecraft/mods'
$target = Join-Path $mods $name
function Invoke-Deploy([bool] $shouldSucceed) {
    # Windows PowerShell 5 converts redirected stderr into ErrorRecords.
    $ErrorActionPreference = 'Continue'
    $out = & (Join-Path $repo 'gradlew.bat') -p $repo "-Pmod_version=$Version" --offline --console=plain deployToHuncut -x remapJar 2>&1
    if (($LASTEXITCODE -eq 0) -ne $shouldSucceed) { throw ($out -join [Environment]::NewLine) }
}
function Require([bool] $passed, [string] $message) { if (!$passed) { throw $message } }

try {
    if (!$env:JAVA_HOME) { $env:JAVA_HOME = Join-Path $originalAppData 'PrismLauncher/java/java-runtime-delta' }
    $env:APPDATA = $fixture
    New-Item -ItemType Directory -Path $mods -Force | Out-Null
    New-Item -ItemType File -Path (Join-Path $instance 'instance.cfg') | Out-Null
    $unrelated = Join-Path $mods 'aop-tools-other-addon.jar'
    [IO.File]::WriteAllText($unrelated, 'unrelated file')
    Invoke-Deploy $true
    Require ((Get-FileHash $target).Hash -eq $expected) 'Clean install differs from build.'
    $lock = [IO.File]::Open($target, 'Open', 'Read', 'Read')
    try { Invoke-Deploy $true } finally { $lock.Dispose() }

    $oldA = Join-Path $mods 'aop-tools-old-a.jar'
    $oldB = Join-Path $mods 'aop-tools-old-b.jar'
    Move-Item -LiteralPath $target -Destination $oldA
    Copy-Item -LiteralPath $oldA -Destination $oldB
    $lock = [IO.File]::Open($oldB, 'Open', 'Read', 'Read')
    try { Invoke-Deploy $false } finally { $lock.Dispose() }
    Require (!(Test-Path $target)) 'Partial install after a locked old JAR.'
    foreach ($old in $oldA,$oldB) {
        Require ((Get-FileHash $old).Hash -eq $expected) 'Original JAR was lost.'
    }
    [IO.File]::WriteAllText($target, 'occupied')
    Invoke-Deploy $false
    Require (([IO.File]::ReadAllText($target)) -eq 'occupied') 'A different file was overwritten.'
    Remove-Item -LiteralPath $target
    Invoke-Deploy $true
    Require ((Get-FileHash $target).Hash -eq $expected) 'Retry failed.'
    Require ((Test-Path $unrelated) -and !(Test-Path $oldA) -and !(Test-Path $oldB)) 'Incorrect cleanup.'
    'Deployment checks passed (clean, repeat, locked upgrade, name conflict, retry).'
} finally {
    $env:APPDATA = $originalAppData
    $env:JAVA_HOME = $originalJava
    if (Test-Path -LiteralPath $fixture) {
        $resolved = (Resolve-Path -LiteralPath $fixture).Path
        if ($resolved -ne [IO.Path]::GetFullPath($fixture) -or
                [IO.Path]::GetFileName($resolved) -notmatch '^aop-deploy-test-[0-9a-f]{32}$') { throw "Unexpected test directory: $resolved" }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
