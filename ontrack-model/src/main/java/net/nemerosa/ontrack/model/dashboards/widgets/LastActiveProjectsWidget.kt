package net.nemerosa.ontrack.model.dashboards.widgets

import net.nemerosa.ontrack.json.asJson
import net.nemerosa.ontrack.model.dashboards.WidgetInstance
import org.springframework.stereotype.Component

@Component
class LastActiveProjectsWidget : AbstractWidget<LastActiveProjectsWidget.LastActiveProjectsWidgetConfig>(
    key = "home/LastActiveProjects",
    name = "Last active projects",
    description = "Displays the list of the last active projects. The number of projects which are displayed can be configured.",
    defaultConfig = LastActiveProjectsWidgetConfig(count = 10),
) {
    data class LastActiveProjectsWidgetConfig(
        val count: Int = 10,
    ) : WidgetConfig
}