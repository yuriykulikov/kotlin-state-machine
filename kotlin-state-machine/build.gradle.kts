plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.30")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("junit:junit:4.13")
}

tasks.test {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
