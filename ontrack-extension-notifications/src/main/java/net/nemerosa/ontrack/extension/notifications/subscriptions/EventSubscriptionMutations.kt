package net.nemerosa.ontrack.extension.notifications.subscriptions

import net.nemerosa.ontrack.graphql.schema.Mutation
import net.nemerosa.ontrack.graphql.support.ListRef
import net.nemerosa.ontrack.graphql.support.TypeRef
import net.nemerosa.ontrack.graphql.support.TypedMutationProvider
import net.nemerosa.ontrack.model.annotations.APIDescription
import net.nemerosa.ontrack.model.structure.ID
import net.nemerosa.ontrack.model.structure.ProjectEntityType
import net.nemerosa.ontrack.model.structure.StructureService
import org.springframework.stereotype.Component

@Component
class EventSubscriptionMutations(
    private val eventSubscriptionService: EventSubscriptionService,
    private val structureService: StructureService,
) : TypedMutationProvider() {

    override val mutations: List<Mutation> = listOf(
        simpleMutation(
            name = "subscribeToEvents",
            description = "Creates a subscription to a list of events",
            input = SubscribeToEventsInput::class,
            outputName = "subscription",
            outputDescription = "Saved subscription",
            outputType = EventSubscriptionPayload::class,
        ) { input ->
            val projectEntity = input.projectEntity?.run {
                type.getEntityFn(structureService).apply(ID.of(id))
            }
            val record = eventSubscriptionService.subscribe(
                EventSubscription(
                    channels = input.channels.toSet(),
                    events = input.events.toSet(),
                    projectEntity = projectEntity,
                )
            )
            EventSubscriptionPayload(
                id = record.id,
                channels = record.data.channels.toList(),
                events = record.data.events.toList(),
            )
        }
    )
}

@APIDescription("Subscription to events")
data class SubscribeToEventsInput(
    @APIDescription("Target project entity (null for global events)")
    @TypeRef(embedded = true)
    val projectEntity: EventSubscriptionEntityInput?,
    @APIDescription("Channels to send this event to")
    @ListRef(embedded = true, suffix = "Input")
    val channels: List<EventSubscriptionChannel>,
    @APIDescription("List of events types to subscribe to")
    @ListRef
    val events: List<String>,
)

@APIDescription("Target entity for a subscription")
data class EventSubscriptionEntityInput(
    @APIDescription("Project entity type")
    val type: ProjectEntityType,
    @APIDescription("Project entity ID")
    val id: Int,
)

@APIDescription("Event subscription record")
data class EventSubscriptionPayload(
    @APIDescription("Unique ID for this subscription")
    val id: String,
    @APIDescription("Channels to send this event to")
    val channels: List<EventSubscriptionChannel>,
    @APIDescription("List of events types to subscribe to")
    val events: List<String>,
)
