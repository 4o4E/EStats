plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
}

dependencies {
    // spigot
    compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")
    // eplugin
    implementation(eplugin("core"))
    implementation(eplugin("serialization"))
    // Bstats
    implementation("org.bstats:bstats-bukkit:3.0.2")
    // cron4j
    implementation("it.sauronsoftware.cron4j:cron4j:2.2.5")
    implementation(project(":common"))

    // hikari
    compileOnly("com.zaxxer:HikariCP:7.0.2")
    compileOnly("org.springframework:spring-expression:7.0.2")
    compileOnly("net.bytebuddy:byte-buddy:1.18.3")
    compileOnly("net.bytebuddy:byte-buddy-agent:1.18.3")
}

tasks {
    build {
        finalizedBy(shadowJar)
    }

    shadowJar {
        val archiveName = "EStats-${project.version}.jar"
        archiveFileName.set(archiveName)

        relocate("org.bstats", "top.e404.estats.relocate.bstats")
        relocate("kotlin", "top.e404.estats.relocate.kotlin")
        relocate("top.e404.eplugin", "top.e404.estats.relocate.eplugin")
        relocate("com.charleskorn.kaml", "top.e404.estats.relocate.kaml")
        exclude("META-INF/**")

        doLast {
            val archiveFile = archiveFile.get().asFile
            println(archiveFile.parentFile.absolutePath)
            println(archiveFile.absolutePath)
        }
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}
