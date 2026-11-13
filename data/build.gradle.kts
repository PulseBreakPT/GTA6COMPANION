plugins { id("com.android.library"); kotlin("android"); kotlin("plugin.serialization"); id("com.google.devtools.ksp") }
android {
 namespace="pt.leonida.atlas.data"; compileSdk=36; buildToolsVersion="36.0.0"
 defaultConfig { minSdk=26; testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner" }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 api(project(":core"))
 implementation("androidx.room:room-runtime:2.7.1")
 implementation("androidx.room:room-ktx:2.7.1")
 ksp("androidx.room:room-compiler:2.7.1")
 implementation("androidx.datastore:datastore-preferences:1.1.7")
 implementation("androidx.work:work-runtime-ktx:2.10.1")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.room:room-testing:2.7.1")
}
