package net.nemerosa.ontrack.graphql.service

import graphql.ExecutionResult
import graphql.schema.GraphQLSchema

/**
 * Execution of GraphQL queries.
 */
interface GraphQLService {
    /**
     * Executes a GraphQL query
     *
     * @param schema Schema to run the query against
     * @param query GraphQL query
     * @param variables List of variables to associate with the query
     * @param operationName Optional name of the operation
     * @param reportErrors If `true`, converts the execution result errors into an exception
     * @return Execution result
     * @throws GraphQLServiceException If `reportErrors` is `true` and if there are some errors
     */
    @Throws(GraphQLServiceException::class)
    fun execute(
            schema: GraphQLSchema,
            query: String,
            variables: Map<String, Any> = mapOf(),
            operationName: String? = null,
            reportErrors: Boolean = false
    ): ExecutionResult
}