import com.android.build.api.variant.impl.VariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// --- Semantic version (single source of truth) ------------------------------
// MAJOR.MINOR.PATCH, baselined at 1.0.0 with the initial release (PR #1) and
// advanced per merged PR using the policy in VERSIONING.md:
//   feat  -> MINOR     fix -> PATCH     breaking (feat!/BREAKING CHANGE) -> MAJOR
//   dependabot: build-tooling/patch/security bump -> PATCH; a bump that changes
//   shipped-app capability -> MINOR; a breaking shipped-runtime bump -> MAJOR.
// History 1.0.0 -> 1.1.0(#2) -> 1.1.1(#3) -> 1.1.2(#4) -> 1.2.0(#9) ->
//   1.3.0(#10) -> 1.3.1(#11) -> 1.4.0(#12) -> 1.5.0(#13) -> 1.5.1(#14) ->
//   1.5.2(#15) -> 1.5.3(#16). Bump this line on every release.
val appVersionName = "1.5.3"

// Monotonic integer derived from the semantic version: MAJOR*10000 +
// MINOR*100 + PATCH. Allows up to 99 minors / 99 patches per band and always
// increases as the semver does, so Android's install/upgrade check is happy.
val appVersionCode = appVersionName.split(".").let { (maj, min, patch) ->
    maj.toInt() * 10000 + min.toInt() * 100 + patch.toInt()
}

plugins {
    // AGP 9 built-in Kotlin (android.builtInKotlin=true): no standalone
    // kotlin.android plugin. Compose compiler + KSP (Room) still applied.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ch.gryphus.creditdebittracker"
    // core 1.19.x / compose 1.12.x / nav 2.10.x require compiling against API 37.
    compileSdk = 37

    defaultConfig {
        applicationId = "ch.gryphus.creditdebittracker"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Produce JaCoCo coverage from JVM unit tests (consumed by SonarCloud).
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

// --- APK naming -------------------------------------------------------------
// Rename the default "app-<buildType>.apk" to "<project>-<version>-<buildType>.apk"
// (e.g. CreditDebitTracker-1.5.3-debug.apk / -release.apk) so every artifact
// self-identifies by app + semantic version. Uses the AGP variant API.
androidComponents {
    onVariants { variant ->
        val baseName = rootProject.name // "CreditDebitTracker"
        variant.outputs.forEach { output ->
            (output as? VariantOutputImpl)?.outputFileName?.set(
                "$baseName-$appVersionName-${variant.buildType}.apk",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }

    // Provision a Java 21 toolchain for ALL JVM work (javac, Kotlin, Lint),
    // independent of whichever JDK runs the Gradle daemon. Locally the daemon
    // is temurin-21 (mise), but GitHub's default-setup CodeQL autobuild drives
    // Gradle on its own bundled JDK 17 - a JDK 17 javac cannot emit Java 21
    // bytecode ("invalid source release: 21"), and Lint refuses to run below
    // the target version. The toolchain makes Gradle fetch/use a JDK 21 for
    // those tasks regardless of the daemon. Auto-provisioning is served by the
    // foojay resolver configured in settings.gradle.kts.
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Backup: JSON serialization + Google Sign-In (Drive OAuth token).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.auth)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
