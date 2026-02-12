# Shared Debug Keystore (Team)

This project is configured to sign `debug` builds with a shared keystore:

- File: `keystore/taskmanager-debug.keystore`
- Alias: `taskmanagerdebug`
- Store password: `android`
- Key password: `android`

Current debug SHA-1:

`C4:F4:AF:2E:1E:3D:ED:14:27:F1:4B:3D:AF:27:A4:C8:0A:64:16:04`

## Google Cloud OAuth setup

Create or update an **Android OAuth client** with:

- Package name: `com.example.taskmanagerapp`
- SHA-1: `C4:F4:AF:2E:1E:3D:ED:14:27:F1:4B:3D:AF:27:A4:C8:0A:64:16:04`

Keep using the same **Web client ID** in `local.properties` for `requestIdToken(...)`.

## Verify locally

Run:

```bash
./gradlew signingReport
```

`Variant: debug` must show `Config: sharedDebug` and the SHA-1 above.

## Notes

- This is for debug/development only.
- Do not use this keystore for production release signing.
