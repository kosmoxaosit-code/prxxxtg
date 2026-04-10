# README-Windows

## 1-click build

```bat
build-apk.bat
```

Release variant:

```bat
build-apk.bat -Configuration release
```

## Prerequisites
- Android Studio (SDK installed)
- JDK 17
- PowerShell
- Optional: `gradle` in PATH only for first run (to generate wrapper if missing)

## Output
- `dist/tgwsproxy-android-universal-debug.apk` (default)
- `SHA256SUMS.txt`
