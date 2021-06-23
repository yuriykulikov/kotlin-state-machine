buildscript {
    repositories {
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.30")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.15.1")
    }
}

repositories {
    mavenCentral()
}

plugins {
    // adds aggregateJacocoReport
    // see https://kordamp.org/kordamp-gradle-plugins/#_task_aggregate_jacoco_report
    id("org.kordamp.gradle.jacoco") version "0.45.0"
}
