import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    `maven-publish`
}

group = "org.atlasin"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("jitpack") {
            groupId = "com.github.Atlas-Initiative"
            artifactId = "Alcove"
            version = rootProject.version.toString()

            from(components["java"])
        }
    }
}
tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}