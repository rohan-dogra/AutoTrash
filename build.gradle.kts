plugins {
    `java-library`
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("com.gradleup.shadow") version "9.6.1"
    id("com.modrinth.minotaur") version "2.10.0"
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

// Minecraft versions advertised on Modrinth. Add new versions here once tested.
val supportedGameVersions = listOf(
    "1.20.6",
    "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6",
    "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
    "26.1", "26.1.1", "26.1.2", "26.2", "26.3",
)

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

    // Test on any supported version: ./gradlew runServer -PmcVersion=26.3
    runServer {
        val mcVersion = providers.gradleProperty("mcVersion").getOrElse("1.21.11")
        minecraftVersion(mcVersion)
        // 26.x servers require Java 25; older ones run on Java 21.
        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(if (mcVersion.startsWith("1.")) 21 else 25)
        }
    }
}

modrinth {
    token = providers.environmentVariable("MODRINTH_TOKEN")
    projectId = "X9IcZoco"
    versionNumber = project.version.toString()
    versionType = providers.environmentVariable("MODRINTH_VERSION_TYPE").orElse("release")
    changelog = providers.environmentVariable("MODRINTH_CHANGELOG").orElse("")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(supportedGameVersions)
    loaders.addAll("paper", "purpur")
    // ./gradlew modrinth -PmodrinthDryRun prints the upload instead of publishing it.
    debugMode = providers.gradleProperty("modrinthDryRun").isPresent
}
