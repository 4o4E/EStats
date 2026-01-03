plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
}

dependencies {
    // velocity
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    // eplugin
    implementation(eplugin("velocity-core"))
    // Bstats
    implementation("org.bstats:bstats-velocity:3.0.2")
    // cron4j
    implementation("it.sauronsoftware.cron4j:cron4j:2.2.5")
    implementation(project(":common"))

    // hikari
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.springframework:spring-expression:7.0.2")
    implementation("net.bytebuddy:byte-buddy:1.18.3")
    implementation("net.bytebuddy:byte-buddy-agent:1.18.3")
}

tasks {
    build {
        finalizedBy(shadowJar)
    }

    shadowJar {
        val archiveName = "EStatsVelocity-${project.version}.jar"
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
        filesMatching("velocity-plugin.json") {
            expand("version" to project.version)
        }
    }
}
