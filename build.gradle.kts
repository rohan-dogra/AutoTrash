plugins {
    `java-library`
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("com.gradleup.shadow") version "9.6.1"
}

group = property("group") as String
version = providers.exec {
    commandLine("git", "describe", "--tags", "--abbrev=0")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().removePrefix("v") }.orElse("dev").get().ifEmpty { "dev" }
description = "Automatically trash unwanted items on pickup"

// Release jars target Java 21 against the 1.21.11 API, so one jar runs on every
// supported server: 1.20.6-1.21.x (Java 21) and 26.x (Java 25 runs Java 21 bytecode).
// CI overrides these to compile-check against the oldest and newest APIs, e.g.
//   ./gradlew compileJava -PpaperApi=26.3.build.+ -PjavaVersion=25
val paperApi = providers.gradleProperty("paperApi").getOrElse("1.21.11-R0.1-SNAPSHOT")
val javaVersion = providers.gradleProperty("javaVersion").getOrElse("21").toInt()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApi")
}

tasks {
    compileJava {
        options.release = javaVersion
        options.encoding = "UTF-8"
    }

    processResources {
        val props = mapOf(
            "version" to project.version,
            "description" to project.description
        )
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    // Keep the plain jar from colliding with the shadow jar's file name
    // (autotrash-x.jar vs AutoTrash-x.jar are the same file on Windows/macOS).
    jar {
        archiveClassifier = "plain"
    }

    shadowJar {
        archiveBaseName = "AutoTrash"
        archiveClassifier = ""
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("1.21.11")
    }
}
