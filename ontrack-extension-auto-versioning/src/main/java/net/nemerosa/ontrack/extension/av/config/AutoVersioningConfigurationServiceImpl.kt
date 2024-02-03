package net.nemerosa.ontrack.extension.av.config

import net.nemerosa.ontrack.common.syncForward
import net.nemerosa.ontrack.extension.notifications.subscriptions.EventSubscription
import net.nemerosa.ontrack.extension.notifications.subscriptions.EventSubscriptionFilter
import net.nemerosa.ontrack.extension.notifications.subscriptions.EventSubscriptionService
import net.nemerosa.ontrack.model.security.ProjectConfig
import net.nemerosa.ontrack.model.security.SecurityService
import net.nemerosa.ontrack.model.structure.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AutoVersioningConfigurationServiceImpl(
    private val securityService: SecurityService,
    private val entityDataService: EntityDataService,
    private val regexBranchSource: RegexBranchSource,
    private val structureService: StructureService,
    private val eventSubscriptionService: EventSubscriptionService,
    private val autoVersioningBranchExpressionService: AutoVersioningBranchExpressionService,
) : AutoVersioningConfigurationService {

    override fun setupAutoVersioning(branch: Branch, config: AutoVersioningConfig?) {
        securityService.checkProjectFunction(branch, ProjectConfig::class.java)
        if (config != null) {
            securityService.asAdmin {
                setupNotifications(branch, config)
            }
            entityDataService.store(branch, STORE, config)
        } else {
            securityService.asAdmin {
                setupNotifications(branch, null)
            }
            entityDataService.delete(branch, STORE)
        }
    }

    private data class AVConfigSubscription(
        val source: AutoVersioningSourceConfig,
        val notification: AutoVersioningNotification,
    ) {
        fun toEventSubscription(branch: Branch) = EventSubscription(
            projectEntity = branch,
            events = AutoVersioningNotificationScope.toEvents(notification.scope),
            keywords = "${source.sourceProject} ${branch.project.name} ${branch.name}",
            channel = notification.channel,
            channelConfig = notification.config,
            disabled = false,
            contentTemplate = notification.notificationTemplate,
            origin = AutoVersioningNotification.ORIGIN,
        )
    }

    private fun setupNotifications(branch: Branch, config: AutoVersioningConfig?) {
        if (config == null) {
            eventSubscriptionService.deleteSubscriptionsByEntity(branch)
        } else {
            // Existing subscriptions
            val existingSubscriptions = eventSubscriptionService.filterSubscriptions(
                EventSubscriptionFilter(
                    size = Int.MAX_VALUE,
                    entity = branch.toProjectEntityID(),
                    origin = AutoVersioningNotification.ORIGIN,
                )
            ).pageItems
            // New subscriptions
            val newSubscriptions = config.configurations.flatMap { source ->
                source.notifications?.map { notification ->
                    AVConfigSubscription(source, notification)
                } ?: emptyList()
            }

            // Subscription
            fun subscribe(item: AVConfigSubscription) {
                eventSubscriptionService.subscribe(item.toEventSubscription(branch))
            }

            // Sync between current subscriptions & configured subscriptions
            syncForward(
                from = newSubscriptions,
                to = existingSubscriptions,
            ) {
                equality { item, existing ->
                    item.toEventSubscription(branch) == existing.data
                }
                onCreation { item ->
                    subscribe(item)
                }
                onModification { item, _ ->
                    subscribe(item)
                }
                onDeletion { existing ->
                    eventSubscriptionService.deleteSubscriptionById(branch, existing.id)
                }
            }
        }
    }

    override fun getAutoVersioning(branch: Branch): AutoVersioningConfig? =
        entityDataService.retrieve(branch, STORE, AutoVersioningConfig::class.java)?.postDeserialize()

    override fun getAutoVersioningBetween(parent: Branch, dependency: Branch): AutoVersioningSourceConfig? {
        // Gets the AV config of the parent branch
        // or returns null if not set at all
        val parentAVConfig = getAutoVersioning(parent) ?: return null
        // Gets the configurations matching the dependency project
        val dependencyConfigs = parentAVConfig.configurations.filter {
            it.sourceProject == dependency.project.name
        }
        // Among these project matching configurations, retains the one
        // where the dependency branch matches the last eligible branch
        val branchMatchingConfigs = dependencyConfigs.filter {
            val latestBranch = getLatestBranch(parent, dependency.project, it)
            latestBranch?.id == dependency.id
        }
        // Takes the first one
        return branchMatchingConfigs.firstOrNull()
    }

    override fun getBranchesConfiguredFor(project: String, promotion: String): List<Branch> =
        entityDataService.findEntities(
            type = ProjectEntityType.BRANCH,
            key = STORE,
            jsonQuery = """JSON_VALUE::jsonb->'configurations' @> '[{"sourceProject":"$project","sourcePromotion":"$promotion"}]'::jsonb""",
            jsonQueryParameters = emptyMap(),
        ).map {
            structureService.getBranch(ID.of(it.id))
        }

    override fun getLatestBranch(
        eligibleTargetBranch: Branch,
        project: Project,
        config: AutoVersioningSourceConfig
    ): Branch? =
        if (config.sourceBranch.startsWith("&")) {
            val avBranchExpression = config.sourceBranch.drop(1)
            autoVersioningBranchExpressionService.getLatestBranch(eligibleTargetBranch, project, avBranchExpression)
        } else {
            regexBranchSource.getLatestBranch(
                config.sourceBranch,
                project,
                eligibleTargetBranch
            )
        }

    companion object {
        private val STORE: String = AutoVersioningConfig::class.java.name
    }
}