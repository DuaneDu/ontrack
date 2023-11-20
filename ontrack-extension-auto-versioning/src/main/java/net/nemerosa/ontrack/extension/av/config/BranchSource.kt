package net.nemerosa.ontrack.extension.av.config

import net.nemerosa.ontrack.model.structure.Branch
import net.nemerosa.ontrack.model.structure.Project

/**
 * Mechanism to get the last source branch given a configuration and a target branch
 * for the auto versioning.
 */
interface BranchSource {

    /**
     * Gets the last source branch for the auto versioning.
     *
     * @param config Configuration for this branch source
     * @param project Source project
     * @param targetBranch Target branch for the auto versioning
     */
    fun getLatestBranch(config: String?, project: Project, targetBranch: Branch): Branch?

    /**
     * Identifier for this branch source
     */
    val id: String

}