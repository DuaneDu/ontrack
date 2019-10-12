dependencies {
    compile(project(":ontrack-common"))
    compile(project(":ontrack-model"))
    compile(project(":ontrack-test-utils"))
    compile(project(":ontrack-extension-support"))
    compile(project(":ontrack-ui-support"))
    compile("org.springframework:spring-context")
    compile("org.springframework:spring-jdbc")
    compile("org.springframework:spring-test")
    compile("org.springframework.security:spring-security-core")
    compile("org.springframework.boot:spring-boot-starter-jdbc")
    compile("org.slf4j:slf4j-api")
    compile("org.springframework.boot:spring-boot-starter-actuator")
    compile("org.springframework.boot:spring-boot-starter-test")
    compile("io.micrometer:micrometer-core")

    runtime("org.postgresql:postgresql")
    runtime("org.flywaydb:flyway-core")
}