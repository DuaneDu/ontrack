package net.nemerosa.ontrack.extension.av.config

import net.nemerosa.ontrack.model.security.ProjectConfig
import net.nemerosa.ontrack.model.security.SecurityService
import net.nemerosa.ontrack.model.structure.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AutoVersioningConfigurationServiceImpl(
    private val securityService: SecurityService,
    private val entityDataService: EntityDataService,
    private val structureService: StructureService,
) : AutoVersioningConfigurationService {

    override fun setupAutoVersioning(branch: Branch, config: AutoVersioningConfig?) {
        securityService.checkProjectFunction(branch, ProjectConfig::class.java)
        if (config != null) {
            entityDataService.store(branch, STORE, config)
        } else {
            entityDataService.delete(branch, STORE)
        }
    }

    override fun getAutoVersioning(branch: Branch): AutoVersioningConfig? =
        entityDataService.retrieve(branch, STORE, AutoVersioningConfig::class.java)?.postDeserialize()

    override fun getBranchesConfiguredFor(project: String, promotion: String): List<Branch> =
        entityDataService.findEntities(
            type = ProjectEntityType.BRANCH,
            key = STORE,
            jsonQuery = """JSON_VALUE::jsonb->'configurations' @> '[{"sourceProject":"$project","sourcePromotion":"$promotion"}]'::jsonb""",
            jsonQueryParameters = emptyMap(),
        ).map {
            structureService.getBranch(ID.of(it.id))
        }

    companion object {
        private val STORE: String = AutoVersioningConfig::class.java.name
    }
}