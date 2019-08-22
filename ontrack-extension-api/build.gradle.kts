plugins {
    `java-library`
}

dependencies {
    api(project(":ontrack-model"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.apache.commons:commons-lang3")
    implementation("com.google.guava:guava")
}

val testJar by tasks.registering(Jar::class) {
    classifier = "tests"
    from(sourceSets["test"].output)
}

configure<PublishingExtension> {
    publications {
        maybeCreate<MavenPublication>("mavenCustom").artifact(tasks["testJar"])
    }
}

tasks["assemble"].dependsOn("testJar")

val tests by configurations.creating

artifacts {
    add("tests", testJar)
}
