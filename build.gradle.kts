/*
 * Copyright (c) 2020. UltraDev
 */

group = "net.ultragrav"
version = "1.0.2"

plugins {
    `java-library`
    maven
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava-primitives:r03")

    implementation("org.projectlombok:lombok:1.18.8")
    annotationProcessor("org.projectlombok:lombok:1.18.8")
}