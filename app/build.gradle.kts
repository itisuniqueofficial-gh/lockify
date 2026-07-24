import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun signingValue(name: String): String? {
    return providers.gradleProperty(name).orNull ?: System.getenv(name)
}

android {
    namespace = "com.itisuniqueofficial.lockify"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.itisuniqueofficial.lockify"
        minSdk = 26
        targetSdk = 36
        // Version is injected by CI (release workflow / version.sh) via either a
        // Gradle property (-PVERSION_CODE / -PVERSION_NAME) or an environment
        // variable. Fallbacks are used for local development builds.
        versionCode = (signingValue("VERSION_CODE"))?.toIntOrNull() ?: 4
        versionName = signingValue("VERSION_NAME") ?: "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = signingValue("KEYSTORE_FILE")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
            }
            storePassword = signingValue("KEYSTORE_PASSWORD")
            keyAlias = signingValue("KEY_ALIAS")
            keyPassword = signingValue("KEY_PASSWORD")
        }
    }

    // A release keystore is only present in CI (from GitHub Secrets) or a
    // properly configured local machine. When it is absent we fall back to the
    // debug signing config so that `assembleRelease`/`bundleRelease` still
    // produce a (debug-signed) artifact for local verification instead of
    // failing the build. CI provides the real keystore, so production releases
    // are always release-signed.
    val hasReleaseKeystore = !signingValue("KEYSTORE_FILE").isNullOrBlank()

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin.compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Fail the build (and therefore CI) on any lint error.
        abortOnError = true
        checkReleaseBuilds = true
        // Emit machine- and human-readable reports for CI artifact upload.
        sarifReport = true
        xmlReport = true
        htmlReport = true
        textReport = true
        // `LocalContextGetResourceValueCall` (shipped by the Compose UI lint) is
        // a false positive for this project: every flagged call reads a static
        // string resource inside a side-effecting, non-composable scope
        // (Toast.makeText, BiometricPrompt.Builder, Intent.putExtra, onClick
        // lambdas) where the @Composable stringResource() cannot be invoked.
        disable += "LocalContextGetResourceValueCall"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":appintro"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // fixes "Can only use lower 16 bits for requestCode"
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.refine.runtime)
    compileOnly(project(":hidden-api"))
    implementation(libs.hiddenapibypass)
    implementation(project(":patternlock"))

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
}
