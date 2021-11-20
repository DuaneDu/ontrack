package net.nemerosa.ontrack.extension.github.ingestion.processing.config

import net.nemerosa.ontrack.model.annotations.APIDescription
import net.nemerosa.ontrack.model.annotations.APIName

/**
 * Default path for the ingestion file
 */
const val INGESTION_CONFIG_FILE_PATH = ".github/ontrack/ingestion.yml"

/**
 * Configuration for the ingestion.
 *
 * @param general General settings
 * @param steps List of specific step configurations
 * @param jobs List of specific job configurations
 * @param jobsFilter Filtering on the jobs
 * @param stepsFilter Filtering on the steps
 * @param promotions Auto promotion configuration
 */
@APIName("GitHubIngestionConfig")
@APIDescription("Configuration for the ingestion of GitHub Actions workflows.")
data class IngestionConfig(
    @APIDescription("General settings")
    val general: IngestionConfigGeneral = IngestionConfigGeneral(),
    @APIDescription("List of specific step configurations")
    val steps: List<StepConfig> = emptyList(),
    @APIDescription("List of specific job configurations")
    val jobs: List<JobConfig> = emptyList(),
    @APIDescription("Filtering on the jobs")
    val jobsFilter: FilterConfig = FilterConfig(),
    @APIDescription("Filtering on the steps")
    val stepsFilter: FilterConfig = FilterConfig(),
    @APIDescription("Auto promotion configuration")
    val promotions: List<PromotionConfig> = emptyList(),
)

/**
 * General settings
 *
 * @param skipJobs Must jobs be considered as validations?
 */
@APIName("GitHubIngestionConfigGeneral")
@APIDescription("General settings")
data class IngestionConfigGeneral(
    @APIDescription("Must jobs be considered as validations?")
    val skipJobs: Boolean = true,
)

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
)

/**
 * Step configuration
 *
 * @param name Exact name of the step in the workflow
 * @param validation Name of the validation stamp to use (instead of a generated one)
 * @param validationJobPrefix Must we use the job name as a prefix to the validation stamp?
 * @param description Description for the validation stamp
 */
@APIName("GitHubIngestionStepConfig")
data class StepConfig(
    @APIDescription("Exact name of the step in the workflow")
    val name: String,
    @APIDescription("Name of the validation stamp to use (instead of a generated one)")
    val validation: String? = null,
    @APIDescription("Must we use the job name as a prefix to the validation stamp?")
    val validationJobPrefix: Boolean = true,
    @APIDescription("Description for the validation stamp")
    val description: String? = null,
)

/**
 * Job level configuration
 *
 * @param name Exact name of the job in the workflow
 * @param validation Name of the validation stamp to use (instead of a generated one)
 * @param description Description for the validation stamp
 */
@APIName("GitHubIngestionJobConfig")
data class JobConfig(
    @APIDescription("Exact name of the job in the workflow")
    val name: String,
    @APIDescription("Name of the validation stamp to use (instead of a generated one)")
    val validation: String? = null,
    @APIDescription("Description for the validation stamp")
    val description: String? = null,
)

/**
 * Promotion configuration
 *
 * @param name Unique name for the promotion in the branch
 */
@APIName("GitHubIngestionPromotionConfig")
data class PromotionConfig(
    @APIDescription("Unique name for the promotion in the branch")
    val name: String,
    @APIDescription("Optional description")
    val description: String? = null,
    @APIDescription("List of validations triggering this promotion. Important: these names are the names of the validations after step name resolution.")
    val validations: List<String> = emptyList(),
    @APIDescription("List of promotions triggering this promotion")
    val promotions: List<String> = emptyList(),
    @APIDescription("Regular expression to include validation stamps by name")
    val include: String? = null,
    @APIDescription("Regular expression to exclude validation stamps by name")
    val exclude: String? = null,
)
