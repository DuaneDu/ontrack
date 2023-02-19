package net.nemerosa.ontrack.extension.git.model

import net.nemerosa.ontrack.extension.scm.model.SCMChangeLogCommit
import net.nemerosa.ontrack.git.model.GitCommit
import net.nemerosa.ontrack.graphql.support.ListRef
import net.nemerosa.ontrack.graphql.support.TypeRef
import net.nemerosa.ontrack.model.structure.Build
import net.nemerosa.ontrack.model.structure.PromotionRun

/**
 * @param build Build attached to the commit (if any or requested)
 */
data class GitUICommit(
    val commit: GitCommit,
    val annotatedMessage: String,
    val fullAnnotatedMessage: String,
    override val link: String,
    @TypeRef
    val build: Build? = null,
    @ListRef
    val promotions: List<PromotionRun>? = null,
    @ListRef
    val dependencies: List<Build>? = null,
) : SCMChangeLogCommit {

    override val message = commit.fullMessage

    override val id: String = commit.id

    override val author = commit.author.name
    override val authorEmail = commit.author.email

    override val timestamp = commit.commitTime

    override val formattedMessage = fullAnnotatedMessage

    override val shortId = commit.shortId
}
