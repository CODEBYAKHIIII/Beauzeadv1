# Play Console Release Checklist

## Before uploading AAB
- [ ] Privacy policy URL is live: https://waves.metricfluxsolutions.com/privacypolicy
- [ ] Account deletion URL is live: https://waves.metricfluxsolutions.com/datadeletion
- [ ] Complete the Data safety form using actual collection, sharing, and storage behavior. NOTE: AdMob IS integrated since v1.0.4 - declare the Google Mobile Ads SDK data collection (device identifiers / advertising ID shared with ad partners) and the UMP consent flow.
- [ ] Content rating questionnaire completed
- [ ] Confirm target audience; select 18+ only if that matches the app's intended audience
- [ ] Ads declaration: app CONTAINS ads since v1.0.4 (AdMob banners + opt-in rewarded). App content > Ads must be set to YES.
- [ ] App access: provide Play reviewers with test credentials for login-restricted features
- [ ] Version code exceeds all previous version codes (current: 8 = v1.0.5)

## Signing
- [ ] `credentials/my-upload-key.jks` is present and git-ignored
- [ ] SHA-1 fingerprint matches the Play Console upload key
- [ ] SHA-1 added to Firebase Console

## Build
- [ ] JDK 21 active
- [ ] Build Tools 36.0.0 installed
- [ ] `./gradlew bundleRelease` succeeds
- [ ] AAB is signed with the registered release upload key