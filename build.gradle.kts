// Top-level build file. Plugins are declared here (apply false) and applied in
// the :app module, so their versions resolve from the version catalog.
plugins {
    // AGP 8.x line + standalone Kotlin plugin, so Room's KSP and the Compose
    // compiler plugin can all share one pinned Kotlin version.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
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
                "**/data/CurrencyManager.kt",
                "**/data/Entities.kt",
                "**/data/LedgerDao.kt",
                "**/data/LedgerDatabase.kt",
                "**/data/LedgerRepository.kt",
                "**/data/ThemePreference.kt",
                "**/databinding/**",
            ).joinToString(","),
        )
    }
}
