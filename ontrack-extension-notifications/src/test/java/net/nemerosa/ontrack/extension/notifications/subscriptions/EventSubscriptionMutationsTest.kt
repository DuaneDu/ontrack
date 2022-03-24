package net.nemerosa.ontrack.extension.notifications.subscriptions

import graphql.schema.*
import net.nemerosa.ontrack.graphql.support.GraphQLBeanConverter
import net.nemerosa.ontrack.test.assertIs
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class EventSubscriptionMutationsTest {

    @Test
    fun `SubscribeToEventsInput input type`() {
        val dictionary = mutableSetOf<GraphQLType>()
        val type = GraphQLBeanConverter.asInputType(SubscribeToEventsInput::class, dictionary)

        assertIs<GraphQLInputObjectType>(type) { objectType ->
            assertNotNull(objectType.getField("events")) { eventsField ->
                assertIs<GraphQLNonNull>(eventsField.type) { eventsNotNull ->
                    assertIs<GraphQLList>(eventsNotNull.wrappedType) { eventsList ->
                        assertIs<GraphQLNonNull>(eventsList.wrappedType) { eventsItemNotNull ->
                            assertIs<GraphQLScalarType>(eventsItemNotNull.wrappedType) { eventsItem ->
                                assertEquals("String", eventsItem.name)
                            }
                        }
                    }
                }
            }
        }

        assertNotNull(dictionary.find { it is GraphQLInputObjectType && it.name == "EventSubscriptionEntityInput" },
            "EventSubscriptionEntityInput input type has been created")
        assertNotNull(dictionary.find { it is GraphQLInputObjectType && it.name == "EventSubscriptionChannelInput" },
            "EventSubscriptionChannelInput input type has been created")
    }

    @Test
    fun `EventSubscriptionEntityInput input type`() {
        val type = GraphQLBeanConverter.asInputType(EventSubscriptionEntityInput::class, mutableSetOf())
        assertIs<GraphQLInputObjectType>(type) { input ->
            assertNotNull(input.getField("type")) { typeField ->
                assertIs<GraphQLNonNull>(typeField.type) { typeNotNullType ->
                    assertIs<GraphQLTypeReference>(typeNotNullType.wrappedType) { typeType ->
                        assertEquals("ProjectEntityType", typeType.name)
                    }
                }
            }

        }
    }

}