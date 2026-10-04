// Pure Kotlin: figure rig, IK, muscles, motion + exercise models. No Android imports,
// so the same code renders on the phone (Compose) and in the PNG preview tool (AWT).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.test {
    // Tests validate the real content files.
    systemProperty("contentDir", rootProject.file("content").absolutePath)
}
