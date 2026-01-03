plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    compileOnly(kotlinx("serialization-core-jvm", "1.9.0"))
    // cron4j
    implementation("it.sauronsoftware.cron4j:cron4j:2.2.5")

    // hikari
    compileOnly("com.zaxxer:HikariCP:7.0.2")
    compileOnly("org.springframework:spring-expression:7.0.2")
    compileOnly("net.bytebuddy:byte-buddy:1.18.3")
    compileOnly("net.bytebuddy:byte-buddy-agent:1.18.3")
}
