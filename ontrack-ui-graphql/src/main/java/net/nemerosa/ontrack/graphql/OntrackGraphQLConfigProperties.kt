package net.nemerosa.ontrack.graphql

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ontrack.config.graphql")
class OntrackGraphQLConfigProperties {

    /**
     * GraphQL instrumentation
     */
    var instrumentation = GraphQLIntrumentationProperties()

    class GraphQLIntrumentationProperties {

        /**
         * Enabling data loader stats
         */
        var dataloader: Boolean = false

        /**
         * Enabling tracing
         */
        var tracing: Boolean = false

    }

}