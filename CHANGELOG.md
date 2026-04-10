# Changelog

## 2026-04-10
- Replaced scaffold-only repository with a self-contained Android application project.
- Implemented a localhost-only SOCKS5 proxy service with foreground lifecycle controls.
- Added hardened AndroidManifest defaults (backup disabled, minimized package visibility, non-exported service).
- Added internal application logging model (in-memory, redaction, no logcat clearing).
- Updated Windows build scripts to build from current repository sources and produce APK + SHA256.
