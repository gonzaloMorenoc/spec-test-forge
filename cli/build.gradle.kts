plugins {
    application
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
    implementation(project(":core"))
    implementation("info.picocli:picocli:4.7.5")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

application {
    mainClass.set("com.specforge.cli.Main")
}