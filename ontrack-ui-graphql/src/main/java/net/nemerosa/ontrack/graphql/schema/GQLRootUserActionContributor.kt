package net.nemerosa.ontrack.graphql.schema

import org.springframework.stereotype.Component
import java.net.URI

/**
 * Link definition
 */
class URIDefinition(
        val name: String,
        val securityCheck: () -> Boolean,
        val uri: () -> URI
)

/**
 * Contributes a list of fields to the user root actions.
 *
 * @see GQLRootQueryUserRootActions
 */
@Deprecated("""
    Security grants are given through GraphQL mutations.
    
    See UIActionsProvider.
        
    The GQLRootUserActionContributor mechanism will be removed in V5.
""")
interface GQLRootUserActionContributor {

    /**
     * List of links to add
     */
    val userRootActions: List<URIDefinition>

}

/**
 * NOP contributor
 */

@Component
class NOPGQLRootUserActionContributor(
        override val userRootActions: List<URIDefinition> = listOf()
) : GQLRootUserActionContributor
