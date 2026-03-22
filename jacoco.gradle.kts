apply(plugin = "jacoco")

configure<JacocoPluginExtension> {
    toolVersion = "0.8.11"
}

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val fileFilter = mutableSetOf(
    "**/R.class",
    "**/R\$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    "**/*_MembersInjector.class",
    "**/Dagger*Component.class",
    "**/Dagger*Component\$Builder.class",
    "**/*_*Factory.class",
    "**/*ComponentImpl.class",
    "**/*SubComponentBuilder.class",
    "**/presentation/**",
    "**/manager/**",
    "**/data/local/auth/**",
    "**/database/**",
    "**/di/**"
)

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val debugTree = fileTree("${project.layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get().asFile) {
        include("jacoco/testDebugUnitTest.exec")
    })
}
