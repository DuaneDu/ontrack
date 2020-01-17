package net.nemerosa.ontrack.model.structure

import net.nemerosa.ontrack.model.security.BuildCreate
import net.nemerosa.ontrack.model.security.ProjectFunction
import net.nemerosa.ontrack.model.security.ValidationRunCreate
import java.time.LocalDateTime
import kotlin.reflect.KClass

/**
 * [ProjectEntity] which can be associated with some [RunInfo].
 */
interface RunnableEntity : ProjectEntity {
    /**
     * Gets the type of runnable entity
     */
    val runnableEntityType: RunnableEntityType
    /**
     * Gets the tags for metrics
     */
    val runMetricTags: Map<String, String>
    /**
     * Gets a name for this entity in a metric record
     */
    val runMetricName: String
    /**
     * Gets the creation time for this entity
     */
    val runTime: LocalDateTime
}

/**
 * Known list of [RunnableEntity] (not extensible).
 */
enum class RunnableEntityType(
        val projectFunction: KClass<out ProjectFunction>,
        private val loader: StructureService.(Int) -> RunnableEntity
) {
    build(
            BuildCreate::class,
            { getBuild(ID.of(it)) }
    ),
    validation_run(
            ValidationRunCreate::class,
            { getValidationRun(ID.of(it)) }
    );

    fun load(structureService: StructureService, id: Int) =
            structureService.loader(id)
}
