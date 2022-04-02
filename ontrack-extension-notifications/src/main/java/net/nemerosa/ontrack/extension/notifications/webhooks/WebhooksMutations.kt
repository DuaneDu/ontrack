package net.nemerosa.ontrack.extension.notifications.webhooks

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.graphql.schema.Mutation
import net.nemerosa.ontrack.graphql.support.TypedMutationProvider
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class WebhooksMutations(
    private val webhookAdminService: WebhookAdminService,
) : TypedMutationProvider() {
    override val mutations: List<Mutation> = listOf(
        simpleMutation(
            name = "createWebhook",
            description = "Registers a webhook",
            input = CreateWebhookInput::class,
            outputName = "webhook",
            outputDescription = "Registered webhook",
            outputType = Webhook::class,
        ) { input ->
            webhookAdminService.createWebhook(
                name = input.name,
                enabled = input.enabled,
                url = input.url,
                timeout = Duration.ofSeconds(input.timeoutSeconds),
                authentication = WebhookAuthentication(
                    type = input.authenticationType,
                    config = input.authenticationConfig,
                )
            )
        }
    )
}

data class CreateWebhookInput(
    val name: String,
    val enabled: Boolean,
    val url: String,
    val timeoutSeconds: Long,
    val authenticationType: String,
    val authenticationConfig: JsonNode,
)
