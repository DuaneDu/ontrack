package net.nemerosa.ontrack.graphql.schema

import graphql.Scalars.GraphQLString
import graphql.schema.GraphQLObjectType
import net.nemerosa.ontrack.graphql.support.enumField
import net.nemerosa.ontrack.graphql.support.idField
import net.nemerosa.ontrack.graphql.support.listType
import net.nemerosa.ontrack.graphql.support.stringField
import net.nemerosa.ontrack.model.annotations.getPropertyDescription
import net.nemerosa.ontrack.model.annotations.getPropertyName
import net.nemerosa.ontrack.model.structure.ValidationStampFilter
import org.springframework.stereotype.Component

@Component
class GQLTypeValidationStampFilter : GQLType {

    override fun getTypeName(): String = ValidationStampFilter::class.java.simpleName

    override fun createType(cache: GQLTypeCache): GraphQLObjectType = GraphQLObjectType.newObject()
            .name(typeName)
            .description("Validation stamp filter")
            .idField(ValidationStampFilter::id)
            .stringField(ValidationStampFilter::name)
            .field {
                it.name(getPropertyName(ValidationStampFilter::vsNames))
                        .description(getPropertyDescription(ValidationStampFilter::vsNames))
                        .type(listType(GraphQLString))
            }
            .enumField(ValidationStampFilter::scope)
            .build()
}