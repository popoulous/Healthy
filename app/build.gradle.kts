plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "hu.galambos.healthy"
    // Compose 1.12+ requires compileSdk 37; the phone runs Android 16, so the
    // app targets 36 until 37 behaviour can actually be tested on a device.
    compileSdk = 37

    defaultConfig {
        applicationId = "hu.galambos.healthy"
        // Android 9 is the floor for the Health Connect app itself. The old
        // Galaxy A71 tops out at Android 13, and the app has to run there too,
        // so the pre-Android-14 paths stay in: Health Connect as an installable
        // app, and no history permission.
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        warningsAsErrors = true
        // targetSdk deliberately trails compileSdk: the phone runs Android 16,
        // so targeting 37 would ship behaviour changes nobody can test here.
        disable += "OldTargetApi"
        // "A newer AGP exists" is news, not a defect. Upgrading is a decision,
        // and it should not be forced by a build that fails the day a release
        // lands.
        disable += "AndroidGradlePluginVersion"
        // Same reasoning for library versions: a dependency bump is a decision
        // to make and test, not something a passing build should demand the
        // day upstream publishes.
        disable += "GradleDependency"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.health.connect.client)

    testImplementation(libs.junit)
}
