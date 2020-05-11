package net.nemerosa.ontrack.extension.indicators.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.nemerosa.ontrack.extension.indicators.store.IndicatorStore
import net.nemerosa.ontrack.extension.indicators.store.StoredIndicator
import net.nemerosa.ontrack.model.security.SecurityService
import net.nemerosa.ontrack.model.structure.Project
import net.nemerosa.ontrack.model.structure.Signature
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class IndicatorServiceImpl(
        private val securityService: SecurityService,
        private val indicatorStore: IndicatorStore,
        private val indicatorTypeService: IndicatorTypeService
) : IndicatorService {

    override fun getProjectIndicators(project: Project, all: Boolean): List<Indicator<*>> {
        // Gets all the types
        val types = indicatorTypeService.findAll()
        // Gets the values
        return types.map {
            loadIndicator(project, it)
        }.filter {
            all || it.value != null
        }
    }

    override fun getProjectIndicator(project: Project, typeId: String): Indicator<*> {
        val type = indicatorTypeService.getTypeById(typeId)
        return loadIndicator(project, type)
    }

    override fun <T> getProjectIndicator(project: Project, type: IndicatorType<T, *>): Indicator<T> {
        return loadIndicator(project, type)
    }

    override fun <T> updateProjectIndicator(project: Project, typeId: String, input: JsonNode): Indicator<T> {
        @Suppress("UNCHECKED_CAST")
        val type = indicatorTypeService.getTypeById(typeId) as IndicatorType<T, *>
        // Comment extraction
        val comment = if (input is ObjectNode && input.has(FIELD_COMMENT)) {
            val comment = input.get(FIELD_COMMENT).asText()
            input.remove(FIELD_COMMENT)
            comment
        } else {
            null
        }
        // Parsing
        val value = type.fromClientJson(input)
        // Signature
        val signature = securityService.currentSignature
        // Storing the indicator
        indicatorStore.storeIndicator(project, type.id, StoredIndicator(
                value = value?.run { type.toStoredJson(this) } ?: NullNode.instance,
                comment = comment,
                signature = signature
        ))
        // OK
        return loadIndicator(project, type)
    }

    private fun <T, C> loadIndicator(project: Project, type: IndicatorType<T, C>): Indicator<T> {
        val stored = indicatorStore.loadIndicator(project, type.id)
        return if (stored != null && !stored.value.isNull) {
            val value = type.fromStoredJson(stored.value)
            Indicator(
                    type = type,
                    value = value,
                    status = value?.let { type.getStatus(it) },
                    comment = stored.comment,
                    signature = stored.signature
            )
        } else {
            Indicator(
                    type = type,
                    value = null,
                    status = null,
                    comment = null,
                    signature = Signature.anonymous()
            )
        }
    }

    companion object {
        private const val FIELD_COMMENT = "comment"
    }
}