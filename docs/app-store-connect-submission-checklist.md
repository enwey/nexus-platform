# App Store Connect Submission Checklist

This checklist is for the current `nexus-platform` iOS host app after the hosted mini-app hardening work.

## 1. Build And Release Inputs

- Confirm the `Release` target uses the real production HTTPS API domain instead of a placeholder.
- Confirm `BACKEND_CERT_SHA256` in the `Release` build settings matches the current production certificate chain leaf pin.
- Rebuild the iOS app in `Release` before upload.
- Keep `Debug` local-network settings only for development and verify the archive is generated from `Release`.

## 2. Reviewer Access

- Prepare a reviewer test account that can sign in successfully.
- Ensure at least one review-safe mini game is already uploaded, approved, and runnable.
- Make sure the reviewer account can reach the discovery page, open a game detail page, and launch a hosted mini game without extra internal whitelisting.
- Prepare a fallback demo game in case a primary game package is unavailable during review.

## 3. Review Notes

- Explain that the app is a hosted mini-app / mini-game container aligned to App Review Guideline `4.7`.
- State that uploaded mini games are platform-reviewed before distribution.
- State that hosted game packages are limited to platform-approved web content and run inside the app's controlled web runtime.
- State that the runtime bridge is restricted to platform-defined APIs and backend-approved network domains.
- Provide reviewer login credentials, test steps, and the exact path to launch a sample game.
- Attach or paste the summary from [app-store-mini-app-review-notes.md](/Users/apple/Documents/WordSpace/nexus-platform/docs/app-store-mini-app-review-notes.md).
- Fill and paste the ready-to-submit template from [app-store-review-notes-template.md](/Users/apple/Documents/WordSpace/nexus-platform/docs/app-store-review-notes-template.md).

## 4. Privacy And Compliance

- Verify the app bundle contains `PrivacyInfo.xcprivacy`.
- Verify App Store Connect privacy nutrition answers match the current product behavior.
- Confirm no tracking SDKs or undeclared data-sharing SDKs are present in the final archive.
- Confirm any customer support, billing, or login-related data disclosures in App Store Connect are reviewed by product/legal.

## 5. Content And Operations

- Ensure ops can see manifest inspection results for the latest game version before approving release.
- Reject any uploaded package whose manifest validation fails.
- Verify the production backend keeps manifest normalization enabled.
- Verify at least one approved game package has valid icon, title, version, and entry metadata.

## 6. Final Smoke Test

- Fresh install -> open app -> sign in -> browse discovery -> open detail -> launch approved game.
- Relaunch app -> reopen the same approved game.
- Verify release environment requests go to the production HTTPS domain.
- Verify certificate pinning is active in the release build.
- Verify a non-approved or malformed hosted package cannot be launched.
