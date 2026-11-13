# Architecture

`app` owns Jetpack Compose/Material 3 presentation, Navigation and a lifecycle ViewModel. `core` is a JVM-only domain layer containing bilingual content models, backup contracts, spoiler-aware search/recommendations, countdown/calculator rules and RSA signature verification. `data` owns Room, DataStore, HTTPS retrieval and WorkManager. Dependencies point inward toward core. The application-scoped repository is constructed once in `AtlasApp`; explicit constructor injection keeps this small initial graph readable without a DI code-generation framework.

## Data flow

Room flows and DataStore preferences combine into immutable UI state. Writes run in viewModelScope; Room dispatches queries off the main thread. Seed parsing/network/file access run on IO. UI collects with lifecycle awareness. Navigation owns back stack; search/filter/editor values use saveable state. SQLite transactions protect backup merges and remote content replacement. No destructive database migration fallback is enabled. Version 1 Room schema is exported; any future database version requires a tested Migration before release.

The SQLite content row stores a versioned normalized JSON contract. Search constructs a local accent-normalized in-memory index from up to 10,000 entries; the current edition has 18 entries. Searching large future catalogs needs profiling before this upper bound is increased. Personal rows have indexes on profile/kind/target. Content updates replace editorial content without modifying personal data, so saves remain compatible by stable entry ID. Missing IDs remain in backups for a future edition rather than being silently deleted.

## Remote publishing

Content is not scraped by the app. An owner-operated HTTPS endpoint returns a JSON envelope containing base64 payload/signature. A bundled RSA public key verifies SHA256withRSA before schema validation and a Room transaction. Envelopes above 2 MB, malformed dates, duplicate IDs, unsupported schemas, unsafe source links and lower versions are rejected. HTTPS requests do not follow redirects. An update failure preserves cached data. WorkManager uses connectivity/battery constraints and 12-hour periodic work, with bounded retries for transport errors. No tracker, account or credentials are needed. The owner must install a public key and feed before enabling live publishing. Only the public key may enter Git.

## Scope decisions

The atlas board deliberately does not claim a geographic layout. Its card positions are interface layout, not game coordinates. Pinch/zoom and accessible zoom buttons explore the board; verified regional dossiers are accessible without gestures. No original game map, interior, collectible or road data is fabricated.

Recommendations score categories using this device’s favorites/history. No data leaves the device for recommendations. Spoiler filtering happens before search/recommendation results and detail pages require an explicit reveal. The calculator uses user-entered values only. There is no Rockstar login or simulated game sync.

The native Canvas horizon/palms/mark are original vector artwork. Platform typefaces avoid font redistribution. Static art and short navigation transitions avoid continuous idle animation; Compose observes Android’s animator duration scale. Themes have separate palettes; the content is flexible-height and text scales with system settings. The tablet layout uses a navigation rail at 840 dp. Accessibility and actual device performance remain release checks, not claims established by code structure.
