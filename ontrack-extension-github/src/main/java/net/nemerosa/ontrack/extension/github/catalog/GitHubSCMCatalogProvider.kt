package net.nemerosa.ontrack.extension.github.catalog

import net.nemerosa.ontrack.extension.github.client.OntrackGitHubClient
import net.nemerosa.ontrack.extension.github.client.OntrackGitHubClientFactory
import net.nemerosa.ontrack.extension.github.model.GitHubEngineConfiguration
import net.nemerosa.ontrack.extension.github.property.GitHubProjectConfigurationProperty
import net.nemerosa.ontrack.extension.github.property.GitHubProjectConfigurationPropertyType
import net.nemerosa.ontrack.extension.github.service.GitHubConfigurationService
import net.nemerosa.ontrack.extension.scm.catalog.SCMCatalogEntry
import net.nemerosa.ontrack.extension.scm.catalog.SCMCatalogProvider
import net.nemerosa.ontrack.extension.scm.catalog.SCMCatalogSource
import net.nemerosa.ontrack.extension.scm.catalog.SCMCatalogTeam
import net.nemerosa.ontrack.model.settings.CachedSettingsService
import net.nemerosa.ontrack.model.structure.Project
import net.nemerosa.ontrack.model.structure.PropertyService
import org.springframework.stereotype.Component

@Component
class GitHubSCMCatalogProvider(
    private val cachedSettingsService: CachedSettingsService,
    private val gitHubConfigurationService: GitHubConfigurationService,
    private val gitHubClientFactory: OntrackGitHubClientFactory,
    private val propertyService: PropertyService
) : SCMCatalogProvider {

    override val id: String = "github"

    override val entries: List<SCMCatalogSource>
        get() {
            val settings = cachedSettingsService.getCachedSettings(GitHubSCMCatalogSettings::class.java)
            return gitHubConfigurationService.configurations.flatMap { config ->
                getConfigEntries(settings, config)
            }
        }

    private fun getConfigEntries(
        settings: GitHubSCMCatalogSettings,
        config: GitHubEngineConfiguration
    ): Iterable<SCMCatalogSource> {
        val client = gitHubClientFactory.create(config)
        return client.organizations.filter { it.login in settings.orgs }.flatMap {
            client.findRepositoriesByOrganization(it.login).map { name -> "${it.login}/$name" }
        }.map { repo ->
            SCMCatalogSource(
                config = config.name,
                repository = repo,
                repositoryPage = "${config.url}/$repo",
                lastActivity = client.getRepositoryLastModified(repo),
                teams = getTeams(client, repo)
            )
        }
    }

    private fun getTeams(client: OntrackGitHubClient, repo: String): List<SCMCatalogTeam>? =
        try {
            client.getRepositoryTeams(repo)?.map {
                SCMCatalogTeam(
                    id = it.slug,
                    name = it.name,
                    description = it.description,
                    url = it.html_url,
                    role = it.permission
                )
            }
        } catch (_: Exception) {
            null
        }

    override fun matches(entry: SCMCatalogEntry, project: Project): Boolean {
        val property: GitHubProjectConfigurationProperty? =
            propertyService.getProperty(project, GitHubProjectConfigurationPropertyType::class.java).value
        return if (property != null) {
            property.configuration.name == entry.config &&
                    property.repository == entry.repository
        } else {
            false
        }
    }

}