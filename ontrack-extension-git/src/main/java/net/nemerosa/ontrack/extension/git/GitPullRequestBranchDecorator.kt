package net.nemerosa.ontrack.extension.git

import net.nemerosa.ontrack.extension.api.DecorationExtension
import net.nemerosa.ontrack.extension.git.model.GitPullRequest
import net.nemerosa.ontrack.extension.git.service.GitService
import net.nemerosa.ontrack.extension.support.AbstractExtension
import net.nemerosa.ontrack.model.structure.Branch
import net.nemerosa.ontrack.model.structure.Decoration
import net.nemerosa.ontrack.model.structure.ProjectEntity
import net.nemerosa.ontrack.model.structure.ProjectEntityType
import org.springframework.stereotype.Component
import java.util.*

@Component
class GitPullRequestBranchDecorator(
    extensionFeature: GitExtensionFeature,
    private val gitService: GitService,
) : AbstractExtension(extensionFeature), DecorationExtension<GitPullRequestDecoration> {

    override fun getDecorations(entity: ProjectEntity): List<Decoration<GitPullRequestDecoration>> =
        if (entity is Branch && gitService.isBranchAPullRequest(entity)) {
            val pr = gitService.getBranchAsPullRequest(entity)
            pr?.let {
                listOf(
                    Decoration.of(
                        this,
                        GitPullRequestDecoration(it)
                    )
                )
            } ?: emptyList()
        } else {
            emptyList()
        }

    override fun getScope(): EnumSet<ProjectEntityType> = EnumSet.of(ProjectEntityType.BRANCH)
}

class GitPullRequestDecoration(
    val isValid: Boolean,
    val key: String,
    val source: String,
    val target: String,
    val title: String,
    val status: String,
    val url: String,
) {

    constructor(pr: GitPullRequest) : this(
        isValid = pr.isValid,
        key = pr.key,
        source = GitPullRequest.simpleBranchName(pr.source),
        target = GitPullRequest.simpleBranchName(pr.target),
        title = pr.title,
        status = pr.status,
        url = pr.url
    )

}