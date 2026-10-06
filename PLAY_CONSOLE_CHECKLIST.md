# Play Console Release Checklist

## Before uploading AAB
- [ ] Privacy policy URL is live: https://waves.metricfluxsolutions.com/privacypolicy
- [ ] Account deletion URL is live: https://waves.metricfluxsolutions.com/datadeletion
- [ ] Complete the Data safety form using actual collection, sharing, and storage behavior; verify AdMob separately because no AdMob dependency is present in this project.
- [ ] Content rating questionnaire completed
- [ ] Confirm target audience; select 18+ only if that matches the app's intended audience
- [ ] Ads declaration matches the shipped app; declare AdMob only if it is integrated
- [ ] App access: provide Play reviewers with test credentials for login-restricted features
- [ ] Version code 4 exceeds all previous version codes

## Signing
- [ ] `credentials/my-upload-key.jks` is present and git-ignored
- [ ] SHA-1 fingerprint matches the Play Console upload key
- [ ] SHA-1 added to Firebase Console

## Build
- [ ] JDK 21 active
- [ ] Build Tools 36.0.0 installed
- [ ] `./gradlew bundleRelease` succeeds
- [ ] AAB is signed with the registered release upload key