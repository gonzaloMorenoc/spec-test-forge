plugins {
    java
}

group = "com.specforge"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // OpenAPI parser
    implementation("io.swagger.parser.v3:swagger-parser:2.1.16")

    // AI
    implementation("dev.langchain4j:langchain4j:0.35.0")
    implementation("dev.langchain4j:langchain4j-ollama:0.35.0")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // Templates
    implementation("com.github.spullara.mustache.java:compiler:0.9.10")

    // Logging API
    implementation("org.slf4j:slf4j-api:2.0.13")

    // Tests
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
