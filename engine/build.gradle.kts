plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.afollestad.mnmlscreenrecord.engine"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":notifications"))
    implementation(project(":theming"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")

    // afollestad
    implementation("com.afollestad.material-dialogs:core:3.3.0")
    implementation("com.afollestad:assent:3.0.1")

    // Square
    implementation("com.squareup:seismic:1.0.2")
}