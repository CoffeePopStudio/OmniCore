plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.0.0-beta9"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Database connection pool
    implementation("com.zaxxer:HikariCP:6.3.0") {
        exclude(group = "org.slf4j")
    }

    // SQLite driver
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")

    // MySQL driver
    implementation("com.mysql:mysql-connector-j:9.2.0")

    // Embedded HTTP server
    implementation("io.javalin:javalin:6.1.3") {
        exclude(group = "org.slf4j")
    }

    // Password hashing (bcrypt)
    implementation("at.favre.lib:bcrypt:0.10.2")

    // JWT for Web panel authentication
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // JSON processing
    implementation("com.google.code.gson:gson:2.10.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    shadowJar {
        relocate("com.zaxxer.hikari", "cn.oneachina.onmicore.libs.hikari")
        relocate("io.javalin", "cn.oneachina.onmicore.libs.javalin")
        relocate("at.favre.lib", "cn.oneachina.onmicore.libs.bcrypt")
        relocate("io.jsonwebtoken", "cn.oneachina.onmicore.libs.jjwt")
        relocate("com.google.gson", "cn.oneachina.onmicore.libs.gson")
        archiveFileName.set("OnmiCore-${project.version}.jar")
        mergeServiceFiles()
        exclude("META-INF/LICENSE*")
        exclude("META-INF/NOTICE*")
        minimize {
            exclude(dependency("org.xerial:sqlite-jdbc:.*"))
            exclude(dependency("com.mysql:mysql-connector-j:.*"))
        }
    }

    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
