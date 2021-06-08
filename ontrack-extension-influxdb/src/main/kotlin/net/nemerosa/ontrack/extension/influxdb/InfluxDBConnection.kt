package net.nemerosa.ontrack.extension.influxdb

import org.influxdb.InfluxDB

/**
 * Holds a connection to InfluxDB.
 */
interface InfluxDBConnection {

    /**
     * Gets the current connection to InfluxDB.
     *
     * The connection is returned as-is and might be invalid. Its validaty
     * can be tested using the [isValid] method.
     *
     * The connection may be renewed at any time to deal with instabilities
     * with the connectivity to the InfluxDB backend.
     *
     * Is `null` is the connection to InfluxDB could not be established.
     */
    val current: InfluxDB?

    /**
     * Uses the [current] connection to run a safe operation with InfluxDB (no fail)
     */
    fun safe(code: InfluxDB.() -> Unit)

    /**
     * Checks if the [current] connection to InfluxDB is a valid one.
     *
     * @param immediate If the connection must be tested immediately or can wait until next test window.
     */
    fun isValid(immediate: Boolean = false): Boolean

    /**
     * Forces a reconnection at the next attempt
     */
    fun reset()

}