package net.nemerosa.ontrack.extension.scm.changelog

import net.nemerosa.ontrack.extension.issues.model.ConfiguredIssueService
import net.nemerosa.ontrack.extension.issues.model.Issue
import net.nemerosa.ontrack.extension.scm.service.SCMDetector
import net.nemerosa.ontrack.model.structure.Build
import net.nemerosa.ontrack.model.structure.StructureService
import org.springframework.stereotype.Service

@Service
class SCMChangeLogServiceImpl(
    private val scmDetector: SCMDetector,
    private val structureService: StructureService,
) : SCMChangeLogService {

    override suspend fun getChangeLogBoundaries(
        from: Build,
        to: Build,
        dependencies: List<DependencyLink>
    ): Pair<Build, Build>? {
        if (from.project.id() != to.project.id()) {
            throw SCMChangeLogNotSameProjectException()
        } else if (dependencies.isEmpty()) {
            return from to to
        } else {
            val project = dependencies.first()
            val restProjects = dependencies.drop(1)

            // Gets the link to the dependency
            val linkedFrom = structureService.getQualifiedBuildsUsedBy(from, size = 1) {
                it.build.branch.project.name == project.project &&
                        it.qualifier == project.qualifier
            }.pageItems.firstOrNull()?.build
            val linkedTo = structureService.getQualifiedBuildsUsedBy(to, size = 1) {
                it.build.branch.project.name == project.project &&
                        it.qualifier == project.qualifier
            }.pageItems.firstOrNull()?.build

            // If one of the links is absent, giving up
            if (linkedFrom == null || linkedTo == null) {
                return null
            } else {
                return getChangeLogBoundaries(linkedFrom, linkedTo, restProjects)
            }
        }
    }

    override suspend fun getChangeLog(from: Build, to: Build, dependencies: List<DependencyLink>): SCMChangeLog? {
        val boundaries = getChangeLogBoundaries(from, to, dependencies)
        return boundaries?.let { (f, t) ->
            computeChangeLog(f, t)
        }
    }

    private suspend fun computeChangeLog(from: Build, to: Build): SCMChangeLog? {

        val scm = scmDetector.getSCM(from.project)
        if (scm == null || scm !is SCMChangeLogEnabled) {
            return null
        }

        // Gets the two boundaries
        val fromCommit = scm.getBuildCommit(from)
        val toCommit = scm.getBuildCommit(to)
        if (fromCommit.isNullOrBlank() || toCommit.isNullOrBlank()) {
            throw SCMChangeLogNoCommitException()
        }

        // Getting the list of commits
        val commits = scm.getCommits(fromCommit, toCommit)
            // ... sorted from the newest to the oldest
            .sortedByDescending { it.timestamp }

        // Decoration of the commits
        val decoratedCommits = commits.map { commit ->
            SCMDecoratedCommit(
                project = from.project,
                commit = commit,
            )
        }

        // Getting the issue service
        val configuredIssueService: ConfiguredIssueService? = scm.getConfiguredIssueService()
        val issuesChangeLog: SCMChangeLogIssues? = if (configuredIssueService != null) {
            // Index of issues
            val index = mutableMapOf<String, Issue>()
            // For all commits in this commit log
            commits.forEach { commit ->
                val keys = configuredIssueService.extractIssueKeysFromMessage(commit.message)
                keys.forEach { key ->
                    val exisingIssue = index[key]
                    if (exisingIssue == null) {
                        val issue = configuredIssueService.getIssue(key)
                        if (issue != null) {
                            index[key] = issue
                        }
                    }
                }
            }
            // OK
            val issues = index.values.sortedBy { it.key }
            val issueServiceConfiguration = configuredIssueService.issueServiceConfigurationRepresentation
            SCMChangeLogIssues(
                issueServiceConfiguration,
                issues
            )
        } else {
            null
        }

        // OK
        return SCMChangeLog(
            from = from,
            to = to,
            fromCommit = fromCommit,
            toCommit = toCommit,
            commits = decoratedCommits,
            issues = issuesChangeLog,
        )
    }

}