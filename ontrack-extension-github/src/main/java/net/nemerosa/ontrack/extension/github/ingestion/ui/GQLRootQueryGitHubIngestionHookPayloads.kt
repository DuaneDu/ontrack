package net.nemerosa.ontrack.extension.github.ingestion.ui

import graphql.Scalars.GraphQLString
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import net.nemerosa.ontrack.extension.github.ingestion.payload.IngestionHookPayload
import net.nemerosa.ontrack.extension.github.ingestion.payload.IngestionHookPayloadStatus
import net.nemerosa.ontrack.extension.github.ingestion.payload.IngestionHookPayloadStorage
import net.nemerosa.ontrack.graphql.schema.GQLRootQuery
import net.nemerosa.ontrack.graphql.schema.GQLTypeCache
import net.nemerosa.ontrack.graphql.schema.listInputType
import net.nemerosa.ontrack.graphql.support.pagination.GQLPaginatedListFactory
import net.nemerosa.ontrack.model.pagination.PaginatedList
import org.springframework.stereotype.Component

@Component
class GQLRootQueryGitHubIngestionHookPayloads(
    private val gqlPaginatedListFactory: GQLPaginatedListFactory,
    private val gqlGitHubIngestionHookPayload: GQLGitHubIngestionHookPayload,
    private val gqlEnumIngestionHookPayloadStatus: GQLEnumIngestionHookPayloadStatus,
    private val ingestionHookPayloadStorage: IngestionHookPayloadStorage,
) : GQLRootQuery {
    override fun getFieldDefinition(): GraphQLFieldDefinition =
        gqlPaginatedListFactory.createPaginatedField<Any?, IngestionHookPayload>(
            cache = GQLTypeCache(),
            fieldName = "gitHubIngestionHookPayloads",
            fieldDescription = "List of payloads received by the GitHub Ingestion Hook payload",
            itemType = gqlGitHubIngestionHookPayload,
            arguments = listOf(
                GraphQLArgument.newArgument()
                    .name(ARG_STATUSES)
                    .description("Filter on the statuses")
                    .type(
                        listInputType(
                            gqlEnumIngestionHookPayloadStatus.getTypeRef(),
                            nullable = true,
                        )
                    )
                    .build(),
                GraphQLArgument.newArgument()
                    .name(ARG_UUID)
                    .description("Filter on the UUID")
                    .type(GraphQLString)
                    .build(),
            ),
            itemPaginatedListProvider = { env, _, offset, size ->
                val uuid: String? = env.getArgument(ARG_UUID)
                if (uuid != null) {
                    val payload = ingestionHookPayloadStorage.findByUUID(uuid)
                    if (payload != null) {
                        PaginatedList.ofOne(payload)
                    } else {
                        PaginatedList.empty()
                    }
                } else {
                    val statusesList: List<String>? = env.getArgument(ARG_STATUSES)
                    val statuses = statusesList?.map {
                        IngestionHookPayloadStatus.valueOf(it)
                    }
                    val count = ingestionHookPayloadStorage.count(statuses = statuses)
                    val items = ingestionHookPayloadStorage.list(offset, size, statuses = statuses)
                    PaginatedList.create(
                        items = items,
                        offset = offset,
                        pageSize = size,
                        total = count,
                    )
                }
            },
        )

    companion object {
        private const val ARG_UUID = "uuid"
        private const val ARG_STATUSES = "statuses"
    }

}