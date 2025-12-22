plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(
        libs.versions.jdkVersion
            .get()
            .toInt(),
    )
}

dependencies {
    implementation(libs.koog.agents.core)
    implementation(libs.koog.prompt.executor.llms.all)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.client.apache5)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(project.dependencies.platform(libs.ktor.bom))
}

tasks.register<JavaExec>("runMain") {
    mainClass.set("koog.ssd.MainKt")
    args(File(rootDir, "files").absolutePath)
    classpath = sourceSets["main"].runtimeClasspath
}
