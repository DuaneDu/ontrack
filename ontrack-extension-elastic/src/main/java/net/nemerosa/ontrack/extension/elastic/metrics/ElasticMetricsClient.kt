package net.nemerosa.ontrack.extension.elastic.metrics

import net.nemerosa.ontrack.model.structure.SearchNodeResults

interface ElasticMetricsClient {

    fun saveMetric(entry: ECSEntry)

    fun saveMetrics(entries: Collection<ECSEntry>)

    fun rawSearch(
        token: String,
        offset: Int = 0,
        size: Int = 10,
    ): SearchNodeResults

    /**
     * Drops the target index, and all its data.
     */
    fun dropIndex()

}