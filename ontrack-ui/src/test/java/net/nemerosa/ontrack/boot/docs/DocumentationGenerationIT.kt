package net.nemerosa.ontrack.boot.docs

import net.nemerosa.ontrack.it.AbstractDSLTestSupport
import net.nemerosa.ontrack.model.annotations.getAPITypeDescription
import net.nemerosa.ontrack.model.docs.getDocumentationExampleCode
import net.nemerosa.ontrack.model.docs.getFieldsDocumentation
import net.nemerosa.ontrack.model.events.EventFactory
import net.nemerosa.ontrack.model.events.EventType
import net.nemerosa.ontrack.model.templating.TemplatingFilter
import net.nemerosa.ontrack.model.templating.TemplatingFunction
import net.nemerosa.ontrack.model.templating.TemplatingSource
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

/**
 * Generation of the documentation
 */
class DocumentationGenerationIT : AbstractDSLTestSupport() {

    @Autowired
    private lateinit var templatingFunctions: List<TemplatingFunction>

    @Autowired
    private lateinit var templatingFilters: List<TemplatingFilter>

    @Autowired
    private lateinit var templatingSources: List<TemplatingSource>

    @Autowired
    private lateinit var eventFactory: EventFactory

    @Test
    fun `Templating functions generation`() {
        withDirectory("templating/functions") {
            templatingFunctions.forEach { templatingFunction ->
                generateTemplatingFunction(this, templatingFunction)
            }
        }
    }

    @Test
    fun `Templating filters generation`() {
        withDirectory("templating/filters") {
            templatingFilters.forEach { templatingFilter ->
                generateTemplatingFilter(this, templatingFilter)
            }
        }
    }

    @Test
    fun `Templating sources generation`() {
        withDirectory("templating/sources") {
            templatingSources.forEach { templatingSource ->
                generateTemplatingSource(this, templatingSource)
            }
        }
    }

    @Test
    fun `Events generation`() {
        withDirectory("events") {
            eventFactory.eventTypes.forEach { eventType ->
                generateEventType(this, eventType)
            }
        }
    }

    private fun generateEventType(directoryContext: DirectoryContext, eventType: EventType) {
        val id = eventType.id

        val fileId = "event-$id"

        directoryContext.writeFile(
            fileId = fileId,
            level = 3,
            title = id,
        ) { s ->

            s.append(eventType.description).append("\n\n")

            s.append("Default template:\n\n")
            s.append("[source]\n")
            s.append("----\n")
            s.append(eventType.template)
            s.append("\n----\n\n")

        }
    }

    private fun generateTemplatingFunction(directoryContext: DirectoryContext, templatingFunction: TemplatingFunction) {
        val id = templatingFunction.id
        val description = getAPITypeDescription(templatingFunction::class)
        val parameters = getFieldsDocumentation(templatingFunction::class)
        val example = getDocumentationExampleCode(templatingFunction::class)

        val fileId = "templating-function-$id"

        directoryContext.writeFile(
            fileId = fileId,
            level = 4,
            title = id,
            header = description,
            fields = parameters,
            example = example,
        )
    }

    private fun generateTemplatingFilter(directoryContext: DirectoryContext, templatingFilter: TemplatingFilter) {
        val id = templatingFilter.id
        val description = getAPITypeDescription(templatingFilter::class)
        val example = getDocumentationExampleCode(templatingFilter::class)

        val fileId = "templating-filter-$id"

        directoryContext.writeFile(
            fileId = fileId,
            level = 4,
            title = id,
            header = description,
            fields = emptyMap(),
            example = example,
        )
    }

    private fun generateTemplatingSource(directoryContext: DirectoryContext, templatingSource: TemplatingSource) {
        val field = templatingSource.field
        val description = getAPITypeDescription(templatingSource::class)
        val parameters = getFieldsDocumentation(templatingSource::class)
        val example = getDocumentationExampleCode(templatingSource::class)

        val fileId = "templating-source-$field"

        directoryContext.writeFile(
            fileId = fileId,
            level = 4,
            title = field,
            header = description,
            fields = parameters,
            example = example,
        )
    }

    private class DirectoryContext(
        val dir: File,
    ) {

        fun writeFile(
            fileId: String,
            level: Int,
            title: String,
            code: (s: StringBuilder) -> Unit,
        ) {
            val file = File(dir, "${fileId}.adoc")

            val s = StringBuilder()

            val fileTitle = "${(1..level).joinToString("") { "=" }} $title"

            s.append("[[").append(fileId).append("]]\n")
            s.append(fileTitle).append("\n").append("\n")

            code(s)

            file.writeText(s.toString())
        }

        fun writeFile(
            fileId: String,
            level: Int,
            title: String,
            header: String?,
            fields: Map<String, String>,
            example: String?,
        ) {
            writeFile(
                fileId = fileId,
                level = level,
                title = title,
            ) { s ->

                if (!header.isNullOrBlank()) {
                    s.append(header.trimIndent()).append("\n").append("\n")
                }

                fields.toSortedMap().forEach { (name, description) ->
                    s.append("* **").append(name).append("** - ").append(description.trimIndent()).append("\n")
                        .append("\n")
                }

                if (!example.isNullOrBlank()) {
                    s.append("Example:").append("\n").append("\n")
                    s.append("[source]\n----\n")
                    s.append(example)
                    s.append("\n----\n")
                }

            }
        }
    }

    private fun withDirectory(path: String, code: DirectoryContext.() -> Unit) {
        val root = File("../ontrack-docs/src/docs/asciidoc")
        val dir = File(root, path)
        if (!dir.exists()) {
            dir.mkdirs()
        } else {
            dir.listFiles()?.forEach { file ->
                file.delete()
            }
        }
        val context = DirectoryContext(dir)
        context.code()
    }

}