import com.vanniktech.maven.publish.SonatypeHost

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.15.1")
    }
}

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
    id("com.vanniktech.maven.publish.base")
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

plugins.withType<com.vanniktech.maven.publish.MavenPublishBasePlugin>() {
    group = "io.github.yuriykulikov"
    version = "1.0.0"
    mavenPublishing {
        publishToMavenCentral(SonatypeHost.S01)

        // Will only apply to non snapshot builds.
        signAllPublications()

        pom {
            name.value("kotlin-state-machine")
            description.value("Hierarchical state machine written in Kotlin")
            inceptionYear.value("2019")
            url.value("https://github.com/yuriykulikov/kotlin-state-machine")
            licenses {
                license {
                    name.value("MIT License")
                    url.value("https://raw.githubusercontent.com/yuriykulikov/kotlin-state-machine/main/LICENSE")
                    distribution.value("https://raw.githubusercontent.com/yuriykulikov/kotlin-state-machine/main/LICENSE")
                }
            }
            developers {
                developer {
                    id.value("yuriykulikov")
                    name.value("Yuriy Kulikov")
                    url.value("https://github.com/yuriykulikov/")
                }
            }
            scm {
                url.value("https://github.com/yuriykulikov/kotlin-state-machine")
                connection.value("scm:git:git://github.com/yuriykulikov/kotlin-state-machine.git")
                developerConnection.value("scm:git:ssh://git@github.com/yuriykulikov/kotlin-state-machine.git")
            }
        }
    }
}