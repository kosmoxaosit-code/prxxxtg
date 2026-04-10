param(
  [string]$SourceRepo = "https://github.com/amurcanov/tg-ws-proxy-android.git",
  [string]$Branch = "main",
  [string]$Configuration = "debug"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Require-Command([string]$Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Required command '$Name' is not available in PATH."
  }
}

function Resolve-AndroidSdk {
  if ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) { return $env:ANDROID_SDK_ROOT }
  if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) { return $env:ANDROID_HOME }

  $default = Join-Path $env:LOCALAPPDATA "Android\Sdk"
  if (Test-Path $default) { return $default }

  throw "Android SDK not found. Set ANDROID_SDK_ROOT (or ANDROID_HOME)."
}

Require-Command "git"
$SdkRoot = Resolve-AndroidSdk

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildRoot = Join-Path $root ".build"
$srcRoot = Join-Path $buildRoot "src"
$dist = Join-Path $root "dist"

if (Test-Path $buildRoot) { Remove-Item -Recurse -Force $buildRoot }
New-Item -ItemType Directory -Path $buildRoot | Out-Null
New-Item -ItemType Directory -Path $dist -Force | Out-Null

Write-Host "[1/5] Cloning source repository..."
git clone --depth 1 --branch $Branch $SourceRepo $srcRoot

if (-not (Test-Path (Join-Path $srcRoot "gradlew"))) {
  throw "Cloned repository does not contain gradlew."
}

Push-Location $srcRoot
try {
  if ($IsWindows) {
    $gradleCmd = ".\\gradlew.bat"
  } else {
    $gradleCmd = "./gradlew"
  }

  Write-Host "[2/5] Building Android APK ($Configuration)..."
  if ($Configuration -ieq "release") {
    & $gradleCmd clean :app:assembleRelease
    $apkPath = Join-Path $srcRoot "app\build\outputs\apk\release\app-release.apk"
    $outName = "tgwsproxy-android-universal-release.apk"
  } else {
    & $gradleCmd clean :app:assembleDebug
    $apkPath = Join-Path $srcRoot "app\build\outputs\apk\debug\app-debug.apk"
    $outName = "tgwsproxy-android-universal-debug.apk"
  }

  if (-not (Test-Path $apkPath)) {
    throw "Build completed but APK was not found at: $apkPath"
  }

  Write-Host "[3/5] Copying APK to dist/..."
  $targetApk = Join-Path $dist $outName
  Copy-Item -Force $apkPath $targetApk

  Write-Host "[4/5] Calculating SHA256..."
  $hash = (Get-FileHash -Algorithm SHA256 $targetApk).Hash.ToLowerInvariant()
  "$hash  $outName" | Out-File -Encoding ASCII (Join-Path $root "SHA256SUMS.txt")

  Write-Host "[5/5] Done."
  Write-Host "APK: $targetApk"
  Write-Host "SHA256: $hash"
}
finally {
  Pop-Location
}
