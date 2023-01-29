package net.nemerosa.ontrack.extension.jenkins.notifications

import io.mockk.every
import io.mockk.mockk
import net.nemerosa.ontrack.extension.jenkins.JenkinsConfiguration
import net.nemerosa.ontrack.extension.jenkins.JenkinsConfigurationService
import net.nemerosa.ontrack.extension.jenkins.client.JenkinsBuild
import net.nemerosa.ontrack.extension.jenkins.client.JenkinsClient
import net.nemerosa.ontrack.extension.jenkins.client.JenkinsClientFactory
import net.nemerosa.ontrack.extension.jenkins.client.JenkinsJob
import net.nemerosa.ontrack.extension.notifications.channels.NotificationResultType
import net.nemerosa.ontrack.model.events.Event
import net.nemerosa.ontrack.model.events.EventFactoryImpl
import net.nemerosa.ontrack.model.structure.*
import net.nemerosa.ontrack.test.TestUtils.uid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.test.assertEquals

class JenkinsNotificationChannelTest {

    private lateinit var jenkinsClientFactory: JenkinsClientFactory
    private lateinit var jenkinsClient: JenkinsClient

    private lateinit var jenkinsNotificationChannel: JenkinsNotificationChannel

    private lateinit var jenkinsConfigName: String
    private lateinit var jenkinsConfig: JenkinsConfiguration
    private lateinit var jenkinsConfigurationService: JenkinsConfigurationService

    @BeforeEach
    fun init() {
        jenkinsConfigName = uid("jc")
        jenkinsConfig = JenkinsConfiguration(
            name = jenkinsConfigName,
            url = URL,
            user = "someuser",
            password = "somepassword"
        )

        jenkinsConfigurationService = mockk()
        every { jenkinsConfigurationService.findConfiguration(jenkinsConfigName) } returns jenkinsConfig

        jenkinsClient = mockk()
        every { jenkinsClient.getJob(JOB) } returns JenkinsJob(
            name = "job",
            url = "${URL}/${JOB}"
        )

        jenkinsClientFactory = mockk()
        every { jenkinsClientFactory.getClient(jenkinsConfig) } returns jenkinsClient

        jenkinsNotificationChannel = JenkinsNotificationChannel(
            jenkinsConfigurationService,
            jenkinsClientFactory,
        )
    }

    @Test
    fun `Async job successfully queued`() {
        val config = newJenkinsNotificationConfig()
        val event = newPromotionRunEvent()

        every { jenkinsClient.fireAndForgetJob(JOB, emptyMap()) } returns URI("uri:queue")

        val result = jenkinsNotificationChannel.publish(config, event)

        assertEquals(NotificationResultType.OK, result.type)
    }

    @Test
    fun `Async job not successfully queued`() {
        val config = newJenkinsNotificationConfig()
        val event = newPromotionRunEvent()

        every { jenkinsClient.fireAndForgetJob(JOB, emptyMap()) } returns null

        val result = jenkinsNotificationChannel.publish(config, event)

        assertEquals(NotificationResultType.ERROR, result.type)
    }

    @Test
    fun `Sync job successfully finishing`() {
        val config = newJenkinsNotificationConfig(
            callMode = JenkinsNotificationChannelConfigCallMode.SYNC
        )
        val event = newPromotionRunEvent()

        val jenkinsBuild = JenkinsBuild(
            id = "build-id",
            building = false,
            url = "uri:build",
            result = "SUCCESS",
        )

        every { jenkinsClient.runJob(JOB, emptyMap(), any(), any()) } returns jenkinsBuild

        val result = jenkinsNotificationChannel.publish(config, event)

        assertEquals(NotificationResultType.OK, result.type)
    }

    @Test
    fun `Sync job finishing with an error`() {
        val config = newJenkinsNotificationConfig(
            callMode = JenkinsNotificationChannelConfigCallMode.SYNC
        )
        val event = newPromotionRunEvent()

        val jenkinsBuild = JenkinsBuild(
            id = "build-id",
            building = false,
            url = "uri:build",
            result = "FAILURE",
        )

        every { jenkinsClient.runJob(JOB, emptyMap(), any(), any()) } returns jenkinsBuild

        val result = jenkinsNotificationChannel.publish(config, event)

        assertEquals(NotificationResultType.ERROR, result.type)
    }

    private fun newJenkinsNotificationConfig(
        callMode: JenkinsNotificationChannelConfigCallMode = JenkinsNotificationChannelConfigCallMode.ASYNC,
    ) = JenkinsNotificationChannelConfig(
        config = jenkinsConfigName,
        job = JOB,
        parameters = emptyList(),
        callMode = callMode,
    )

    private fun newPromotionRunEvent(): Event {
        val project = Project.of(NameDescription.nd("project", "")).withId(ID.of(1))
        val branch = Branch.of(project, NameDescription.nd("main", "")).withId(ID.of(10))
        val promotionLevel = PromotionLevel.of(branch, NameDescription.nd("GOLD", "")).withId(ID.of(100))
        val build = Build.of(branch, NameDescription.nd("1", ""), Signature.of("test")).withId(ID.of(1000))
        val promotionRun = PromotionRun.of(build, promotionLevel, Signature.of("test"), null).withId(ID.of(10000))
        return EventFactoryImpl().newPromotionRun(promotionRun)
    }

    companion object {
        const val URL = "https://jenkins"
        const val JOB = "folder/job"
    }

}
