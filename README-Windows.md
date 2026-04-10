# Windows one-click APK build

## Goal
Build installable APK in one action and put it in `dist/`.

## Requirements
1. Windows 10/11
2. Android Studio (includes SDK + platform tools)
3. JDK 17
4. Git
5. PowerShell 5+

## Environment
Set one variable (if Android Studio is not in default path):

```powershell
setx ANDROID_SDK_ROOT "C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk"
```

## Build
From repository root:

```bat
build-apk.bat
```

Optional release build:

```bat
build-apk.bat -Configuration release
```

## Output
- APK: `dist/tgwsproxy-android-universal-debug.apk` (or release variant)
- SHA256: `SHA256SUMS.txt`

## Notes
- Script clones `amurcanov/tg-ws-proxy-android` into temporary `.build/src` and runs Gradle wrapper there.
- If repository branch differs from `main`, pass `-Branch <name>`.
