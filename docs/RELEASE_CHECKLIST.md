# Release Checklist

Use this checklist before shipping a build to users or Play Console.

## Build and Signing
- [ ] Create a release keystore and save it outside source control.
- [ ] Copy `app/keystore.properties.example` to `app/keystore.properties`.
- [ ] Update `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` in `app/keystore.properties`.
- [ ] Confirm the release build is signed and installable.
- [ ] Build both `assembleRelease` and `bundleRelease`.

## App Behavior
- [ ] Verify launch, scanning, OCR editing, and saved document opening on a physical device.
- [ ] Verify the subscription page opens from the home screen and the restore flow still works.
- [ ] Confirm PDF export is not exposed anywhere in the UI or file flow.
- [ ] Confirm the app opens images and text files correctly after the latest file-type cleanup.

## Store and Compliance
- [ ] Replace all placeholder billing data with production Play Billing product IDs.
- [ ] Publish privacy policy and terms links.
- [ ] Fill out Play Console Data safety details for camera, OCR, billing, and ads.
- [ ] Review ad disclosures and premium upsell copy.
- [ ] Prepare screenshots, feature graphics, and short/long descriptions.

## Quality Gates
- [ ] Run the connected harness tests.
- [ ] Run a release smoke test on a physical device.
- [ ] Check lint and release build warnings.
- [ ] Verify version code and version name are bumped.