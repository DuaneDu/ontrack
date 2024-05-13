package net.nemerosa.ontrack.kdsl.acceptance.tests.workflows

import net.nemerosa.ontrack.kdsl.acceptance.tests.notifications.AbstractACCDSLNotificationsTestSupport
import net.nemerosa.ontrack.kdsl.acceptance.tests.support.waitUntil
import net.nemerosa.ontrack.kdsl.spec.extension.workflows.WorkflowInstance
import net.nemerosa.ontrack.kdsl.spec.extension.workflows.WorkflowInstanceStatus
import net.nemerosa.ontrack.kdsl.spec.extension.workflows.workflows
import kotlin.test.fail

abstract class AbstractACCDSLWorkflowsTestSupport : AbstractACCDSLNotificationsTestSupport() {

    protected fun waitUntilWorkflowFinished(
        instanceId: String,
        returnInstanceOnError: Boolean = false,
    ): WorkflowInstance {
        waitUntil(
            timeout = 30_000L,
            interval = 500L,
        ) {
            val instance = ontrack.workflows.workflowInstance(instanceId)
            instance != null && instance.finished
        }
        // Getting the final errors
        val instance = ontrack.workflows.workflowInstance(instanceId)
            ?: fail("Could not get the workflow instance")
        if (!returnInstanceOnError && instance.status == WorkflowInstanceStatus.ERROR) {
            // Displaying the errors
            instance.nodesExecutions.forEach { node ->
                println("Node: ${node.id}")
                println("  Status: ${node.status}")
                println("  Output: ${node.output}")
                println("  Error: ${node.error}")
            }
            // Failing
            fail("Workflow failed in error. See errors above.")
        }
        // OK, returning the final state of the instance
        return instance
    }

}