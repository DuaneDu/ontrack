package net.nemerosa.ontrack.extension.github.client

import net.nemerosa.ontrack.extension.github.model.GitHubIssue
import net.nemerosa.ontrack.extension.github.model.GitHubTeam
import net.nemerosa.ontrack.extension.github.model.GitHubUser
import org.eclipse.egit.github.core.client.GitHubClient
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

/**
 * Client used to connect to a GitHub engine from Ontrack.
 */
interface OntrackGitHubClient {

    /**
     * Gets an issue from a repository.
     *
     * @param repository Repository name, like `nemerosa/ontrack`
     * @param id         ID of the issue
     * @return Details about the issue
     */
    fun getIssue(repository: String, id: Int): GitHubIssue?

    /**
     * Gets the list of repositories available using this client.
     */
    val repositories: List<String>

    /**
     * Gets the list of organizations available from this client.
     */
    val organizations: List<GitHubUser>

    /**
     * Gets the list of repositories for an organization
     *
     * @param organization Organization name
     * @return List of repository names in this [organization]
     */
    fun findRepositoriesByOrganization(organization: String): List<String>

    /**
     * Gets the underlying / native GitHub client so that extensions
     * can add features.
     */
    @Deprecated("Prefer using the RestTemplate")
    fun createGitHubClient(): GitHubClient

    /**
     * Creates a [RestTemplate] for accessing GitHub.
     */
    fun createGitHubRestTemplate(): RestTemplate

    /**
     * Gets the last modified date for a given [repository][repo].
     */
    fun getRepositoryLastModified(repo: String): LocalDateTime?

    /**
     * Gets the list of teams for a repository
     *
     * @param repo Repository (org/name)
     * @return List of teams or `null` if the teams cannot be accessed
     */
    fun getRepositoryTeams(repo: String): List<GitHubTeam>?
}