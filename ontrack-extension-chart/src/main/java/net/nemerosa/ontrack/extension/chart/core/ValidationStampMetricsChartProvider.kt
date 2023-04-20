package net.nemerosa.ontrack.extension.chart.core

import com.fasterxml.jackson.databind.node.NullNode
import net.nemerosa.ontrack.extension.chart.ChartDefinition
import net.nemerosa.ontrack.extension.chart.GetChartOptions
import net.nemerosa.ontrack.extension.chart.support.MetricsChart
import net.nemerosa.ontrack.extension.chart.support.MetricsChartItemData
import net.nemerosa.ontrack.json.asJson
import net.nemerosa.ontrack.model.structure.*
import org.springframework.stereotype.Component

@Component
class ValidationStampMetricsChartProvider(
        structureService: StructureService,
        private val validationDataTypeService: ValidationDataTypeService,
) : AbstractValidationStampChartProvider<MetricsChart>(structureService) {

    override val name: String = "validation-stamp-metrics"

    override fun getChartDefinition(subject: ValidationStamp): ChartDefinition? {
        // Gets the validation data type from the validation stamp
        val dataTypeId = subject.dataType?.descriptor?.id
        // ... or from the last validation run
                ?: structureService.getValidationRunsForValidationStamp(subject, 0, 1).firstOrNull()?.data?.descriptor?.id
                ?: return null
        // Gets the data type
        val dataType = validationDataTypeService.getValidationDataType<Any, Any>(dataTypeId)
        // Creating a chart only if this validation data type can provide numeric metrics
        return if (dataType is NumericValidationDataType) {
            ChartDefinition(
                    id = name,
                    title = "Validation stamp metrics",
                    type = MetricsChart.TYPE,
                    config = NullNode.instance,
                    parameters = mapOf(
                            "id" to subject.id()
                    ).asJson(),
            )
        } else {
            null
        }
    }

    override fun getChart(runs: List<ValidationRun>, options: GetChartOptions): MetricsChart {
        // Gets the validation data type from the v
        val dataTypeId = runs.firstOrNull()
                ?.validationStamp?.dataType?.descriptor?.id
                ?: return MetricsChart.compute(
                        names = emptyList(),
                        colors = emptyList(),
                        items = emptyList(),
                        interval = options.actualInterval,
                        period = options.period,
                )
        val dataType = (validationDataTypeService.getValidationDataType<Any, Any>(dataTypeId)
                ?.takeIf { it is NumericValidationDataType }
                ?: return MetricsChart.compute(
                        names = emptyList(),
                        colors = emptyList(),
                        items = emptyList(),
                        interval = options.actualInterval,
                        period = options.period,
                ))
                as NumericValidationDataType
        // Collecting all metrics
        val items = runs.mapNotNull { run ->
            run.data?.let { data ->
                val runDataType = validationDataTypeService.getValidationDataType<Any, Any>(data.descriptor.id)
                if (runDataType != null && runDataType.descriptor.id == dataTypeId) {
                    MetricsChartItemData(
                            timestamp = run.lastStatus.signature.time,
                            dataType.getNumericMetrics(data.data!!)
                    )
                } else {
                    null
                }
            }
        }
        // Creating the chart
        return MetricsChart.compute(
                names = dataType.getMetricNames(),
                colors = dataType.getMetricColors(),
                items = items,
                interval = options.actualInterval,
                period = options.period,
        )
    }
}