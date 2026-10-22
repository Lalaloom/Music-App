plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cameronbatten.dadmusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cameronbatten.dadmusic"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val keystorePath: String? = project.findProperty("MUSICAPP_KEYSTORE_PATH") as? String
        val keystorePwd: String? = project.findProperty("MUSICAPP_KEYSTORE_PASSWORD") as? String
        val keyAlias: String? = project.findProperty("MUSICAPP_KEY_ALIAS") as? String
        val keyPwd: String? = project.findProperty("MUSICAPP_KEY_PASSWORD") as? String

        if (keystorePath != null && keystorePwd != null && keyAlias != null && keyPwd != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePwd
                this.keyAlias = keyAlias
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            // Uses the default debug keystore automatically
        }
        release {
            // M1: R8 disabled — proguard-rules.pro covers only 3 rules and has not been
            // smoke-tested against Hilt/Room/Media3 reflection. Enable in M2 once the rule
            // set is validated against an installed release APK.
            isMinifyEnabled = false
            // Attach release signing config only if it was configured.
            signingConfigs.findByName("release")?.let { signingConfig = it }
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM — manages all Compose library versions
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DocumentFile — SAF tree traversal
    implementation(libs.androidx.documentfile)

    // Media3 (ExoPlayer + MediaSession + UI)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
