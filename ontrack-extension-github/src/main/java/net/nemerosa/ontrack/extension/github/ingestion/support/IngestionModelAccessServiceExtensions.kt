package net.nemerosa.ontrack.extension.github.ingestion.support

import net.nemerosa.ontrack.extension.github.ingestion.processing.model.Repository

fun IngestionModelAccessService.getOrCreateBranch(
    repository: Repository,
    configuration: String?,
    headBranch: String,
    baseBranch: String?,
) = getOrCreateBranch(
    project = getOrCreateProject(repository, configuration),
    headBranch = headBranch,
    baseBranch = baseBranch
)
