import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    // Firebase pluginy odkomentuj, jakmile přidáš google-services.json
    // id("com.google.gms.google-services")
    // id("com.google.firebase.crashlytics")
}

// Load signing config from keystore.properties (git-ignored). This file lives
// in the repo root and contains storeFile/storePassword/keyAlias/keyPassword.
// Without it, release APKs would be unsigned and Android would refuse to
// install them ("App not installed" / "package corrupt").
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.zeddihub.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zeddihub.mobile"
        minSdk = 26
        targetSdk = 34
        versionCode = 15
        versionName = "0.6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "API_BASE_URL", "\"https://zeddihub.eu/api/\"")
        buildConfigField("String", "SITE_BASE_URL", "\"https://zeddihub.eu/\"")
        buildConfigField("String", "WS_BASE_URL", "\"wss://zeddihub.eu/ws/\"")
        buildConfigField("String", "WEB_URL", "\"https://zeddihub.eu/tools\"")
        buildConfigField("String", "DISCORD_URL", "\"https://dsc.gg/zeddihub\"")
        // Shared secret sent as X-App-Secret so native apps bypass hCaptcha.
        // Must match ZH_APP_SECRET in website/api/_config.php.
        buildConfigField(
            "String",
            "APP_SECRET",
            "\"696d63c65a8536637183028e4eecb841cd5b679ce7b2d33c6ef2d4054166e438\""
        )
        buildConfigField("String", "CLIENT_KIND", "\"mobile\"")

        resourceConfigurations += listOf("cs", "en")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps.getProperty("storeFile")
            if (storeFilePath != null) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
            // Use the legacy JAR signing scheme AND modern APK signature schemes
            // so installs work across Android 7+ (v1 + v2 + v3).
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
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
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "ZeddiHub-App-${variant.versionName}.apk"
            }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core / lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-compiler:2.51")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")

    // Storage / security
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")

    // Room (telemetry queue)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager + Hilt Work
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Firebase – odkomentuj po přidání google-services.json
    // implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    // implementation("com.google.firebase:firebase-messaging-ktx")
    // implementation("com.google.firebase:firebase-crashlytics-ktx")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ML Kit Document Scanner (PDF scanner)
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

    // ZXing for WiFi QR code generation
    implementation("com.google.zxing:core:3.5.3")

    // Google Code Scanner — unbundled, no camera permission required.
    // Used by WiFi Tools to scan WIFI: QR codes and extract SSID + password.
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // osmdroid for WiFi Map (OpenStreetMap)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Fused Location Provider for WiFi Map
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
