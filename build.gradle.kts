plugins {
    java
}

java {
    // Use your system JDK (JAVA_HOME) directly
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

group = "com.iTimess.redlightgreenlight"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
}

// Handle duplicate resources gracefully
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Configure JAR output
tasks.jar {
    from(sourceSets.main.get().output) // includes classes + resources
    archiveBaseName.set("RedLightGreenLight") // JAR name
    archiveVersion.set("") // remove version suffix
}