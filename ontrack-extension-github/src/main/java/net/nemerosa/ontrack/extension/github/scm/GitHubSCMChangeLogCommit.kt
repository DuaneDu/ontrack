package net.nemerosa.ontrack.extension.github.scm

import net.nemerosa.ontrack.extension.github.client.GitHubAuthor
import net.nemerosa.ontrack.extension.github.client.GitHubCommit
import net.nemerosa.ontrack.extension.scm.changelog.SCMChangeLogCommit
import java.time.LocalDateTime

class GitHubSCMChangeLogCommit(commit: GitHubCommit) : SCMChangeLogCommit {

    override val id: String = commit.sha

    override val shortId: String = id.take(7)

    private val actualAuthor: GitHubAuthor = commit.commit.author ?: commit.commit.committer

    override val author: String = actualAuthor.name

    override val authorEmail: String = actualAuthor.email

    override val timestamp: LocalDateTime = actualAuthor.date

    override val message: String = commit.commit.message

    override val link: String = commit.url
}