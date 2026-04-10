# tgwsproxy-android (self-contained)

This repository now contains a self-contained Android project with:

- localhost-only SOCKS5 proxy service (`127.0.0.1` by default),
- foreground service with start/stop/restart,
- hardened manifest defaults (no backup, no `QUERY_ALL_PACKAGES`, non-exported service),
- internal app log buffer (without global logcat operations),
- Windows build scripts producing APK in `dist/`.

## Build (Windows)

```bat
build-apk.bat
```

Output:
- `dist/tgwsproxy-android-universal-debug.apk`
- `SHA256SUMS.txt`
