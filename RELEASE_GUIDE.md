# Android Release and Play Console Guide

## App and Firebase identity

- Android application ID / Play Console package: `com.waves.androidapp`
- Current release version: `1.0.2` (`versionCode` 5; the next Play release must use a code greater than 5 and greater than any code already used on any track)
- Firebase Android app: `1:1088277592046:android:1fccb895b44a59bd6f80ff`
- Firebase configuration: `app/google-services.json`
- Android Gradle namespace remains `com.example`; it is separate from the installed application ID and keeps existing source packages stable.
- Keep the Firebase API key restricted in Google Cloud Console to the APIs and Android app that need it. The Firebase config is client-side configuration, not a place for private service-account credentials.

## Build toolchain

- Gradle: 8.11.1
- Android Gradle Plugin: 8.10.1
- Kotlin: 2.2.20
- KSP: 2.2.20-2.0.4
- Kotlin Compose plugin: 2.2.20
- Compose BOM: 2024.09.00
- Compile SDK / target SDK: 36
- Minimum SDK: 24
- JDK: 21

Gradle must run on JDK 21 for the current Kotlin Gradle DSL/toolchain. App source and Kotlin bytecode target Java 17; this is distinct from the JDK used to run Gradle. Robolectric tests configured for SDK 36 also require JDK 21.

## Signing keys

The release signing configuration in `app/build.gradle.kts` reads:

- Keystore: `credentials/my-upload-key.jks` when `KEYSTORE_PATH` is set for the documented build; otherwise `my-upload-key.jks` in the repository root
- Key alias: `upload`
- Environment variables sourced from `credentials/signing.env`: `STORE_PASSWORD` and `KEY_PASSWORD`
- Local-only credential files: `credentials/` (ignored by Git)
- Public upload certificate: [`upload_certificate.pem`](upload_certificate.pem)

The ignored local credentials directory contains `credentials/my-upload-key.jks`, `credentials/debug.keystore`, `credentials/upload_certificate.pem`, `credentials/google-services.json`, `credentials/sha-fingerprints.txt`, and `credentials/signing.env`. The active Firebase configuration remains `app/google-services.json`. The signing environment file contains passwords; never print it or paste its contents into a terminal transcript, issue, chat, or source file.

The private upload keystore, debug keystore, and local credential copies are excluded by `.gitignore`. Never commit, email, or publish the private keystore or its passwords. Keep encrypted backups of the upload key and store its passwords in a password manager. On a new build machine, provision `credentials/my-upload-key.jks` and `credentials/signing.env` from the team's approved encrypted secret storage; do not generate a replacement key for an existing Play app. Restrict local access, for example with `chmod 600 credentials/signing.env credentials/my-upload-key.jks`.

SHA-1 fingerprints:

- Release/upload certificate: `DB:B4:7E:C1:6D:A6:6C:59:62:CA:99:A1:EB:AF:1F:CF:30:28:40:BE`
- Release/upload certificate SHA-256: `7F:36:71:12:DF:46:F3:2F:34:F8:7E:91:27:36:DE:A1:5B:EC:8F:47:49:85:7A:CC:23:6A:AF:47:C7:8F:2F:FF`
- Local debug certificate: `94:0E:9C:D7:3A:44:95:76:28:62:FF:4D:F6:B6:AE:E8:55:81:D0:B4`
- Local debug certificate SHA-256: `A0:D5:70:CC:B0:AA:F0:37:0D:7C:98:68:20:4A:E7:74:F0:0D:DB:06:E6:AE:45:73:A6:D6:FC:94:2B:B8:E7:6D`

Register the upload certificate fingerprint where required by Firebase/Google services. Google Play App Signing uses a separate app-signing key after enrollment; use the fingerprints shown in Play Console for production app-signing identity and API integrations.

## Build signed release APK and AAB

The current release configuration is `versionName = "1.0.2"` and `versionCode = 5` in `app/build.gradle.kts`. Increment the code before each Play upload, and first confirm it is greater than every code already used in Play Console. The repository cannot query Play Console's track history.

The following is the command used to produce the current release in this workspace. It explicitly selects JDK 21, uses the ignored upload-key files, and limits Gradle to one worker with a 1 GiB heap because the default parallel build caused its daemon to be terminated in this Codespace. Adjust `JAVA_HOME` if JDK 21 is installed elsewhere. Never add password values to the command itself:

```bash
set -a
. credentials/signing.env
set +a
export KEYSTORE_PATH="$PWD/credentials/my-upload-key.jks"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew :app:assembleRelease :app:bundleRelease \
  --no-daemon \
  --max-workers=1 \
  --no-configuration-cache \
  -Dorg.gradle.jvmargs='-Xmx1024m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8'

unset STORE_PASSWORD KEY_PASSWORD
```

