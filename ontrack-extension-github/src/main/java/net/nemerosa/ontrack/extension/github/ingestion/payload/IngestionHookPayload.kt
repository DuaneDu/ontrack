package net.nemerosa.ontrack.extension.github.ingestion.payload

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.common.Time
import java.time.LocalDateTime
import java.util.*

/**
 * Payload for an ingestion to be processed.
 *
 * @property uuid Unique ID for this payload
 * @property timestamp Timestamp of reception for this payload
 * @property gitHubDelivery Mapped to the `X-GitHub-Delivery` header
 * @property gitHubEvent Mapped to the `X-GitHub-Event` header
 * @property gitHubHookID Mapped to the `X-GitHub-Hook-ID` header
 * @property gitHubHookInstallationTargetID Mapped to the `X-GitHub-Hook-Installation-Target-ID` header
 * @property gitHubHookInstallationTargetType Mapped to the `X-GitHub-Hook-Installation-Target-Type` header
 * @property payload JSON payload, raw from GitHub
 */
data class IngestionHookPayload(
    val uuid: UUID = UUID.randomUUID(),
    val timestamp: LocalDateTime = Time.now(),
    val gitHubDelivery: String,
    val gitHubEvent: String,
    val gitHubHookID: Int,
    val gitHubHookInstallationTargetID: Int,
    val gitHubHookInstallationTargetType: String,
    val payload: JsonNode,
)
