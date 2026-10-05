// Pure Kotlin training logic: workout generator, progression, records, gamification.
// No Android imports, so every rule is unit-tested on the JVM against the real content.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(17) }

dependencies {
    api(project(":anim"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.test { systemProperty("contentDir", rootProject.file("content").absolutePath) }
