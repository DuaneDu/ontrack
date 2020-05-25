package net.nemerosa.ontrack.extension.indicators.ui.graphql

import graphql.schema.GraphQLFieldDefinition
import net.nemerosa.ontrack.graphql.schema.GQLRootQuery
import org.springframework.stereotype.Component

@Component
class GQLRootQueryIndicatorCategories(
        private val indicatorCategories: GQLTypeIndicatorCategories
) : GQLRootQuery {

    override fun getFieldDefinition(): GraphQLFieldDefinition =
            GraphQLFieldDefinition.newFieldDefinition()
                    .name("indicatorCategories")
                    .description("List of indicator categories")
                    .type(indicatorCategories.typeRef)
                    .dataFetcher { IndicatorCategories() }
                    .build()

}