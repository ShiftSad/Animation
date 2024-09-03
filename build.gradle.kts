plugins {
    kotlin("jvm") version "2.0.20"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "codes.shiftmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://maven.enginehub.org/repo/") {
        name = "enginehub"
    }
    maven("https://repo.dmulloy2.net/repository/public/") {
        name = "protocolLib"
    }
    maven("https://repo.codemc.org/repository/maven-public/") {
        name = "codemc"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    compileOnly("dev.jorel:commandapi-bukkit-core:9.5.2")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.4.0-SNAPSHOT")
}

tasks.runServer {
    downloadPlugins {
        modrinth("worldedit", "yAujLUIK") // 7.3.6
        modrinth("commandapi", "9.5.3")
        url("https://ci.dmulloy2.net/job/ProtocolLib/726/artifact/build/libs/ProtocolLib.jar") // ProtocolLib 5.3.0
    }

    minecraftVersion("1.21.1")
}

// Make ShadowJar name file {project.name} and not depend on build
tasks.shadowJar {
    archiveFileName.set("${project.name}.jar")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
