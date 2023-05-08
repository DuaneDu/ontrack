package net.nemerosa.ontrack.extension.tfc.config

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.common.syncForward
import net.nemerosa.ontrack.extension.casc.context.AbstractCascContext
import net.nemerosa.ontrack.extension.casc.context.SubConfigContext
import net.nemerosa.ontrack.extension.casc.schema.*
import net.nemerosa.ontrack.json.JsonParseException
import net.nemerosa.ontrack.json.asJson
import net.nemerosa.ontrack.json.getRequiredTextField
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * CasC definition for the list of TFC configurations.
 */
@Component
class TFCConfigurationCasc(
        private val tfcConfigurationService: TFCConfigurationService,
) : AbstractCascContext(), SubConfigContext {

    private val logger: Logger = LoggerFactory.getLogger(TFCConfigurationCasc::class.java)

    override val field: String = "tfc"

    override val type: CascType
        get() = cascArray(
                "List of TFC configurations",
                cascObject(
                        "TFC configuration",
                        cascField("name", cascString, "Unique name for the configuration", required = true),
                        cascField("url", cascString, "TFC/TFE root URL", required = true),
                        cascField("token", cascString, "TFC/TFE user or token", required = true),
                )
        )

    override fun run(node: JsonNode, paths: List<String>) {
        val items = node.mapIndexed { index, child ->
            try {
                child.parseItem()
            } catch (ex: JsonParseException) {
                throw IllegalStateException(
                        "Cannot parse into ${TFCConfiguration::class.qualifiedName}: ${path(paths + index.toString())}",
                        ex
                )
            }
        }

        // Gets the list of existing configurations
        val configurations = tfcConfigurationService.configurations

        // Synchronization
        syncForward(
                from = items,
                to = configurations,
        ) {
            equality { a, b -> a.name == b.name }
            onCreation { item ->
                logger.info("Creating TFC configuration: ${item.name}")
                tfcConfigurationService.newConfiguration(item)
            }
            onModification { item, _ ->
                logger.info("Updating TFC configuration: ${item.name}")
                tfcConfigurationService.updateConfiguration(item.name, item)
            }
            onDeletion { existing ->
                logger.info("Deleting TFC configuration: ${existing.name}")
                tfcConfigurationService.deleteConfiguration(existing.name)
            }
        }
    }

    override fun render(): JsonNode = tfcConfigurationService
            .configurations
            .map(TFCConfiguration::obfuscate)
            .asJson()

    private fun JsonNode.parseItem(): TFCConfiguration =
            TFCConfiguration(
                    name = getRequiredTextField("name"),
                    url = getRequiredTextField("url"),
                    token = getRequiredTextField("token"),
            )
}