package net.nemerosa.ontrack.model.dashboards

import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.json.asJson
import net.nemerosa.ontrack.model.dashboards.widgets.Widget

/**
 * A _widget instance_ is the association of a _widget_ and its _configuration_.
 *
 * @property uuid Unique ID of this widget instance (since several widgets of the same type/key)
 * can be present in a dashboard.
 * @property key Identifier of the widget _type_
 * @property config Configuration for this widget inside the dashboard.
 */
data class WidgetInstance(
    val uuid: String,
    val key: String,
    val config: JsonNode,
) {
    companion object {
        fun fromDefaultWidget(uuid: String, widget: Widget<*>) = WidgetInstance(
            uuid = uuid,
            key = widget.key,
            config = widget.defaultConfig.asJson(),
        )
    }
}