package net.nemerosa.ontrack.service

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.json.ObjectMapperFactory
import net.nemerosa.ontrack.model.security.ProjectConfig
import net.nemerosa.ontrack.model.security.ProjectView
import net.nemerosa.ontrack.model.security.SecurityService
import net.nemerosa.ontrack.model.structure.EntityDataService
import net.nemerosa.ontrack.model.structure.ProjectEntity
import net.nemerosa.ontrack.repository.EntityDataRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class EntityDataServiceImpl(
        private val repository: EntityDataRepository,
        private val securityService: SecurityService
) : EntityDataService {

    private val objectMapper = ObjectMapperFactory.create()

    override fun store(entity: ProjectEntity, key: String, value: Boolean) {
        store(entity, key, Objects.toString(value))
    }

    override fun store(entity: ProjectEntity, key: String, value: Int) {
        store(entity, key, Objects.toString(value))
    }

    override fun store(entity: ProjectEntity, key: String, value: Any) {
        securityService.checkProjectFunction(entity, ProjectConfig::class.java)
        val jsonNode = objectMapper.valueToTree<JsonNode>(value)
        repository.storeJson(entity, key, jsonNode)
    }

    override fun store(entity: ProjectEntity, key: String, value: String) {
        securityService.checkProjectFunction(entity, ProjectConfig::class.java)
        repository.store(entity, key, value)
    }

    override fun retrieveBoolean(entity: ProjectEntity, key: String): Boolean? {
        return retrieve(entity, key) { it == "true" }
    }

    override fun retrieveInteger(entity: ProjectEntity, key: String): Int? {
        return retrieve(entity, key) { value -> Integer.parseInt(value, 10) }
    }

    override fun retrieve(entity: ProjectEntity, key: String): String? {
        return retrieve(entity, key) { it }
    }

    override fun retrieveJson(entity: ProjectEntity, key: String): JsonNode? {
        return repository.retrieveJson(entity, key)
    }

    override fun <T> retrieve(entity: ProjectEntity, key: String, type: Class<T>): T? {
        val json = repository.retrieveJson(entity, key)
        return json?.let {
            objectMapper.treeToValue(it, type)
        }
    }

    protected fun <T> retrieve(entity: ProjectEntity, key: String, parser: (String) -> T?): T? {
        securityService.checkProjectFunction(entity, ProjectView::class.java)
        val text = repository.retrieve(entity, key)
        return text?.let(parser)
    }

    override fun delete(entity: ProjectEntity, key: String) {
        securityService.checkProjectFunction(entity, ProjectConfig::class.java)
        repository.delete(entity, key)
    }

    override fun <T> withData(entity: ProjectEntity, key: String, type: Class<T>, processFn: (T) -> T) {
        retrieve(entity, key, type)?.let { data -> store(entity, key, processFn(data) as Any) }
    }

}
