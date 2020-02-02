package net.nemerosa.ontrack.extension.git.service

import net.nemerosa.ontrack.extension.api.model.BuildDiffRequest
import net.nemerosa.ontrack.extension.git.model.*
import net.nemerosa.ontrack.extension.git.property.GitBranchConfigurationProperty
import net.nemerosa.ontrack.extension.scm.service.SCMService
import net.nemerosa.ontrack.git.GitRepositoryClient
import net.nemerosa.ontrack.git.model.GitCommit
import net.nemerosa.ontrack.job.JobCategory
import net.nemerosa.ontrack.job.JobRunListener
import net.nemerosa.ontrack.model.Ack
import net.nemerosa.ontrack.model.structure.Branch
import net.nemerosa.ontrack.model.structure.Build
import net.nemerosa.ontrack.model.structure.ID
import net.nemerosa.ontrack.model.structure.Project
import java.util.concurrent.Future
import java.util.function.BiConsumer

val GIT_JOB_CATEGORY = JobCategory.of("git").withName("Git")

interface GitService : SCMService {

    /**
     * Tests if a branch is correctly configured for Git.
     */
    fun isBranchConfiguredForGit(branch: Branch): Boolean

    /**
     * Gets the configuration for a project.
     */
    fun getProjectConfiguration(project: Project): GitConfiguration?

    /**
     * Gets the configuration for a branch
     *
     * @param branch Branch to check
     * @return Configuration.
     */
    fun getBranchConfiguration(branch: Branch): GitBranchConfiguration?

    /**
     * Gets an Ontrack branch for a given Git branch inside an Ontrack project.
     *
     * @param project Project where to look for the branch
     * @param branchName Name of the Git branch
     * @return Ontrack branch or `null` if not found
     */
    fun findBranchWithGitBranch(project: Project, branchName: String): Branch?

    /**
     * Launches the build/tag synchronisation for a branch
     */
    fun launchBuildSync(branchId: ID, synchronous: Boolean): Future<*>?

    /**
     * Change log
     */
    fun changeLog(request: BuildDiffRequest): GitChangeLog

    /**
     * Change log commits
     */
    fun getChangeLogCommits(changeLog: GitChangeLog): GitChangeLogCommits

    /**
     * Change log issues IDs
     */
    fun getChangeLogIssuesIds(changeLog: GitChangeLog): List<String>

    /**
     * Change log issues
     */
    fun getChangeLogIssues(changeLog: GitChangeLog): GitChangeLogIssues

    /**
     * Change log files
     */
    fun getChangeLogFiles(changeLog: GitChangeLog): GitChangeLogFiles

    /**
     * Loops over each correctly configured project.
     */
    fun forEachConfiguredProject(consumer: BiConsumer<Project, GitConfiguration>)

    /**
     * Loops over each correctly configured branch. Branch template definitions are excluded.
     */
    fun forEachConfiguredBranch(consumer: BiConsumer<Branch, GitBranchConfiguration>)


    /**
     * Loops over each correctly configured branch in a project. Branch template definitions are excluded.
     */
    fun forEachConfiguredBranchInProject(project: Project, consumer: (Branch, GitBranchConfiguration) -> Unit)

    /**
     * Gets information about an issue in a Git-configured project
     *
     * @param projectId ID of the project
     * @param key Display key of the issue
     * @return Issue & commit information about the issue in the project if available
     */
    fun getIssueProjectInfo(projectId: ID, key: String): OntrackGitIssueInfo?

    /**
     * Looks up a commit in the given `configuration`.
     *
     * @param id Commit long or short ID
     * @return The content of a commit if it exists, `null` otherwise.
     */
    fun lookupCommit(configuration: GitConfiguration, id: String): GitCommit?

    /**
     * Gets information about a commit in a Git-configured project.
     */
    fun getCommitProjectInfo(projectId: ID, commit: String): OntrackGitCommitInfo

    /**
     * Gets the list of remote branches, as defined under `ref/heads`.
     */
    fun getRemoteBranches(gitConfiguration: GitConfiguration): List<String>

    /**
     * Gets a diff on a list of file changes, filtering the changes using ANT-like patterns
     */
    fun diff(changeLog: GitChangeLog, patterns: List<String>): String

    /**
     * Synchronises the Git repository attached to this project.
     *
     * @param project Project
     * @param request Sync request
     * @return Result. The synchronisation will occur asynchronously, but the acknowledgment returns
     * if the project did contain a Git configuration or not.
     */
    fun projectSync(project: Project, request: GitSynchronisationRequest): Ack

    /**
     * Synchronises the Git repository attached to this configuration.
     *
     * @param gitConfiguration Configuration to sync
     * @param request          Sync request
     * @return Result. The synchronisation will occur asynchronously, but the acknowledgment returns if the
     * synchronisation was actually launched.
     */
    fun sync(gitConfiguration: GitConfiguration, request: GitSynchronisationRequest): Future<*>?

    /**
     * Gets the Git synchronisation information.
     *
     * @param project Project configured for Git
     * @return Synchronisation information
     */
    fun getProjectGitSyncInfo(project: Project): GitSynchronisationInfo

    fun scheduleGitBuildSync(branch: Branch, property: GitBranchConfigurationProperty)

    fun unscheduleGitBuildSync(branch: Branch, property: GitBranchConfigurationProperty)

    /**
     * Checks the log history and returns `true` if the token can be found.
     *
     * @param gitConfiguration Git configuration
     * @param token               Expression to be searched for
     * @return Result of the search
     */
    fun isPatternFound(gitConfiguration: GitConfiguration, token: String): Boolean

    /**
     * Gets the [GitCommit] associated to a build
     *
     * @param build Build to get the information for
     * @return Commit information or `null` if not found
     */
    fun getCommitForBuild(build: Build): IndexableGitCommit?

    /**
     * Sets the [GitCommit] information for a build.
     *
     * @param build Build to set the information for
     * @param commit Commit information
     */
    fun setCommitForBuild(build: Build, commit: IndexableGitCommit)

    /**
     * Collects and stores the [IndexableGitCommit]s for all builds of a branch.
     */
    fun collectIndexableGitCommitForBranch(branch: Branch, overrides: Boolean = true)

    /**
     * Collects and stores the [IndexableGitCommit]s for all builds of a branch.
     *
     * This is the optimized version for jobs running in the background.
     */
    fun collectIndexableGitCommitForBranch(branch: Branch,
                                           client: GitRepositoryClient,
                                           config: GitBranchConfiguration,
                                           overrides: Boolean,
                                           listener: JobRunListener)

    /**
     * Collects and stores the [IndexableGitCommit]s one build.
     */
    fun collectIndexableGitCommitForBuild(build: Build)

    /**
     * Loops over the commits of a configuration
     */
    fun forEachCommit(gitConfiguration: GitConfiguration, code: (GitCommit) -> Unit)
}
