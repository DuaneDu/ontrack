package net.nemerosa.ontrack.graphql

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.graphql.schema.UserError
import net.nemerosa.ontrack.graphql.service.GraphQLService
import net.nemerosa.ontrack.graphql.support.exception
import net.nemerosa.ontrack.it.links.AbstractBranchLinksTestSupport
import net.nemerosa.ontrack.json.JsonUtils
import net.nemerosa.ontrack.json.isNullOrNullNode
import net.nemerosa.ontrack.json.parse
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.fail

abstract class AbstractQLKTITSupport : AbstractBranchLinksTestSupport() {

    @Autowired
    private lateinit var graphQLService: GraphQLService

    fun run(query: String, variables: Map<String, Any> = emptyMap<String, Any>()): JsonNode {
        // Task to run
        val code = { internalRun(query, variables) }
        // Making sure we're at least authenticated
        return if (securityService.isLogged) {
            code()
        } else {
            asUser().call(code)
        }
    }

    fun run(query: String, variables: Map<String, Any> = emptyMap<String, Any>(), code: (data: JsonNode) -> Unit) {
        run(query, variables).let { data ->
            code(data)
        }
    }

    private fun internalRun(query: String, variables: Map<String, Any> = emptyMap<String, Any>()): JsonNode {
        val result = graphQLService.execute(
                query,
                variables,
        )
        val error = result.exception
        if (error != null) {
            throw error
        } else if (result.errors != null && !result.errors.isEmpty()) {
            fail(result.errors.joinToString("\n") { it.message })
        } else {
            val data: Any? = result.getData()
            if (data != null) {
                return JsonUtils.format(data)
            } else {
                fail("No data was returned and no error was thrown.")
            }
        }
    }

    protected fun assertNoUserError(data: JsonNode, userNodeName: String): JsonNode {
        val userNode = data.path(userNodeName)
        val errors = userNode.path("errors")
        if (!errors.isNullOrNullNode() && errors.isArray && errors.size() > 0) {
            errors.forEach { error: JsonNode ->
                error.path("exception")
                    .takeIf { !it.isNullOrNullNode() }
                    ?.let { println("Error exception: ${it.asText()}") }
                error.path("location")
                    .takeIf { !it.isNullOrNullNode() }
                    ?.let { println("Error location: ${it.asText()}") }
                fail(error.path("message").asText())
            }
        }
        return userNode
    }

    protected fun assertUserError(
        data: JsonNode,
        userNodeName: String,
        message: String? = null,
        exception: String? = null
    ) {
        val errors = data.path(userNodeName).path("errors")
        if (errors.isNullOrNullNode()) {
            fail("Excepted the `errors` user node.")
        } else if (!errors.isArray) {
            fail("Excepted the `errors` user node to be an array.")
        } else if (errors.isEmpty) {
            fail("Excepted the `errors` user node to be a non-empty array.")
        } else {
            val error = errors.first()
            if (message != null) {
                assertEquals(message, error.path("message").asText())
            }
            if (exception != null) {
                assertEquals(exception, error.path("exception").asText())
            }
        }
    }

    protected fun checkGraphQLUserErrors(data: JsonNode, field: String): JsonNode {
        val payload = data.path(field)
        val node = payload.path("errors")
        if (node != null && node.isArray && node.size() > 0) {
            val error = node.first().parse<UserError>()
            throw IllegalStateException(error.toString())
        }
        return payload
    }

    protected fun checkGraphQLUserErrors(data: JsonNode, field: String, code: (payload: JsonNode) -> Unit) {
        val payload = checkGraphQLUserErrors(data, field)
        code(payload)
    }

}
