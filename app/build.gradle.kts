import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
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
