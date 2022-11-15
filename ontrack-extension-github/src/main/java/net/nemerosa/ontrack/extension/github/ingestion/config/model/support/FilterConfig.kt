package net.nemerosa.ontrack.extension.github.ingestion.config.model.support

import net.nemerosa.ontrack.extension.github.ingestion.support.FilterHelper
import net.nemerosa.ontrack.model.annotations.APIDescription
import net.nemerosa.ontrack.model.annotations.APIName

/**
 * Filter rule
 *
 * @param includes Regular expression to include the items
 * @param excludes Regular expression to exclude the items (empty = no exclusion)
 */
@APIName("GitHubIngestionFilterConfig")
@APIDescription("Filter rule")
data class FilterConfig(
    @APIDescription("Regular expression to include the items")
    val includes: String = ".*",
    @APIDescription("Regular expression to exclude the items (empty = no exclusion)")
    val excludes: String = "",
) {
    fun includes(name: String) = FilterHelper.includes(name, includes, excludes)

    companion object {
        /**
         * Filter to include everything
         */
        val all = FilterConfig(
            includes = ".*",
            excludes = ""
        )

        /**
         * Filter to exclude everything
         */
        val none = FilterConfig(
            includes = "",
            excludes = ".*"
        )
    }
}