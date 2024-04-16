package net.nemerosa.ontrack.extension.jira.mock

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.extension.jira.JIRAConfiguration
import net.nemerosa.ontrack.extension.jira.client.JIRAClient
import net.nemerosa.ontrack.extension.jira.model.JIRAIssue
import net.nemerosa.ontrack.extension.jira.model.JIRAIssueStub

class MockJIRAClient(
    private val instance: MockJIRAInstance,
) : JIRAClient {

    override fun close() {
    }

    override fun getIssue(key: String, configuration: JIRAConfiguration): JIRAIssue? =
        instance.getIssue(key)

    override fun createIssue(
        configuration: JIRAConfiguration,
        project: String,
        issueType: String,
        labels: List<String>,
        fixVersion: String?,
        assignee: String?,
        title: String,
        customFields: Map<String, JsonNode>,
        body: String
    ): JIRAIssueStub {
        TODO("Not yet implemented")
    }

    override val projects: List<String>
        get() = instance.projectNames

}