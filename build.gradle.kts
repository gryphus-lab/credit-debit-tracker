// Top-level build file. Plugins are declared here (apply false) and applied in
// the :app module, so their versions resolve from the version catalog.

// --- Security: pin vulnerable *build-tooling* transitives -------------------
// The Android Gradle Plugin, the SonarQube Gradle plugin and the foojay
// toolchain resolver drag in old copies of guava, commons-lang3, httpclient
// and bouncycastle on the BUILD classpath. None of these reach the shipped
// APK (the release runtime classpath carries only the harmless guava
// `listenablefuture` stub), so there is no on-device attack surface - but
// GitHub Dependabot still flags the build graph. Forcing the patched versions
// clears the alerts without changing anything that runs on a user's phone.
buildscript {
    configurations.all {
        resolutionStrategy.force(
            "com.google.guava:guava:33.7.2-jre",          // Dependabot #58 (high)
            "org.apache.commons:commons-lang3:3.18.0",     // Dependabot #57 (medium)
            "org.apache.httpcomponents:httpclient:4.5.14", // Dependabot #56 (medium)
            "org.bouncycastle:bcprov-jdk18on:1.85",        // Dependabot #54 (critical), #55 (high)
            "org.bouncycastle:bcpkix-jdk18on:1.85",        // keep BC family aligned
            "org.bouncycastle:bcutil-jdk18on:1.85",        // keep BC family aligned
        )
    }
}

plugins {
    // AGP 9 with built-in Kotlin; Compose compiler + serialization + KSP in :app.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.sonarqube)
}

// Ensure the JaCoCo unit-test coverage report exists before the Sonar scan.
// `build sonar` does not trigger the Android unit-test coverage task on its own.
tasks.named("sonar") {
    dependsOn(":app:createDebugUnitTestCoverageReport")
}

sonar {
    properties {
        property("sonar.projectKey", "gryphus-lab_credit-debit-tracker")
        property("sonar.organization", "gryphus-lab")

        // JaCoCo XML from the debug unit-test coverage report (enableUnitTestCoverage).
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${project.rootDir}/app/build/reports/coverage/test/debug/report.xml",
        )

        // UI / framework plumbing and generated code aren't meaningfully
        // unit-testable on the JVM; keep the coverage metric focused on the
        // pure logic that IS tested (Money, ThemeMode, …).
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/MainActivity.kt",
                "**/LedgerViewModel.kt",
                "**/ui/**",
                "**/*Screen.kt",
                // Android-framework data glue (Room DB/DAO/repository, prefs).
                // The pure, tested helpers (Money, ThemeMode enum) are measured
                // via dedicated paths below, not excluded.
                "**/data/ContactName.kt",
                "**/data/CurrencyManager.kt",
                "**/data/Entities.kt",
                "**/data/LedgerDao.kt",
                "**/data/LedgerDatabase.kt",
                "**/data/LedgerRepository.kt",
                "**/data/ThemePreference.kt",
                // Backup glue: @Serializable DTOs + the Drive REST/Sign-In
                // client (both Android/network, not JVM-testable). The pure
                // BackupSerializer IS tested and stays measured.
                "**/data/backup/BackupModels.kt",
                "**/data/backup/DriveBackup.kt",
                "**/databinding/**",
            ).joinToString(","),
        )
    }
}