The build runs release Kotlin compilation, release lint (`lintVitalRelease`), APK packaging/signing, and AAB packaging/signing. A successful build ends with `BUILD SUCCESSFUL`. Routine warnings about deprecated APIs or native libraries that cannot be stripped did not prevent this release from building.

Expected outputs:

- APK: `app/build/outputs/apk/release/app-release.apk`
- Android App Bundle: `app/build/outputs/bundle/release/app-release.aab`

Artifacts under `app/build/outputs/` are local build outputs and are not committed. For the current version 1.0.2 (versionCode 5) build:

- Package: `com.waves.androidapp`
- Version code/name: `3` / `1.2`
- Minimum / target SDK: `24` / `36`
- APK SHA-256: `f2e8e3cc9dbc1c757c66d2adc3aa053c9028c4160628cceb4f5d58489c6de386`
- AAB SHA-256: `8e0bbf0d37323b2d51401b1911f3fb8e61ad7941bdb4e1eaba6bbb8963063024`

### Verify the release files

Run verification after the build and before uploading. Android SDK Build Tools 36.0.0 provides `apksigner`:

```bash
APK=app/build/outputs/apk/release/app-release.apk
AAB=app/build/outputs/bundle/release/app-release.aab

aapt dump badging "$APK" | grep -E '^package:|^targetSdkVersion:'
apksigner verify --print-certs "$APK"
jarsigner -verify "$AAB"
sha256sum "$APK" "$AAB"
```

Also validate the AAB structure using Google's standalone bundletool (the current release was validated with bundletool 1.18.0):

```bash
java -jar bundletool-all-1.18.0.jar validate --bundle="$AAB"
```

The APK and AAB should show package `com.waves.androidapp`, version code `3`, version name `1.2`, target API 36, and the release upload certificate fingerprints listed above. APK signing is checked with `apksigner`; AAB signing is checked with `jarsigner`; bundletool validates bundle structure. Keep the output files private until ready to distribute.

## Firebase Backend

The Firebase CLI project is `waves-64217` in `.firebaserc`. Email/Password authentication is enabled by the `auth.providers.emailPassword` setting in `firebase.json`. Verification and password-reset emails use Firebase's built-in email delivery. Account-deletion OTP messages use the deployed Firebase Functions and Resend; the Resend API key and sender address are Firebase Secret Manager secrets, not local signing credentials.

Business data is scoped to the authenticated user's UID:

- `users/{uid}/metadata/account`: signup email, display name, terms acceptance, and timestamps
- `users/{uid}/clients/{clientId}`: client contact, billing address, tax, notes, and archive state
- `users/{uid}/products/{productId}`: SKU, description, price, unit, quantity, tax, and archive state
- `users/{uid}/invoices/{invoiceId}`: client snapshot, dates, line items, totals, status, and payment history
- `users/{uid}/settings/business`: business profile, logo URL, tax, bank details, and invoice defaults
- Storage `users/{uid}/business/logo`: owner-only image, limited to 5 MB

New accounts start with empty business data; no sample clients, bank details, or invoices are seeded. Firestore and Storage rules require a verified account and restrict records/files to their owner. The signup metadata rule permits only the exact terms-consent document before email verification.

Firestore and Storage are provisioned for project `waves-64217`:

- Firestore: `(default)` database, Standard edition, `nam5` (US multi-region), with delete protection enabled.
- Storage: default bucket `waves-64217.firebasestorage.app`, Standard storage class, `US` multi-region.
- Authentication: Email/Password provider enabled.
- Security rules: `firestore.rules` and `storage.rules` have been deployed.

The project is linked to a billing account. Storage for Firebase requires the Blaze pay-as-you-go plan under Firebase's current pricing requirements; review the billing plan and usage limits before production use. To deploy backend config and rules later, run `firebase deploy --only auth,firestore:rules,storage --project waves-64217`.

## Google Play submission checks

As of August 31, 2026, new apps and updates must target Android 16 (API 36) or higher. This release is configured for target API 36. Before uploading, check Play Console and ensure the version code exceeds every code already used in all tracks.

This app creates user accounts. Settings links to the supplied Privacy Policy URL, and the in-app account deletion flow sends a six-digit email code and removes the user's Firebase Auth account, Firestore data, and Storage files. Google Play also requires an external web resource for account/data deletion; add its public URL in Play Console and complete the Data safety form before submission. See [Google Play account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111).

## Verified status

The release APK and AAB were built and signed with the local upload key. The APK signature was verified with `apksigner`, and the AAB signature was verified with `jarsigner` and Google's `bundletool`; both use the upload certificate fingerprints listed above. The generated artifacts are local build outputs and are not committed.
