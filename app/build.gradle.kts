plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace  = "com.kathakar.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kathakar.app"
        minSdk        = 24
        targetSdk     = 34
        versionCode   = 1
        versionName   = "1.0.0"
        // Required for Apache POI on older Android versions
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // ── Packaging rules ────────────────────────────────────────────────────────
    // Original rule kept + Apache POI META-INF conflicts resolved
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)

    // ── AndroidX core ──────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.multidex:multidex:2.0.1")

    // ── Compose UI ─────────────────────────────────────────────────────────────
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // ── Hilt DI ────────────────────────────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── Firebase ───────────────────────────────────────────────────────────────
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // ── Image loading ──────────────────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ── Apache POI — reads .docx (Word) files ──────────────────────────────────
    // Core POI for OOXML (.docx) support
    implementation("org.apache.poi:poi:5.2.5") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("org.apache.poi:poi-ooxml:5.2.5") {
        exclude(group = "org.apache.xmlbeans",  module = "xmlbeans")
        exclude(group = "com.github.virtuald",   module = "curvesapi")
        exclude(group = "org.apache.commons",    module = "commons-compress")
        exclude(group = "commons-logging",       module = "commons-logging")
    }
    // POI dependencies — explicitly pinned to avoid version conflicts
    implementation("org.apache.xmlbeans:xmlbeans:5.2.0")
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("commons-io:commons-io:2.16.1")
    implementation("org.apache.commons:commons-collections4:4.4")

    // ── Debug only ─────────────────────────────────────────────────────────────
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ── Tests ──────────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
}
