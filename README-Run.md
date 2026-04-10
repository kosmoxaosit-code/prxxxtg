# README-Run

## Install
```bash
adb install -r dist/tgwsproxy-android-universal-debug.apk
```

## Smoke test
1. Open app and verify status = `stopped`.
2. Set bind address `127.0.0.1`, port `1080`.
3. Press `Start`, verify `running` + foreground notification.
4. Press `Stop`, verify status returns `stopped`.
5. Press `Restart`, verify service returns `running`.
6. Press `Apply in Telegram`; if deep-link not available, use copied manual SOCKS5 settings.
7. Try invalid port `0` and verify error validation.
8. Start another process on same port and verify `port busy` error.

## Manual Telegram settings
- Type: SOCKS5
- Host: 127.0.0.1
- Port: app-configured port
- Username/password: empty
