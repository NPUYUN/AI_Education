// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Fix for SQLite AccessDeniedException on Windows
val sqliteTmpDir = file("build_output/sqlite_tmp")
if (!sqliteTmpDir.exists()) {
    sqliteTmpDir.mkdirs()
}
System.setProperty("org.sqlite.tmpdir", sqliteTmpDir.absolutePath)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.2.1") // Ktlint engine version
        debug.set(false)
        verbose.set(true)
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(true) // Don't break the build immediately during format
        enableExperimentalRules.set(false)
        filter {
            exclude("**/generated/**")
            include("**/kotlin/**")
        }
    }

    afterEvaluate {
        if (plugins.hasPlugin("com.android.application") || plugins.hasPlugin("com.android.library")) {
            apply(from = "${rootProject.projectDir}/jacoco.gradle.kts")
        }
    }
}

