# Data Safety preparation — verify against the final binary and hosted services

| Field | Current default build |
|---|---|
| Account required | No |
| Ads / analytics | None |
| Personal data transmitted by app | No personal plans, names, history or favorites transmitted |
| Data handled locally | Profile names, notes, objectives, collections, history, preferences, favorites |
| Remote content | Disabled until the publisher configures a signed HTTPS feed |
| Feed host metadata | Ordinary connection metadata including IP/time; review actual host logging before declaring collection |
| Sharing | User-initiated Android share chooser for official source links; file exports go to a user-chosen provider |
| Transit security | HTTPS; cleartext disabled |
| Deletion | Delete items/profiles, clear Android app storage, uninstall; user separately deletes exports |
| Sensitive permissions | No dangerous runtime permissions; INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK and RECEIVE_BOOT_COMPLETED, plus an internal signature permission |
| Data access by developer | No in-app collection endpoint or telemetry in the default build |

Do not blindly copy “no data collected” into Play if you enable server logging, crash reports, notifications, community features or monetization later. Assess Google’s definitions, exemptions for on-device processing and user-initiated transfers, all SDKs and actual server configuration. Record the account owner’s declarations and legal controller contact. No Play form has been submitted.
