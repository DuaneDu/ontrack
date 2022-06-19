package net.nemerosa.ontrack.extension.av.audit

import net.nemerosa.ontrack.extension.av.dispatcher.AutoVersioningOrder
import net.nemerosa.ontrack.model.structure.Branch
import java.time.LocalDateTime

/**
 * Storage abstraction for the auto versioning events.
 */
interface AutoVersioningAuditStore {

    /**
     * Creates a new entry
     *
     * @param order Audit entry to create
     */
    fun create(order: AutoVersioningOrder, routing: String)

    /**
     * Adds a new state to an auto versioning process.
     */
    fun addState(targetBranch: Branch, uuid: String, queue: String? = null, state: AutoVersioningAuditState, vararg data: Pair<String, String>)

    fun findByUUID(targetBranch: Branch, uuid: String): AutoVersioningAuditEntry?
    fun findByFilter(filter: AutoVersioningAuditQueryFilter): List<AutoVersioningAuditEntry>
    fun countByFilter(filter: AutoVersioningAuditQueryFilter): Int

    /**
     * Removes all audit entries before [retentionDate].
     *
     * @param retentionDate All items before this date will be removed
     * @param nonRunningOnly Criteria on running state
     * @return Number of entries having been removed
     */
    fun removeAllBefore(retentionDate: LocalDateTime, nonRunningOnly: Boolean): Int

    /**
     * Removes all existing audit entries
     */
    fun removeAll()
}