package net.nemerosa.ontrack.kdsl.acceptance.tests.github

import net.nemerosa.ontrack.kdsl.acceptance.tests.ACCProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestTemplate

/**
 * Playground client
 */

val gitHubPlaygroundClient: RestTemplate by lazy {
    RestTemplateBuilder()
        .rootUri("https://api.github.com")
        .defaultHeader(
            HttpHeaders.AUTHORIZATION,
            "Bearer ${ACCProperties.GitHub.token}"
        )
        .build()
}
