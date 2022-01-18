package net.nemerosa.ontrack.extension.elastic.metrics

/**
 * Defines where the Elastic metrics should be sent.
 */
enum class ElasticMetricsTarget {

    /**
     * When this option is selected, the ES instance used by Ontrack for the regular search will be used.
     */
    MAIN,

    /**
     * When this option is selected, the ES instance used by the management metrics will be used.
     */
    MGT,

    /**
     * When this option is selected, the ES instance defined by the metrics properties will be used.
     */
    CUSTOM,

}