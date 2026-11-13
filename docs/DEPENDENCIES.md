# Dependency and asset provenance

The authoritative version pins are in the Gradle files and wrapper properties. Android Gradle Plugin 8.10.1 supports compile API 36; Gradle 8.13 and JDK 21 are used. Kotlin 2.1.20 and its Compose/serialization compiler plugins are aligned. Compose libraries use BOM 2025.05.01. Room/KSP versions are aligned as declared.

AndroidX (Compose, Material, Navigation, Lifecycle, Room, DataStore, WorkManager, Core and test tools), Kotlin/kotlinx, OkHttp and Gradle are generally distributed under Apache-2.0; JUnit is EPL-1.0. Review the resolved transitive dependency licenses before a public release. Run `./gradlew :app:dependencies` to produce the actual resolved inventory. The app does not bundle native game assets, fonts, soundtrack, trailers or screenshots from Rockstar. Videos/media open at official sources in the browser. Platform fonts are supplied by Android.

`Design.kt`, `atlas_mark.xml` and store vector designs are original project artwork. Their skyline is decorative, not a representation of GTA VI. Native material icons are from the AndroidX Material icons set. Facts are summarized in original bilingual copy and retain direct primary-source URLs and verification dates in `content/content.json`.
