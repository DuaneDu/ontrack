package net.nemerosa.ontrack.extension.notifications.mock

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.extension.notifications.channels.AbstractNotificationChannel
import net.nemerosa.ontrack.extension.notifications.channels.NotificationResult
import net.nemerosa.ontrack.json.asJson
import net.nemerosa.ontrack.model.events.Event
import org.springframework.stereotype.Component

@Component
class OtherMockNotificationChannel :
    AbstractNotificationChannel<MockNotificationChannelConfig>(MockNotificationChannelConfig::class) {

    /**
     * List of messages received, indexed by target.
     */
    val messages = mutableMapOf<String, MutableList<String>>()

    override fun publish(config: MockNotificationChannelConfig, event: Event): NotificationResult {
        messages.getOrPut(config.target) { mutableListOf() }.add(event.renderText())
        return NotificationResult.ok()
    }

    override fun toSearchCriteria(text: String): JsonNode =
        mapOf(MockNotificationChannelConfig::target.name to text).asJson()

    override val type: String = "other-mock"
    override val enabled: Boolean = true
}