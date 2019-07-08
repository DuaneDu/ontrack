plugins {
    groovy
    `java-library`
}

/**
 * Dependencies of the DSL module must be carefully controlled
 * outside of the core modules
 */

dependencies {
    api("org.codehaus.groovy:groovy-all:2.4.15")
    
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("org.apache.httpcomponents:httpclient:4.5.3")
    implementation("org.apache.httpcomponents:httpcore:4.4.6")
    implementation("org.apache.httpcomponents:httpmime:4.5.3")
    implementation("commons-logging:commons-logging:1.2")
    implementation("net.jodah:failsafe:0.9.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.9")

    testImplementation("junit:junit:4.12")
}

if (project.hasProperty("documentation")) {
    tasks.named<Jar>("javadocJar") {
        from("javadoc")
        from("groovydoc")
    }
}
