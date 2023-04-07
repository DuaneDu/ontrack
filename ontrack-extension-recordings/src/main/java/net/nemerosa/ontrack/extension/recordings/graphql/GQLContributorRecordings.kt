package net.nemerosa.ontrack.extension.recordings.graphql

import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType
import net.nemerosa.ontrack.extension.api.ExtensionManager
import net.nemerosa.ontrack.extension.recordings.Recording
import net.nemerosa.ontrack.extension.recordings.RecordingsExtension
import net.nemerosa.ontrack.graphql.schema.GQLContributor
import net.nemerosa.ontrack.graphql.schema.GQLTypeCache
import net.nemerosa.ontrack.graphql.support.GraphQLBeanConverter
import org.springframework.stereotype.Component

/**
 * Contribution to the GraphQL schema by the recordings extensions.
 */
@Component
class GQLContributorRecordings(
        private val extensionManager: ExtensionManager,
) : GQLContributor {

    override fun contribute(cache: GQLTypeCache): Set<GraphQLType> {
        val set = mutableSetOf<GraphQLType>()
        extensionManager.getExtensions(RecordingsExtension::class.java).forEach { extension ->
            set += extension.contribute(cache)
        }
        return set
    }

    private fun <R : Recording> RecordingsExtension<R>.contribute(cache: GQLTypeCache): Set<GraphQLType> =
            graphQLContributions + setOf(
                    // Record
                    createRecordType(cache)
            )

    private fun <R : Recording> RecordingsExtension<R>.createRecordType(cache: GQLTypeCache): GraphQLObjectType =
            GraphQLObjectType.newObject()
                    .name("${prefix}Recording")
                    .description("Recording for $lowerDisplayName")
                    // Common fields
                    .fields(GraphQLBeanConverter.asObjectFields(Recording::class, cache))
                    // Specific fields
                    .fields(graphQLRecordFields(cache))
                    // Ok
                    .build()

    private val <R : Recording> RecordingsExtension<R>.prefix: String get() = id.replaceFirstChar { it.titlecase() }

    private val <R : Recording> RecordingsExtension<R>.lowerDisplayName: String get() = id.replaceFirstChar { it.lowercase() }

}