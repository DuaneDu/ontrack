package net.nemerosa.ontrack.service

import net.nemerosa.ontrack.model.Ack
import net.nemerosa.ontrack.model.metrics.MetricsExportService
import net.nemerosa.ontrack.model.security.ApplicationManagement
import net.nemerosa.ontrack.model.security.ProjectEdit
import net.nemerosa.ontrack.model.security.ProjectView
import net.nemerosa.ontrack.model.security.SecurityService
import net.nemerosa.ontrack.model.structure.*
import net.nemerosa.ontrack.repository.RunInfoRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RunInfoServiceImpl(
    private val runInfoRepository: RunInfoRepository,
    private val structureService: StructureService,
    private val securityService: SecurityService,
    private val metricsExportService: MetricsExportService,
) : RunInfoService {

    private val logger: Logger = LoggerFactory.getLogger(RunInfoService::class.java)

    override fun getRunnableEntity(runnableEntityType: RunnableEntityType, id: Int): RunnableEntity {
        return runnableEntityType.load(structureService, id)
    }

    override fun getRunInfo(entity: RunnableEntity): RunInfo? {
        securityService.checkProjectFunction(entity, ProjectView::class.java)
        return runInfoRepository.getRunInfo(
            entity.runnableEntityType,
            entity.id()
        )
    }

    override fun setRunInfo(entity: RunnableEntity, input: RunInfoInput): RunInfo {
        securityService.checkProjectFunction(entity, entity.runnableEntityType.projectFunction.java)
        val runInfo = runInfoRepository.setRunInfo(
            entity.runnableEntityType,
            entity.id(),
            input,
            securityService.currentSignature
        )
        exportRunInfoTime(entity, runInfo)
        return runInfo
    }

    override fun deleteRunInfo(runnableEntity: RunnableEntity): Ack {
        securityService.checkProjectFunction(runnableEntity, ProjectEdit::class.java)
        return runInfoRepository.deleteRunInfo(
            runnableEntity.runnableEntityType,
            runnableEntity.id()
        )
    }

    private fun exportRunInfoTime(entity: RunnableEntity, runInfo: RunInfo) {
        val time = runInfo.runTime
        if (time != null) {
            metricsExportService.exportMetrics(
                metric = "ontrack_run_${entity.runnableEntityType.name}_time_seconds",
                tags = entity.runMetricTags,
                fields = mapOf("value" to time.toDouble()),
                timestamp = entity.signature.time,
            )
        }
    }

    @Transactional(readOnly = true)
    override fun restore(logger: (String) -> Unit) {
        securityService.checkGlobalFunction(ApplicationManagement::class.java)
        RunnableEntityType.values().forEach { type ->
            logger("Restoring all runnable entities for $type...")
            var count = 0
            val total = runInfoRepository.getCountByRunnableEntityType(type)
            runInfoRepository.forEachRunnableEntityType(type) { id, runInfo ->
                val entity = type.load(structureService, id)
                exportRunInfoTime(entity, runInfo)
                count++
                if (count % 100 == 0) {
                    this.logger.info("Restored $count/$total ${type}s...")
                }
            }
            logger("Restoration of all runnable entities for $type is done.")
        }
    }
}
