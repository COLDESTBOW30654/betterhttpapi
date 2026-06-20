plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1"
}

group = "top.blym"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation("org.eclipse.jetty:jetty-server:11.0.24")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.24")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.shadowJar {
    archiveBaseName.set("BetterHTTPAPI")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // 排除签名文件（避免 Paper 插件重映射器报 SecurityException）
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    // 排除重复的许可证文件（避免 Duplicate entries 错误）
    exclude("META-INF/LICENSE")
    exclude("META-INF/LICENSE.txt")
    exclude("META-INF/NOTICE")
    exclude("META-INF/NOTICE.txt")

    relocate("org.eclipse.jetty", "top.blym.libs.jetty")
    relocate("jakarta.servlet", "top.blym.libs.jakarta.servlet")
    relocate("com.fasterxml", "top.blym.libs.jackson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
