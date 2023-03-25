package net.nemerosa.ontrack.extension.tfc.service

import net.nemerosa.ontrack.common.getOrNull
import net.nemerosa.ontrack.extension.general.BuildLinkDisplayPropertyType
import net.nemerosa.ontrack.model.buildfilter.BuildFilterService
import net.nemerosa.ontrack.model.security.SecurityService
import net.nemerosa.ontrack.model.structure.Build
import net.nemerosa.ontrack.model.structure.PropertyService
import net.nemerosa.ontrack.model.structure.StructureService
import org.springframework.stereotype.Service

@Service
class TFCServiceImpl(
    private val structureService: StructureService,
    private val securityService: SecurityService,
    private val propertyService: PropertyService,
    private val buildFilterService: BuildFilterService,
) : TFCService {

    override fun validate(params: TFCParameters, workspaceId: String, runUrl: String): TFCValidationResult {
        // Getting the actual parameters
        val actualParams = expandParams(params, workspaceId, runUrl)
        securityService.asAdmin {
            // Looking for the build
            val build = findBuild(actualParams)
            TODO("Getting or creating the validation stamp")
            TODO("Validation")
        }
    }

    private fun findBuild(params: TFCParameters): Build? {
        // Gets the branch first
        val branch = structureService.findBranchByName(params.project, params.branch).getOrNull()
            ?: return null
        // Using the build label
        val property = propertyService.getPropertyValue(branch.project, BuildLinkDisplayPropertyType::class.java)
        return if (property != null && property.useLabel) {
            buildFilterService.standardFilterProviderData(1)
                .withWithProperty(BuildLinkDisplayPropertyType::class.java.name)
                .withWithPropertyValue(params.build)
                .build()
                .filterBranchBuilds(branch)
                .firstOrNull()
        }
        // ... or the build name
        else {
            structureService.findBuildByName(params.project, params.branch, params.build).getOrNull()
        }
    }

    private fun expandParams(params: TFCParameters, workspaceId: String, runUrl: String): TFCParameters =
        if (params.hasVariables()) {
            // Gets the list of variables from TFC
            val variables = getWorkspaceVariables(workspaceId, runUrl)
            // Expands the parameters using these variables
            params.expand(variables, workspaceId)
        } else {
            params
        }

    private fun getWorkspaceVariables(workspaceId: String, runUrl: String): Map<String, String> {
        TODO("Not yet implemented")
    }

}