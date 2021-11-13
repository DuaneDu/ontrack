package net.nemerosa.ontrack.extension.github.ingestion

import net.nemerosa.ontrack.extension.github.AbstractGitHubTestSupport
import net.nemerosa.ontrack.extension.github.ingestion.settings.GitHubIngestionSettings
import net.nemerosa.ontrack.extension.github.model.GitHubEngineConfiguration

abstract class AbstractIngestionTestSupport : AbstractGitHubTestSupport() {

    protected fun withGitHubIngestionSettings(
        @Suppress("SameParameterValue") orgProjectPrefix: Boolean? = false,
        stepExcludes: String? = GitHubIngestionSettings.DEFAULT_STEP_EXCLUDES,
        jobExcludes: String? = GitHubIngestionSettings.DEFAULT_JOB_EXCLUDES,
        issueServiceIdentifier: String? = GitHubIngestionSettings.DEFAULT_ISSUE_SERVICE_IDENTIFIER,
        indexationInterval: Int? = GitHubIngestionSettings.DEFAULT_INDEXATION_INTERVAL,
        code: () -> Unit,
    ) {
        withSettings<GitHubIngestionSettings> {
            val old = cachedSettingsService.getCachedSettings(GitHubIngestionSettings::class.java)
            val new = GitHubIngestionSettings(
                token = old.token,
                retentionDays = old.retentionDays,
                orgProjectPrefix = orgProjectPrefix ?: old.orgProjectPrefix,
                stepExcludes = stepExcludes ?: old.stepExcludes,
                jobExcludes = jobExcludes ?: old.jobExcludes,
                indexationInterval = indexationInterval ?: old.indexationInterval,
                issueServiceIdentifier = issueServiceIdentifier ?: old.issueServiceIdentifier,
            )
            asAdmin {
                settingsManagerService.saveSettings(new)
            }
            code()
        }
    }

    protected fun onlyOneGitHubConfig(): GitHubEngineConfiguration =
        asAdmin {
            // Removing all previous configuration
            noGitHubConfig()
            // Creating one config
            gitHubConfig()
        }

    protected fun severalGitHubConfigs(sameRoot: Boolean = false): GitHubEngineConfiguration =
        asAdmin {
            // Removing all previous configuration
            noGitHubConfig()
            // Creating two configs, return the last one
            gitHubConfig(
                url = if (sameRoot) {
                    "https://github.enterprise2.com"
                } else {
                    "https://github.enterprise1.com"
                }
            )
            gitHubConfig(url = "https://github.enterprise2.com")
        }

    protected fun noGitHubConfig() {
        asAdmin {
            // Removing all previous configuration
            gitConfigurationService.configurations.forEach {
                gitConfigurationService.deleteConfiguration(it.name)
            }
        }
    }
}