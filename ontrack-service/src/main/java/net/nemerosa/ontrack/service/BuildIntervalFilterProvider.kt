package net.nemerosa.ontrack.service

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.common.getOrNull
import net.nemerosa.ontrack.json.JsonUtils
import net.nemerosa.ontrack.model.form.Form
import net.nemerosa.ontrack.model.form.Text
import net.nemerosa.ontrack.model.structure.Branch
import net.nemerosa.ontrack.model.structure.Build
import net.nemerosa.ontrack.model.structure.ID
import net.nemerosa.ontrack.model.structure.StructureService
import net.nemerosa.ontrack.repository.CoreBuildFilterRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
@Transactional
@Deprecated("Will be removed in V5")
class BuildIntervalFilterProvider(
    private val filterRepository: CoreBuildFilterRepository,
    private val structureService: StructureService,
) : AbstractBuildFilterProvider<BuildIntervalFilterData>() {

    override val type: String = BuildIntervalFilterProvider::class.java.name

    override val name: String = "Build interval (deprecated)"
    override val isPredefined: Boolean = false

    override fun fill(form: Form, data: BuildIntervalFilterData): Form {
        return form
            .fill("from", data.from)
            .fill("to", data.to)
    }

    override fun blankForm(branchId: ID): Form = Form.create()
        .with(
            Text.of("from")
                .label("From build")
                .help("First build")
        )
        .with(
            Text.of("to")
                .label("To build")
                .optional()
                .help("Last build")
        )

    override fun filterBranchBuilds(branch: Branch, data: BuildIntervalFilterData?): List<Build> {
        return filterRepository.between(branch, data?.from, data?.to)
    }

    override fun validateData(branch: Branch, data: BuildIntervalFilterData?): String? {
        return validateBuild(branch, data?.from) ?: validateBuild(branch, data?.to)
    }

    private fun validateBuild(branch: Branch, name: String?): String? =
        name?.let {
            val build = structureService.findBuildByName(
                branch.project.name,
                branch.name,
                it
            ).getOrNull()
            if (build != null) {
                null
            } else {
                """Build "$name" does not exist for "${branch.entityDisplayName}"."""
            }
        }

    override fun parse(data: JsonNode): BuildIntervalFilterData =
        BuildIntervalFilterData(
            JsonUtils.get(data, "from", true, null),
            JsonUtils.get(data, "to", true, null)
        )
}
