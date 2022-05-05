package net.nemerosa.ontrack.kdsl.spec.extension.github.ingestion

import com.apollographql.apollo.api.Input
import net.nemerosa.ontrack.kdsl.connector.Connected
import net.nemerosa.ontrack.kdsl.connector.Connector
import net.nemerosa.ontrack.kdsl.connector.graphql.convert
import net.nemerosa.ontrack.kdsl.connector.graphql.paginate
import net.nemerosa.ontrack.kdsl.connector.graphql.schema.GitHubIngestionPayloadsQuery
import net.nemerosa.ontrack.kdsl.connector.graphql.schema.GitHubIngestionValidateDataByRunIdMutation
import net.nemerosa.ontrack.kdsl.connector.graphql.schema.type.IngestionHookPayloadStatus
import net.nemerosa.ontrack.kdsl.connector.graphqlConnector
import net.nemerosa.ontrack.kdsl.connector.support.PaginatedList
import net.nemerosa.ontrack.kdsl.connector.support.emptyPaginatedList
import java.util.*

/**
 * Management of the GitHub ingestion.
 */
class GitHubIngestionMgt(connector: Connector) : Connected(connector) {

    /**
     * Getting a list of payloads.
     */
    fun payloads(
        offset: Int = 0,
        size: Int = 10,
        uuid: String? = null,
        statuses: List<String>? = null,
        gitHubEvent: String? = null,
        repository: String? = null,
    ): PaginatedList<GitHubIngestionPayload> = graphqlConnector.query(
        GitHubIngestionPayloadsQuery(
            Input.fromNullable(offset),
            Input.fromNullable(size),
            Input.fromNullable(uuid),
            Input.fromNullable(
                statuses?.map {
                    IngestionHookPayloadStatus.valueOf(it)
                }
            ),
            Input.fromNullable(gitHubEvent),
            Input.fromNullable(repository),
        )
    )?.paginate(
        pageInfo = { it.gitHubIngestionHookPayloads()?.pageInfo()?.fragments()?.pageInfoContent() },
        pageItems = { it.gitHubIngestionHookPayloads()?.pageItems() },
    )?.map {
        GitHubIngestionPayload(
            uuid = it.uuid()!!,
            status = it.status().name,
            message = it.message(),
        )
    } ?: emptyPaginatedList()

    /**
     * Sends some validation data for a repository
     */
    fun validateDataByRunId(
        owner: String,
        repository: String,
        runId: Long,
        validation: String,
        validationData: GitHubIngestionValidationDataInput,
        validationStatus: String?,
    ): UUID =
        graphqlConnector.mutate(
            GitHubIngestionValidateDataByRunIdMutation(
                owner,
                repository,
                runId,
                validation,
                net.nemerosa.ontrack.kdsl.connector.graphql.schema.type.GitHubIngestionValidationDataInput.builder()
                    .data(validationData.data)
                    .type(validationData.type)
                    .build(),
                Input.fromNullable(validationStatus),
            )
        ) {
            it?.gitHubIngestionValidateDataByRunId()?.fragments()?.payloadUserErrors()?.convert()
        }?.gitHubIngestionValidateDataByRunId()?.payload()?.uuid()?.let { UUID.fromString(it) }
            ?: error("Could not get the UUID of the processed request")

}