plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.afollestad.mnmlscreenrecord.notifications"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
    }
}

dependencies {
    implementation(project(":common"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}