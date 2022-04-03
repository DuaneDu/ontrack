package net.nemerosa.ontrack.extension.notifications.webhooks

import net.nemerosa.ontrack.common.Time
import net.nemerosa.ontrack.model.settings.CachedSettingsService
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.LocalDateTime

@Service
class DefaultWebhookExecutionService(
    private val webhookPayloadRenderer: WebhookPayloadRenderer,
    private val webhookAuthenticatorRegistry: WebhookAuthenticatorRegistry,
    private val cachedSettingsService: CachedSettingsService,
    private val webhookExchangeService: WebhookExchangeService,
) : WebhookExecutionService {

    override fun send(webhook: Webhook, payload: WebhookPayload<*>) {
        val client: HttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build()

        // Gets the maximum timeout between the webhook one and the general settings
        val settings = cachedSettingsService.getCachedSettings(WebhookSettings::class.java)
        val settingsTimeout = Duration.ofMinutes(settings.timeoutMinutes.toLong())
        val timeout = maxOf(settingsTimeout, webhook.timeout)

        val payloadString = webhookPayloadRenderer.render(payload)

        val request: HttpRequest =
            HttpRequest.newBuilder()
                .uri(URI.create(webhook.url))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                // Authentication
                .apply {
                    authenticate(webhook, this)
                }
                // Payload
                .POST(BodyPublishers.ofString(payloadString))
                // OK
                .build()

        val start = Time.now()

        try {
            client
                .sendAsync(request, BodyHandlers.ofString())
                .thenAccept { response ->
                    store(webhook, payload, payloadString, start, response)
                }
        } catch (any: Exception) {
            store(webhook, payload, payloadString, start, any)
        }
    }

    private fun store(
        webhook: Webhook,
        payload: WebhookPayload<*>,
        payloadString: String,
        start: LocalDateTime,
        any: Exception,
    ) {
        webhookExchangeService.store(
            WebhookExchange(
                uuid = payload.uuid,
                webhook = webhook.name,
                request = WebhookRequest(
                    timestamp = start,
                    type = payload.type,
                    payload = payloadString,
                ),
                response = null,
                stack = ExceptionUtils.getStackTrace(any),
            )
        )
    }

    private fun store(
        webhook: Webhook,
        payload: WebhookPayload<*>,
        payloadString: String,
        start: LocalDateTime,
        response: HttpResponse<String>,
    ) {
        webhookExchangeService.store(
            WebhookExchange(
                uuid = payload.uuid,
                webhook = webhook.name,
                request = WebhookRequest(
                    timestamp = start,
                    type = payload.type,
                    payload = payloadString,
                ),
                response = WebhookResponse(
                    timestamp = Time.now(),
                    code = response.statusCode(),
                    payload = response.body() ?: "",
                ),
                stack = null,
            )
        )
    }

    private fun authenticate(webhook: Webhook, builder: HttpRequest.Builder) {
        val authenticator = webhookAuthenticatorRegistry.findWebhookAuthenticator(webhook.authentication.type)
            ?: throw WebhookAuthenticatorNotFoundException(webhook.authentication.type)
        authenticate(webhook, authenticator, builder)
    }

    private fun <C> authenticate(
        webhook: Webhook,
        authenticator: WebhookAuthenticator<C>,
        builder: HttpRequest.Builder,
    ) {
        val config = authenticator.validateConfig(webhook.authentication.config)
        authenticator.authenticate(config, builder)
    }

}