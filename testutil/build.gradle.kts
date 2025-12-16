plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.afollestad.mnmlscreenrecord.testutil"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
    }
}

dependencies {
    implementation("org.koin:koin-android:3.5.6")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
    implementation("junit:junit:4.13.2")
    implementation("androidx.arch.core:core-testing:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime:2.9.0")
}