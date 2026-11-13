# Validation matrix and evidence

Status is evidence-based. An implementation is not equivalent to a fully verified commercial release. No physical device, Play pre-launch report or Play approval has been claimed.

## Recorded execution

On 2026-11-13: 21 JVM domain tests passed, 0 failures/errors. Coverage includes the real bundled content, malformed/rollback manifests, source URL checks, signed payload tampering, accented bilingual search, category/spoiler filters, private recommendations, date boundaries, invalid calculator values and Unicode backup round trips. Results are in the Gradle test report and GitHub Actions validation-reports artifact.

Native debug APK, optimized unsigned AAB, Android instrumentation APKs and debug lint built successfully locally. Bundletool validated the AAB; apksigner verified the APK, zipalign passed with 16 KB alignment, and all bundled 64-bit ELF LOAD segments have alignment 16384. The local host has no /dev/kvm: software emulation did not reach a usable test session, so no local UI execution is claimed. GitHub run 43500520549 passed all 3 Android instrumentation tests on API 36 (1 UI journey and 2 Room integration tests), and generated seven actual app screenshots. Both CI jobs succeeded. The UI journey confirms a saved favorite survives Activity recreation and a manually created objective is persisted. This is emulator evidence, not physical-device coverage. A repeated run (43501366157) exposed a test synchronization race after the asynchronous onboarding preference write. The journey now explicitly waits for navigation availability before tapping it, rather than assuming persistence has already completed. The CI device job runs real Android instrumentation, not a mocked UI. The screenshot script captures an installed app by observing Android accessibility bounds; it does not render substitute web screens.

| Priority | Requirement | Implementation / verification |
|---|---|---|
| P0 | Native Android build | Compose application, target/compile API 36, SDK build tools 36.0.0; see current CI run |
| P0 | Working navigation/home | Five bottom destinations, details/settings, optional onboarding; journey test |
| P0 | Verified content/news | 18 conservative bilingual dossiers; stable IDs, source links and checked dates; seed validation test |
| P0 | Searchable encyclopedia | Accent-normalized local search; all required categories, honest empty states; unit tests |
| P0 | Legitimate atlas | Non-geographic region board, zoom/reset, favorites/manual visits; no fabricated coordinates |
| P0 | Persistence | Room plus DataStore; favorite recreation journey and Room import/collection tests |
| P0 | Personal tools | Notes, objectives/dates, completion, collections/profiles, comparison, timeline, calculator |
| P0 | Errors/offline | Seeded offline text/tools, retry, preserved cache on sync failure, no-endpoint message |
| P0 | Spoiler safety | Default story protection, filter before search/recommendations, per-visit detail reveal; unit tests |
| P0 | Data safety | Transactional/size-limited JSON import; HTTPS, signature and version checks; no trackers |
| P0 | Accessibility essentials | Labeled controls, text scaling, semantic Compose controls, non-gesture atlas actions; TalkBack/large-font physical-device pass still required |
| P0 | APK/AAB | CI publishes testing APK and unsigned optimized AAB; upload signature remains owner-controlled |
| P0 | Release submission identity | Owner support contact, hosted privacy policy and signing/Play settings remain outstanding |
| P1 | Distinct themes/tablet | Separate day/night palettes, rail at 840dp; broader device/rotation/font-size visual checks outstanding |
| P1 | Performance | No frame-time, startup-time, memory or battery measurements claimed; benchmark on representative hardware before launch |
| P1 | Remote editorial publishing | Implemented signed schema/version/cache engine; needs owner trust key and HTTPS host before activation |
| P1 | Push alerts | Not implemented; no provider, notification permission or fabricated notification service |
| P1 | Advanced polish | Static original vector art, brief transitions and contextual favorite haptic; no claim of 60 FPS certification |
| P2 | Geographic maps/POIs | Future authorized data only; current region index remains usable |
| P2 | Authorized integrations/community | Not implemented and not implied by the UI |

## Required release-device checks

Test API 26 and API 36+, an intermediate physical Android phone, tablet, rotation, system font/display scaling, TalkBack focus order and target contrast, light/dark/system settings, empty/full archives, multiple profiles, canceled file pickers, malformed/large backups, airplane mode, slow networks, request failures, process death and upgrades over a signed prior build. Verify external-source handling when no browser is installed. No runtime permission prompt is expected in this version. Validate shrinking and 16 KB native-library alignment on the actual signed release using Play’s pre-launch report/bundle tooling. Record failures and retest the corresponding paths after fixes.
