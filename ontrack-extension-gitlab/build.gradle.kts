import net.nemerosa.ontrack.gradle.extension.OntrackExtensionPlugin

apply<OntrackExtensionPlugin>()

dependencies {
    compile(project(":ontrack-extension-git"))
    compile("org.gitlab:java-gitlab-api:1.2.7")

    testCompile(project(":ontrack-test-utils"))
    testCompile(project(":ontrack-it-utils"))
    testCompile("org.springframework.boot:spring-boot-starter-actuator")
    testRuntime(project(":ontrack-service"))
    testRuntime(project(":ontrack-repository-impl"))
    testCompile(project(path = ":ontrack-extension-issues", configuration = "tests"))
}
