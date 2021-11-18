package net.nemerosa.ontrack.extension.github.ingestion.processing.config

import net.nemerosa.ontrack.model.structure.Branch
import net.nemerosa.ontrack.model.structure.EntityDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DefaultConfigService(
    private val entityDataService: EntityDataService,
    private val configLoaderService: ConfigLoaderService,
) : ConfigService {

    override fun getOrLoadConfig(branch: Branch, path: String): IngestionConfig {
        return load(branch)
            ?: configLoaderService.loadConfig(branch, path)
                ?.apply { store(branch) } // Stores when loaded
            ?: IngestionConfig() // Default configuration
    }

    override fun loadAndSaveConfig(branch: Branch, path: String): IngestionConfig? {
        val config = configLoaderService.loadConfig(branch, path)
        return config?.apply {
            store(branch)
        }
    }

    override fun saveConfig(branch: Branch, config: IngestionConfig) {
        config.store(branch)
    }

    private fun IngestionConfig.store(ontrackBranch: Branch) {
        entityDataService.store(
            ontrackBranch,
            IngestionConfig::class.java.name,
            this,
        )
    }

    override fun removeConfig(branch: Branch) {
        entityDataService.delete(
            branch,
            IngestionConfig::class.java.name,
        )
    }

    override fun findConfig(branch: Branch): IngestionConfig? = load(branch)

    private fun load(ontrackBranch: Branch) = entityDataService.retrieve(
        ontrackBranch,
        IngestionConfig::class.java.name,
        IngestionConfig::class.java,
    )
}