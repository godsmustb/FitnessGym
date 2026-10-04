// Desktop tool: renders motion templates / exercises to PNG contact sheets for visual QA.
// Usage: gradlew :preview:run --args="squat lunge"   or   --args="all"
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin { jvmToolchain(17) }

application { mainClass.set("com.nunna.fitnessgym.preview.PreviewMainKt") }

dependencies {
    implementation(project(":anim"))
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    jvmArgs("-Djava.awt.headless=true")
}
