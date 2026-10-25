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

sonar {
    properties {
        property("sonar.projectKey", "gryphus-lab_credit-debit-tracker")
        property("sonar.organization", "gryphus-lab")
        // UI / framework plumbing and generated code aren't meaningfully
        // unit-testable on the JVM; keep coverage focused on pure logic.
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/MainActivity.kt",
                "**/ui/**",
                "**/*Screen.kt",
                "**/data/**",
                "**/databinding/**",
            ).joinToString(","),
        )
    }
}
