package net.nemerosa.ontrack.graphql.schema

import net.nemerosa.ontrack.common.getOrNull
import net.nemerosa.ontrack.graphql.support.TypedMutationProvider
import net.nemerosa.ontrack.model.annotations.APIDescription
import net.nemerosa.ontrack.model.exceptions.BranchNotFoundException
import net.nemerosa.ontrack.model.structure.ID
import net.nemerosa.ontrack.model.structure.NameDescription
import net.nemerosa.ontrack.model.structure.PromotionLevel
import net.nemerosa.ontrack.model.structure.StructureService
import org.springframework.stereotype.Component
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

@Component
class PromotionLevelMutations(
    private val structureService: StructureService,
) : TypedMutationProvider() {

    override val mutations: List<Mutation>
        get() = listOf(
            /**
             * Setting up a promotion level
             */
            simpleMutation(
                name = "setupPromotionLevel",
                description = "Creates or updates a promotion level for a branch",
                input = SetupPromotionLevelInput::class,
                outputName = "promotionLevel",
                outputDescription = "Created or updated promotion level",
                outputType = PromotionLevel::class,
                fetcher = this::setupPromotionLevel
            ),
            /**
             * Creating a promotion level from a branch ID
             */
            simpleMutation(
                "createPromotionLevelById",
                "Creates a new promotion level from a branch ID",
                CreatePromotionLevelByIdInput::class,
                "promotionLevel",
                "Created promotion level",
                PromotionLevel::class
            ) { input ->
                val branch = structureService.getBranch(ID.of(input.branchId))
                structureService.newPromotionLevel(
                    PromotionLevel.of(
                        branch,
                        NameDescription.nd(input.name, input.description)
                    )
                )
            },
        )

    private fun setupPromotionLevel(input: SetupPromotionLevelInput): PromotionLevel {
        val existing =
            structureService.findPromotionLevelByName(input.project, input.branch, input.promotion).getOrNull()
        return if (existing != null) {
            // Updates the promotion if need be
            updatePromotionLevel(existing, input)
        } else {
            createPromotionLevel(input)
        }
    }

    private fun updatePromotionLevel(existing: PromotionLevel, input: SetupPromotionLevelInput): PromotionLevel {
        val updated = existing.update(
            NameDescription.nd(input.promotion, input.description)
        )
        // Saves in repository
        structureService.savePromotionLevel(updated)
        // As resource
        return updated
    }

    private fun createPromotionLevel(input: SetupPromotionLevelInput): PromotionLevel {
        val branch =
            structureService.findBranchByName(input.project, input.branch).getOrNull()
                ?: throw BranchNotFoundException(input.project, input.branch)
        val promotionLevel = PromotionLevel.of(
            branch,
            NameDescription.nd(input.promotion, input.description)
        )
        // Saves it into the repository
        return structureService.newPromotionLevel(promotionLevel)
    }
}

/**
 * Input for the `setupPromotionLevel` mutation.
 */
data class SetupPromotionLevelInput(
    val project: String,
    val branch: String,
    val promotion: String,
    val description: String?,
)

data class CreatePromotionLevelByIdInput(
    @APIDescription("Branch ID")
    val branchId: Int,
    @get:NotNull(message = "The name is required.")
    @get:Pattern(regexp = NameDescription.NAME, message = "The name ${NameDescription.NAME_MESSAGE_SUFFIX}")
    @APIDescription("Promotion level name")
    val name: String,
    @APIDescription("Promotion level description")
    val description: String,
)
