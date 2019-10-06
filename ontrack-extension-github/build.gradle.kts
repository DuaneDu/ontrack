import net.nemerosa.ontrack.gradle.extension.OntrackExtensionPlugin

apply<OntrackExtensionPlugin>()

dependencies {
    compile(project(":ontrack-extension-git"))
    compile("org.eclipse.mylyn.github:org.eclipse.egit.github.core:2.1.5")

    testCompile(project(":ontrack-test-utils"))
    testCompile(project(":ontrack-it-utils"))
    testCompile("org.springframework.boot:spring-boot-starter-actuator")
    testRuntime(project(":ontrack-service"))
    testRuntime(project(":ontrack-repository-impl"))
    testCompile(project(path = ":ontrack-extension-issues", configuration = "tests"))
}