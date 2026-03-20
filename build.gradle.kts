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
}
