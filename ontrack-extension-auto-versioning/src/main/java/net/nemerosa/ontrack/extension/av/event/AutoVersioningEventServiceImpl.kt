package net.nemerosa.ontrack.extension.av.event

import net.nemerosa.ontrack.common.getOrNull
import net.nemerosa.ontrack.extension.av.dispatcher.AutoVersioningOrder
import net.nemerosa.ontrack.extension.scm.service.SCMPullRequest
import net.nemerosa.ontrack.model.events.*
import net.nemerosa.ontrack.model.exceptions.ProjectNotFoundException
import net.nemerosa.ontrack.model.structure.Project
import net.nemerosa.ontrack.model.structure.StructureService
import net.nemerosa.ontrack.model.support.StartupService
import org.springframework.stereotype.Service

@Service
class AutoVersioningEventServiceImpl(
    private val eventFactory: EventFactory,
    private val eventPostService: EventPostService,
    private val structureService: StructureService,
) : AutoVersioningEventService, StartupService {

    override fun sendError(order: AutoVersioningOrder, message: String, error: Exception) {
        eventPostService.post(
            error(order, message, error)
        )
    }

    override fun sendPRMergeTimeoutError(order: AutoVersioningOrder, pr: SCMPullRequest) {
        eventPostService.post(
            prMergeTimeoutError(order, pr)
        )
    }

    override fun sendSuccess(order: AutoVersioningOrder, message: String, pr: SCMPullRequest) {
        eventPostService.post(
            success(order, message, pr)
        )
    }

    override fun getName(): String = "Registration of auto versioning events"

    override fun startupOrder(): Int = StartupService.JOB_REGISTRATION

    override fun start() {
        eventFactory.register(AUTO_VERSIONING_SUCCESS)
        eventFactory.register(AUTO_VERSIONING_ERROR)
    }

    internal fun success(
        order: AutoVersioningOrder,
        message: String,
        pr: SCMPullRequest,
    ): Event =
        Event.of(AUTO_VERSIONING_SUCCESS)
            .withRef(order.branch)
            .withProject(sourceProject(order))
            .with("version", order.targetVersion)
            .with("message", message)
            .with("pr-name", pr.name)
            .with("pr-link", pr.link)
            .get()

    internal fun error(
        order: AutoVersioningOrder,
        message: String,
        error: Exception,
    ): Event =
        Event.of(AUTO_VERSIONING_ERROR)
            .withRef(order.branch)
            .withProject(sourceProject(order))
            .with("version", order.targetVersion)
            .with("message", message)
            .with("error", error.message)
            .get()

    internal fun prMergeTimeoutError(
        order: AutoVersioningOrder,
        pr: SCMPullRequest,
    ): Event =
        Event.of(AUTO_VERSIONING_PR_MERGE_TIMEOUT_ERROR)
            .withRef(order.branch)
            .withProject(sourceProject(order))
            .with("version", order.targetVersion)
            .with("pr-name", pr.name)
            .with("pr-link", pr.link)
            .get()


    private fun sourceProject(order: AutoVersioningOrder): Project =
        structureService.findProjectByName(order.sourceProject).getOrNull()
            ?: throw ProjectNotFoundException(order.sourceProject)

    companion object {

        private val AUTO_VERSIONING_SUCCESS: EventType = SimpleEventType.of(
            "auto-versioning-success",
            """
                Auto versioning of ${'$'}{REF} for dependency ${'$'}{PROJECT} version "${'$'}{:version}" has been done.
                
                Pull request ${'$'}{:pr-name:pr-link}
            """.trimIndent()
        )

        private val AUTO_VERSIONING_ERROR: EventType = SimpleEventType.of(
            "auto-versioning-error",
            """
                Auto versioning of ${'$'}{REF} for dependency ${'$'}{PROJECT} version "${'$'}{:version}" has failed.
                
                ${'$'}{:message}
                
                Error: ${'$'}{:error}
            """.trimIndent()
        )

        private val AUTO_VERSIONING_PR_MERGE_TIMEOUT_ERROR: EventType = SimpleEventType.of(
            "auto-versioning-pr-merge-timeout-error",
            """
                Auto versioning of ${'$'}{REF} for dependency ${'$'}{PROJECT} version "${'$'}{:version}" has failed.
                
                Timeout while waiting for the PR to be ready to be merged.
                
                PR: ${'$'}{:pr-name:pr-link}
            """.trimIndent()
        )

    }

}