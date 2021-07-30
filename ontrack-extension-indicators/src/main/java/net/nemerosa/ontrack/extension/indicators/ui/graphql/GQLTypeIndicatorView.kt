package net.nemerosa.ontrack.extension.indicators.ui.graphql

import graphql.schema.GraphQLObjectType
import net.nemerosa.ontrack.extension.indicators.model.IndicatorCategoryService
import net.nemerosa.ontrack.extension.indicators.portfolio.IndicatorView
import net.nemerosa.ontrack.graphql.schema.GQLFieldContributor
import net.nemerosa.ontrack.graphql.schema.GQLType
import net.nemerosa.ontrack.graphql.schema.GQLTypeCache
import net.nemerosa.ontrack.graphql.schema.graphQLFieldContributions
import net.nemerosa.ontrack.graphql.support.GraphqlUtils.stdList
import net.nemerosa.ontrack.graphql.support.stringField
import org.springframework.stereotype.Component

@Component
class GQLTypeIndicatorView(
    private val indicatorCategory: GQLTypeIndicatorCategory,
    private val indicatorCategoryService: IndicatorCategoryService,
    private val fieldContributors: List<GQLFieldContributor>
) : GQLType {

    override fun createType(cache: GQLTypeCache): GraphQLObjectType =
        GraphQLObjectType.newObject()
            .name(typeName)
            .description("List of categories to display for a portfolio or a list of portfolios.")
            // ID
            .stringField(IndicatorView::id, "Unique ID for this view")
            // Name
            .stringField(IndicatorView::name, "Name of the view")
            // Categories
            .field {
                it.name(IndicatorView::categories.name)
                    .description("Selected categories for this view")
                    .type(stdList(indicatorCategory.typeRef))
                    .dataFetcher { env ->
                        val view: IndicatorView = env.getSource()
                        view.categories.mapNotNull { id ->
                            indicatorCategoryService.findCategoryById(id)
                        }
                    }
            }
            // Links
            .fields(IndicatorView::class.java.graphQLFieldContributions(fieldContributors))
            //OK
            .build()

    override fun getTypeName(): String = IndicatorView::class.java.simpleName
}