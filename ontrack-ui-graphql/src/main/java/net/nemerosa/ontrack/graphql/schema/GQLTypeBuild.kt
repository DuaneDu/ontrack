package net.nemerosa.ontrack.graphql.schema

import graphql.Scalars.*
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLArgument.newArgument
import graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLObjectType.newObject
import graphql.schema.GraphQLTypeReference
import net.nemerosa.ontrack.common.getOrNull
import net.nemerosa.ontrack.graphql.schema.actions.UIActionsGraphQLService
import net.nemerosa.ontrack.graphql.schema.actions.actions
import net.nemerosa.ontrack.graphql.support.listType
import net.nemerosa.ontrack.graphql.support.pagination.GQLPaginatedListFactory
import net.nemerosa.ontrack.model.pagination.PageRequest
import net.nemerosa.ontrack.model.structure.*
import net.nemerosa.ontrack.model.support.FreeTextAnnotatorContributor
import org.springframework.stereotype.Component

@Component
class GQLTypeBuild(
    private val uiActionsGraphQLService: UIActionsGraphQLService,
    private val structureService: StructureService,
    private val projectEntityInterface: GQLProjectEntityInterface,
    private val validation: GQLTypeValidation,
    private val validationRun: GQLTypeValidationRun,
    private val runInfo: GQLTypeRunInfo,
    private val runInfoService: RunInfoService,
    private val paginatedListFactory: GQLPaginatedListFactory,
    creation: GQLTypeCreation,
    projectEntityFieldContributors: List<GQLProjectEntityFieldContributor>,
    freeTextAnnotatorContributors: List<FreeTextAnnotatorContributor>
) : AbstractGQLProjectEntity<Build>(Build::class.java, ProjectEntityType.BUILD, projectEntityFieldContributors, creation, freeTextAnnotatorContributors) {

    override fun getTypeName() = BUILD

    override fun createType(cache: GQLTypeCache): GraphQLObjectType {
        return newObject()
                .name(BUILD)
                .withInterface(projectEntityInterface.typeRef)
                .fields(projectEntityInterfaceFields())
                // Actions
                .actions(uiActionsGraphQLService, Build::class)
                // Ref to branch
                .field(
                        newFieldDefinition()
                                .name("branch")
                                .description("Reference to branch")
                                .type(GraphQLTypeReference(GQLTypeBranch.BRANCH))
                                .build()
                )
                // Promotion runs
                .field(
                        newFieldDefinition()
                                .name("promotionRuns")
                                .description("Promotions for this build")
                                .argument(
                                        newArgument()
                                                .name(ARG_PROMOTION)
                                                .description("Name of the promotion level")
                                                .type(GraphQLString)
                                                .build()
                                )
                                .argument(
                                        newArgument()
                                                .name(ARG_LAST_PER_LEVEL)
                                                .description("Returns the last promotion run per promotion level")
                                                .type(GraphQLBoolean)
                                                .build()
                                )
                                .type(listType(GraphQLTypeReference(GQLTypePromotionRun.PROMOTION_RUN)))
                                .dataFetcher(buildPromotionRunsFetcher())
                                .build()
                )
                // Validation runs
                .field(
                        newFieldDefinition()
                                .name("validationRuns")
                                .description("Validations for this build")
                                .argument(
                                        newArgument()
                                                .name(ARG_VALIDATION_STAMP)
                                                .description("Name of the validation stamp, can be a regular expression.")
                                                .type(GraphQLString)
                                                .build()
                                )
                                .argument(
                                        newArgument()
                                                .name(ARG_COUNT)
                                                .description("Maximum number of validation runs")
                                                .type(GraphQLInt)
                                                .defaultValue(50)
                                                .build()
                                )
                                .type(listType(GraphQLTypeReference(GQLTypeValidationRun.VALIDATION_RUN)))
                                .dataFetcher(buildValidationRunsFetcher())
                                .build()
                )

                // Paginated list of validation runs
                .field(
                        paginatedListFactory.createPaginatedField<Build, ValidationRun>(
                                cache = cache,
                                fieldName = "validationRunsPaginated",
                                fieldDescription = "Paginated list of validation runs",
                                itemType = validationRun,
                                itemListCounter = { _, build ->
                                    structureService.getValidationRunsCountForBuild(
                                            build.id
                                    )
                                },
                                itemListProvider = { _, build, offset, size ->
                                    structureService.getValidationRunsForBuild(
                                            build.id,
                                            offset,
                                            size
                                    )
                                }
                        )
                )
                // Validation runs per validation stamp
                .field { f ->
                    f.name("validations")
                            .description("Validations per validation stamp")
                            .argument(
                                    newArgument()
                                            .name(ARG_VALIDATION_STAMP)
                                            .description("Name of the validation stamp")
                                            .type(GraphQLString)
                                            .build()
                            )
                            .argument {
                                it.name(GQLPaginatedListFactory.ARG_OFFSET)
                                        .description("Offset for the page")
                                        .type(GraphQLInt)
                                        .defaultValue(0)
                            }
                            .argument {
                                it.name(GQLPaginatedListFactory.ARG_SIZE)
                                        .description("Size of the page")
                                        .type(GraphQLInt)
                                        .defaultValue(PageRequest.DEFAULT_PAGE_SIZE)
                            }
                            .type(listType(validation.typeRef))
                            .dataFetcher(buildValidationsFetcher())
                }
                // Build links - "using" direction, with pagination
                .field(
                        paginatedListFactory.createPaginatedField<Build, Build>(
                                cache = cache,
                                fieldName = "using",
                                fieldDescription = "List of builds being used by this one.",
                                itemType = this,
                                arguments = listOf(
                                        newArgument()
                                                .name("project")
                                                .description("Keeps only links targeted from this project")
                                                .type(GraphQLString)
                                                .build(),
                                        newArgument()
                                                .name("branch")
                                                .description("Keeps only links targeted from this branch. `project` argument is also required.")
                                                .type(GraphQLString)
                                                .build()
                                ),
                                itemPaginatedListProvider = { environment, build, offset, size ->
                                    val filter: (Build) -> Boolean = getFilter(environment)
                                    structureService.getBuildsUsedBy(
                                            build,
                                            offset,
                                            size,
                                            filter
                                    )
                                }
                        )
                )
                // Build links - "usedBy" direction, with pagination
                .field(
                        paginatedListFactory.createPaginatedField<Build, Build>(
                                cache = cache,
                                fieldName = "usedBy",
                                fieldDescription = "List of builds using this one.",
                                itemType = this,
                                arguments = listOf(
                                        newArgument()
                                                .name("project")
                                                .description("Keeps only links targeted from this project")
                                                .type(GraphQLString)
                                                .build(),
                                        newArgument()
                                                .name("branch")
                                                .description("Keeps only links targeted from this branch. `project` argument is also required.")
                                                .type(GraphQLString)
                                                .build()
                                ),
                                itemPaginatedListProvider = { environment, build, offset, size ->
                                    val filter = getFilter(environment)
                                    structureService.getBuildsUsing(
                                            build,
                                            offset,
                                            size,
                                            filter
                                    )
                                }
                        )
                )
                // Run info
                .field {
                    it.name("runInfo")
                            .description("Run info associated with this build")
                            .type(runInfo.typeRef)
                            .runInfoFetcher<Build> { entity -> runInfoService.getRunInfo(entity) }
                }
                // OK
                .build()
    }

    private fun getFilter(environment: DataFetchingEnvironment): (Build) -> Boolean {
        val projectName: String? = environment.getArgument("project")
        val branchName: String? = environment.getArgument("branch")
        val filter: (Build) -> Boolean = if (branchName != null) {
            if (projectName == null) {
                throw IllegalArgumentException("`project` is required")
            } else {
                {
                    it.branch.project.name == projectName && it.branch.name == branchName
                }
            }
        } else if (projectName != null) {
            {
                it.branch.project.name == projectName
            }
        } else {
            { true }
        }
        return filter
    }

    private fun buildValidationsFetcher(): DataFetcher<List<GQLTypeValidation.GQLTypeValidationData>> =
        DataFetcher { environment ->
            val build: Build = environment.getSource()
            // Filter on validation stamp
            val validationStampName: String? = environment.getArgument(ARG_VALIDATION_STAMP)
            val offset = environment.getArgument<Int>(GQLPaginatedListFactory.ARG_OFFSET) ?: 0
            val size = environment.getArgument<Int>(GQLPaginatedListFactory.ARG_SIZE) ?: 10
            if (validationStampName != null) {
                val validationStamp: ValidationStamp? =
                        structureService.findValidationStampByName(
                                build.project.name,
                                build.branch.name,
                                validationStampName
                        ).orElse(null)
                if (validationStamp != null) {
                    listOf(
                            buildValidation(
                                    validationStamp, build, offset, size
                            )
                    )
                } else {
                    emptyList()
                }
            } else {
                // Gets the validation runs for the build
                structureService.getValidationStampListForBranch(build.branch.id)
                        .map { validationStamp -> buildValidation(validationStamp, build, offset, size) }
            }
        }

    private fun buildValidation(
            validationStamp: ValidationStamp,
            build: Build,
            offset: Int,
            size: Int
    ): GQLTypeValidation.GQLTypeValidationData {
        return GQLTypeValidation.GQLTypeValidationData(
                validationStamp,
                structureService.getValidationRunsForBuildAndValidationStamp(
                        build.id,
                        validationStamp.id,
                        offset,
                        size
                )
        )
    }

    private fun buildValidationRunsFetcher() =
            DataFetcher { environment ->
                val build: Build = environment.getSource()
                // Filter
                val count: Int = environment.getArgument(ARG_COUNT) ?: 50
                val validation: String? = environment.getArgument(ARG_VALIDATION_STAMP)
                if (validation != null) {
                    // Gets one validation stamp by name
                    val validationStamp = structureService.findValidationStampByName(
                            build.project.name,
                            build.branch.name,
                            validation
                    ).getOrNull()
                    // If there is one, we return the list of runs for this very stamp
                    if (validationStamp != null) {
                        // Gets validations runs for this validation level
                        return@DataFetcher structureService.getValidationRunsForBuildAndValidationStamp(
                                build.id,
                                validationStamp.id,
                                0,
                                count
                        )
                    }
                    // If not, we collect the list of matching validation stamp, assuming
                    // the argument is a regular expression
                    else {
                        val vsNameRegex = validation.toRegex()
                        return@DataFetcher structureService.getValidationStampListForBranch(build.branch.id)
                                .filter { vs -> vsNameRegex.matches(vs.name) }
                                .flatMap { vs ->
                                    structureService.getValidationRunsForBuildAndValidationStamp(
                                            build.id,
                                            vs.id,
                                            0, count
                                    )
                                }
                    }
                } else {
                    // Gets all the validation runs (limited by count)
                    return@DataFetcher structureService.getValidationRunsForBuild(build.id, 0, count)
                            .take(count)
                }
            }

    private fun buildPromotionRunsFetcher() =
            DataFetcher<List<PromotionRun>> { environment ->
                val build: Build = environment.getSource()
                // Last per promotion filter?
                val lastPerLevel: Boolean = environment.getArgument(ARG_LAST_PER_LEVEL) ?: false
                // Promotion filter
                val promotion: String? = environment.getArgument(ARG_PROMOTION)
                val promotionLevel: PromotionLevel? = if (promotion != null) {
                    // Gets the promotion level
                    structureService.findPromotionLevelByName(
                            build.project.name,
                            build.branch.name,
                            promotion
                    ).orElse(null)
                } else {
                    null
                }
                if (promotionLevel != null) {
                    // Gets promotion runs for this promotion level
                    if (lastPerLevel) {
                        return@DataFetcher structureService.getLastPromotionRunForBuildAndPromotionLevel(build, promotionLevel)
                                .map { listOf(it) }
                                .orElse(listOf())
                    } else {
                        return@DataFetcher structureService.getPromotionRunsForBuildAndPromotionLevel(build, promotionLevel)
                    }
                } else {
                    // Gets all the promotion runs
                    if (lastPerLevel) {
                        return@DataFetcher structureService.getLastPromotionRunsForBuild(build.id)
                    } else {
                        return@DataFetcher structureService.getPromotionRunsForBuild(build.id)
                    }
                }
            }

    override fun getSignature(entity: Build): Signature? {
        return entity.signature
    }

    companion object {
        /**
         * Name of the type
         */
        const val BUILD = "Build"
        /**
         * Filter on the validation runs
         */
        const val ARG_VALIDATION_STAMP = "validationStamp"
        /**
         * Count argument
         */
        const val ARG_COUNT = "count"
        /**
         * Promotion level argument
         */
        const val ARG_PROMOTION = "promotion"
        /**
         * Last per level argument
         */
        const val ARG_LAST_PER_LEVEL = "lastPerLevel"
    }
}
