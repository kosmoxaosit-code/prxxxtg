param(
  [ValidateSet('debug','release')][string]$Configuration = 'debug'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Require-Command([string]$Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Required command '$Name' is missing in PATH"
  }
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Path $dist -Force | Out-Null

$gradlewBat = Join-Path $root 'gradlew.bat'
if (-not (Test-Path $gradlewBat)) {
  Require-Command 'gradle'
  Push-Location $root
  try {
    gradle wrapper --gradle-version 8.7
  } finally {
    Pop-Location
  }
}

Push-Location $root
try {
  if ($Configuration -eq 'release') {
    & .\gradlew.bat clean :app:assembleRelease
    $apkPath = Join-Path $root 'app\build\outputs\apk\release\app-release.apk'
    $outName = 'tgwsproxy-android-universal-release.apk'
  } else {
    & .\gradlew.bat clean :app:assembleDebug
    $apkPath = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
    $outName = 'tgwsproxy-android-universal-debug.apk'
  }

  if (-not (Test-Path $apkPath)) {
    throw "APK was not produced at expected path: $apkPath"
  }

  $target = Join-Path $dist $outName
  Copy-Item -Path $apkPath -Destination $target -Force

  $hash = (Get-FileHash -Algorithm SHA256 $target).Hash.ToLowerInvariant()
  "$hash  $outName" | Out-File -Encoding ascii (Join-Path $root 'SHA256SUMS.txt')

  Write-Host "Built: $target"
  Write-Host "SHA256: $hash"
}
finally {
  Pop-Location
}
