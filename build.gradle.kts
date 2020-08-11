/*
 * Copyright (c) 2020. UltraDev
 */

group = "net.ultragrav"
version = "1.1.0"

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
    implementation("com.github.luben:zstd-jni:1.4.5-6")

    implementation("org.projectlombok:lombok:1.18.8")
    annotationProcessor("org.projectlombok:lombok:1.18.8")
}