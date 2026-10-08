# WAVES — Deployment Records

## Backend — Firebase Project `waves-64217`

### Deployment #1 — 2026 (current session)

| Target | Status | Notes |
|---|---|---|
| `firestore.rules` | ✅ Released | Security rules live on `(default)` database |
| `firestore.indexes.json` | ✅ Deployed | Composite indexes live |
| `storage.rules` | ✅ Released | Storage security rules live |
| `functions/sendAccountDeletionOtp` (us-central1) | ✅ Live | Verified "No changes detected" (already up to date) |
| `functions/completeAccountDeletion` (us-central1) | ✅ Live | Verified "No changes detected" (already up to date) |

**Deployed with:** Firebase CLI v15.33.0 (`firebase deploy --project waves-64217`)

**Console:** https://console.firebase.google.com/project/waves-64217/overview

**Function region:** `us-central1` — app must call functions in this region (already configured in app code).

### Firebase project verification

- Android app registered: `1:1088277592046:android:1fccb895b44a59bd6f80ff` (`com.waves.androidapp`)
- `credentials/google-services.json` matches the server-side config exactly (verified via `firebase apps:sdkconfig`)
- Auth provider: **Email/Password only** — no SHA-1 fingerprint required for Firebase Auth

---

## App Releases — Play Console

| Version | versionCode | Artifacts | Signed with |
|---|---|---|---|
| 1.0.2 | 5 | `waves-v1.0.2-5-release.aab` / `.apk` | `credentials/my-upload-key.jks` (alias `upload`) |

Artifacts attached to GitHub Release **`v1.0.2`** on this repository.

See `PLAY_CONSOLE_CHECKLIST.md` and `RELEASE_GUIDE.md` for the full release process.
