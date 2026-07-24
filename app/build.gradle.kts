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
            // Enable every APK signature scheme. v2/v3 are the functional
            // requirement for minSdk 26+, but also enabling v1 (JAR) maximises
            // install compatibility across OEM/sideload paths. Broad, valid
            // signing prevents "App not installed" from signature-scheme gaps.
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    // Release-signing policy (prevents the most common "App not installed"
    // cause — inconsistent or missing signatures):
    //   * A real release keystore (CI GitHub Secrets or a configured machine)
    //     is used whenever present, so every published build shares ONE stable
    //     signing certificate and installs cleanly as an update.
    //   * A release must NEVER be published debug-signed or unsigned. Debug
    //     signing for a release is only allowed as an explicit local
    //     convenience via -PallowDebugSigningForRelease=true.
    //   * Otherwise the release build FAILS loudly at validateSigningRelease
    //     rather than emitting an unsigned/mis-signed artifact.
    val hasReleaseKeystore = !signingValue("KEYSTORE_FILE").isNullOrBlank()
    val allowDebugSigningForRelease =
        signingValue("allowDebugSigningForRelease")?.toBoolean() == true

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = when {
                hasReleaseKeystore -> signingConfigs.getByName("release")
                allowDebugSigningForRelease -> signingConfigs.getByName("debug")
                // No keystore and not explicitly allowed: keep the release
                // config (with no storeFile) so validateSigningRelease fails
                // with a clear error instead of producing an unsigned APK.
                else -> signingConfigs.getByName("release")
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
