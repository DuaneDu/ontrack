package net.nemerosa.ontrack.extension.notifications.rendering

import net.nemerosa.ontrack.model.events.Event
import net.nemerosa.ontrack.model.structure.ProjectEntity
import net.nemerosa.ontrack.model.support.NameValue
import net.nemerosa.ontrack.model.support.OntrackConfigProperties
import net.nemerosa.ontrack.ui.controller.ProjectEntityPageBuilder
import org.springframework.stereotype.Component

@Component
class HtmlNotificationEventRenderer(
    ontrackConfigProperties: OntrackConfigProperties,
) : AbstractUrlNotificationEventRenderer(ontrackConfigProperties) {

    override fun render(projectEntity: ProjectEntity, event: Event): String {
        val pageUrl = getUrl(ProjectEntityPageBuilder.getEntityPageRelativeURI(projectEntity))
        return """<a href="$pageUrl">${getProjectEntityName(projectEntity)}</a>"""
    }

    override fun render(valueKey: String, value: NameValue, event: Event): String = value.value
}