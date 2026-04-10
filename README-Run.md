# Run & smoke-test guide

## Install APK
```bash
adb install -r dist/tgwsproxy-android-universal-debug.apk
```

## Basic smoke checks
1. Launch app.
2. Verify status is `stopped` on first open.
3. Start proxy.
4. Confirm foreground notification appears.
5. Stop proxy.
6. Restart proxy and verify it returns to running state.

## Telegram setup (manual fallback)
In Telegram proxy settings:
- Type: SOCKS5
- Host: `127.0.0.1`
- Port: value shown in app (default should be local only)
- User/password: leave empty unless app explicitly requires

## Error handling checks
- Set invalid port (e.g. 0) and verify user-visible validation error.
- Occupy proxy port with another process and verify `port busy` handling.
- Disable network and verify app reports connectivity/start failures.
