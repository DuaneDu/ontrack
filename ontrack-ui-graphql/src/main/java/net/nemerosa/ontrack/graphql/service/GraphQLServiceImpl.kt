package net.nemerosa.ontrack.graphql.service

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.execution.ExecutionStrategy
import graphql.schema.GraphQLSchema
import net.nemerosa.ontrack.tx.TransactionService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GraphQLServiceImpl(
        private val graphQLExceptionHandlers: List<GraphQLExceptionHandler>,
        @Qualifier("queryExecutionStrategy")
        private val queryExecutionStrategy: ExecutionStrategy,
        @Qualifier("queryExecutionStrategy")
        private val mutationExecutionStrategy: ExecutionStrategy,
        private val transactionService: TransactionService
) : GraphQLService {
    override fun execute(
            schema: GraphQLSchema,
            query: String,
            variables: Map<String, Any>,
            operationName: String?,
            reportErrors: Boolean
    ): ExecutionResult {
        val result: ExecutionResult = transactionService.doInTransaction {
            GraphQL.newGraphQL(schema)
                    .queryExecutionStrategy(queryExecutionStrategy)
                    .mutationExecutionStrategy(mutationExecutionStrategy)
                    .build()
                    .execute(
                            ExecutionInput.newExecutionInput()
                                    .query(query)
                                    .operationName(operationName)
                                    .variables(variables)
                                    .build()
                    )
        }
        if (result.errors != null && !result.errors.isEmpty() && reportErrors) {
            result.errors.forEach { error ->
                graphQLExceptionHandlers.forEach {
                    it.handle(error)
                }
            }
            throw GraphQLServiceException(result.errors)
        } else {
            return result
        }
    }
}