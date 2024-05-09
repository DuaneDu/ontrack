package net.nemerosa.ontrack.extension.workflows.engine

import net.nemerosa.ontrack.model.pagination.PaginatedList
import net.nemerosa.ontrack.model.support.StorageService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
@ConditionalOnProperty(
    prefix = "net.nemerosa.ontrack.extension.workflows",
    name = ["store"],
    havingValue = "database",
    matchIfMissing = true,
)
@Transactional(propagation = Propagation.REQUIRES_NEW)
class DatabaseWorkflowInstanceStore(
    private val storageService: StorageService,
) : WorkflowInstanceStore {

    companion object {
        private val STORE = WorkflowInstance::class.java.name
    }

    override fun store(instance: WorkflowInstance): WorkflowInstance {
        storageService.store(STORE, instance.id, instance)
        return instance
    }

    override fun findById(id: String): WorkflowInstance? =
        storageService.find(STORE, id, WorkflowInstance::class)

    override fun findByFilter(workflowInstanceFilter: WorkflowInstanceFilter): PaginatedList<WorkflowInstance> {
        return storageService.paginatedFilter(
            store = STORE,
            type = WorkflowInstance::class,
            offset = workflowInstanceFilter.offset,
            size = workflowInstanceFilter.size,
            // TODO Filters
            // TODO Order by start date desc
        )
    }

    override fun clearAll() {
        storageService.clear(STORE)
    }
}