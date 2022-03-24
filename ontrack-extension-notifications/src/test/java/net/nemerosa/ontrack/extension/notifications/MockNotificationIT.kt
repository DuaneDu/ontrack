package net.nemerosa.ontrack.extension.notifications

import net.nemerosa.ontrack.extension.notifications.mock.MockNotificationChannel
import net.nemerosa.ontrack.extension.notifications.mock.MockNotificationChannelConfig
import net.nemerosa.ontrack.extension.notifications.subscriptions.EventSubscriptionService
import net.nemerosa.ontrack.extension.notifications.subscriptions.subscribe
import net.nemerosa.ontrack.it.AbstractDSLTestSupport
import net.nemerosa.ontrack.model.events.EventFactory
import net.nemerosa.ontrack.test.TestUtils.uid
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Notification integration test using a mock channel.
 */
@TestPropertySource(
    properties = [
        "ontrack.extension.notifications.queue.async=false",
    ]
)
class MockNotificationIT : AbstractDSLTestSupport() {

    @Autowired
    private lateinit var eventSubscriptionService: EventSubscriptionService

    @Autowired
    private lateinit var mockNotificationChannel: MockNotificationChannel

    @Test
    fun `Notification for a branch being created`() {
        project {
            // Subscription to the creation of branches for this project
            val target = uid("t")
            eventSubscriptionService.subscribe(
                mockNotificationChannel,
                MockNotificationChannelConfig(target),
                this,
                EventFactory.NEW_BRANCH,
            )
            // Creating a branch
            branch("my-branch")
            // Checking we got a message on the mock channel
            assertNotNull(mockNotificationChannel.messages[target]) { messages ->
                assertEquals(1, messages.size)
                assertEquals("New branch my-branch for project $name.", messages.first())
            }
        }
    }

}