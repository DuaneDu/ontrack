package net.nemerosa.ontrack.extension.github.casc

import net.nemerosa.ontrack.extension.casc.AbstractCascTestJUnit4Support
import net.nemerosa.ontrack.extension.github.catalog.GitHubSCMCatalogSettings
import org.junit.Test
import kotlin.test.assertEquals

class GitHubSCMCatalogSettingsContextIT : AbstractCascTestJUnit4Support() {

    @Test
    fun `GitHub SCM Catalog settings as CasC`() {
        asAdmin {
            withSettings<GitHubSCMCatalogSettings> {
                casc("""
                    ontrack:
                        config:
                            settings:
                                github-scm-catalog:
                                    orgs:
                                        - nemerosa
                                        - other
                """.trimIndent())
                val settings = cachedSettingsService.getCachedSettings(GitHubSCMCatalogSettings::class.java)
                assertEquals(
                    listOf(
                        "nemerosa",
                        "other"
                    ),
                    settings.orgs
                )
            }
        }
    }

}