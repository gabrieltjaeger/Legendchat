plugins {
    java
}

group = "br.com.devpaulo"
version = "1.1.2"

val pluginVersion = version.toString()

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven("https://jitpack.io")
    maven("https://repo.helpch.at/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.72-stable")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.12.3")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("."))
        resources {
            include("plugin.yml")
            include("config_template.yml")
            include("temporary_channels.yml")
            include("language/**")
        }
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val props = mapOf("version" to pluginVersion)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.jar {
    archiveBaseName.set("Legendchat")
}
