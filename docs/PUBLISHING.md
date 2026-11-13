# Google Play submission guide

Target SDK: 36. Minimum: 26. Package: `pt.leonida.atlas` (owner must confirm availability and desired long-term ownership). Product name: Leonida Atlas. Version: 1.0.0. No Play Console access or publication is performed by this repository.

## Signing

Create a private upload key on a trusted machine and keep offline backups. Do not commit it. Configure repository Actions secrets `ATLAS_KEYSTORE_BASE64`, `ATLAS_STORE_PASSWORD`, `ATLAS_KEY_ALIAS`, `ATLAS_KEY_PASSWORD`. Run **Signed Play release** from main to produce signed APK/AAB artifacts. Its upload key is deleted from the runner in an always step. Use separate secrets/environments and review access before adding collaborators.

For local signing, set `ATLAS_KEYSTORE` to the absolute keystore path, plus the three password/alias variables, and run `./gradlew :app:bundleRelease :app:assembleRelease`. An absent keystore intentionally produces unsigned release artifacts. The debug APK has a development signature and must not be uploaded to production.

Enable Play App Signing while creating the app in Play Console; have Google generate/manage the app signing key and use the private upload key for bundle uploads. Follow the console’s actual prompts and export its key certificate when needed. Keep upload and app signing roles separate. AAB files cannot be installed directly on a phone. Use the APK for direct testing or Play’s internal testing distribution. Inspect the final signed artifact’s manifest and certificate before upload.

`VERSION_CODE` defaults to 1 locally; CI uses the run number. The signed-release workflow asks for an explicit version_code input. Before every Play upload choose a number greater than the last accepted Play version. Never reuse a Play version code. Development signing cache is best-effort and may expire; if a test APK uses a different development key, export data before uninstalling the previous test app. Production continuity relies on your upload key and Play App Signing.

## Store setup still required

- Confirm the owner’s legal developer name, support email and contact address; no invented identity is supplied.
- Host `store/privacy-policy.html` at a public, stable HTTPS URL after completing its controller/contact fields. The packaged draft is not a hosted policy.
- Review English/Portuguese listing copy, adaptive icon, 512×512 store icon, 1024×500 feature graphic and actual device screenshots in `store/`.
- Complete Data Safety using `store/data-safety.md`, reflecting any configured feed host or added SDKs. Check the final merged manifest.
- Answer IARC’s questionnaire truthfully based on the app’s actual content and external links; the game’s rating is not automatically this app’s rating. This companion links to mature game material and is not designed for children. Do not predeclare a rating before IARC assigns it.
- Complete app-access declarations (all functions available without login), ads (none), target audience, content rights/impersonation review and permission declarations. No notification, location or broad storage permission is requested. WorkManager requires WAKE_LOCK and RECEIVE_BOOT_COMPLETED for periodic background sync; the manifest removes its unused foreground service.
- Test signed release on physical devices, accessibility tools, API 26 and 36+, tablet/large fonts, offline mode, SAF denial/cancellation and upgrade persistence. See QA matrix.
- Upload a signed AAB to internal testing, resolve Play’s pre-launch report and any policy issues, then run applicable closed testing and apply for production access.

## Closed tests

Google’s documentation consulted on 2026-11-13 says new personal developer accounts created after 2023-11-13 require at least 12 opted-in testers continuously for 14 days before applying for production access. Applicability depends on the account and the current console requirements. Recruit real testers, share the opt-in URL, collect feedback and document actual test usage. An automated workflow does not complete this requirement.

The screenshot workflow renders the actual app at 1080×1920 and exports lossless RGB PNGs without alpha. It validates the 320–3840 dimension bounds and maximum 2:1 aspect ratio. It changes emulator display resolution before capture, rather than cropping or inventing UI. Pillow is used only to remove the opaque alpha channel without changing RGB pixels. The original icon is 512×512; the feature graphic is 1024×500 RGB.

## Verified policy references

https://support.google.com/googleplay/android-developer/answer/11926878 (target API; checked 2026-11-13: API 36 for new phone app submissions)

https://support.google.com/googleplay/android-developer/answer/14151465 (new personal account testing)

https://developer.android.com/studio/publish/app-signing (Play App Signing and upload keys)

https://support.google.com/googleplay/android-developer/answer/10787469 (Data Safety)

https://support.google.com/googleplay/android-developer/answer/9866151 (preview assets, dimensions and formats)

## Maintenance

Keep source provenance and checked dates, bump content version, preserve stable IDs and verify the JSON with core tests before signing. Never reuse a content version for different bytes. Update Gradle/dependencies deliberately with tests and a release build, preserve Room migrations, audit permissions, and rerun Play checks before release. Back up the upload key and editorial key separately. No automated submission or production deployment is enabled.
