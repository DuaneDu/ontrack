package net.nemerosa.ontrack.extension.influxdb

import net.nemerosa.ontrack.common.Time
import net.nemerosa.ontrack.model.support.StartupService
import okhttp3.OkHttpClient
import org.influxdb.BatchOptions
import org.influxdb.InfluxDB
import org.influxdb.InfluxDBFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class DefaultInfluxDBConnection(
    private val influxDBExtensionProperties: InfluxDBExtensionProperties
) : InfluxDBConnection, StartupService {

    private val logger: Logger = LoggerFactory.getLogger(DefaultInfluxDBConnection::class.java)

    private var internalConnection: InfluxDB? = null
    private var lastCheck: LocalDateTime? = null

    private val internalConnectionLock = ReentrantReadWriteLock()

    override fun getName(): String = "InfluxDB connection"

    override fun startupOrder(): Int = StartupService.SYSTEM_REGISTRATION

    override fun start() {
        if (current != null && isValid) {
            logger.info("InfluxDB connection to ${influxDBExtensionProperties.uri} is OK")
        } else {
            logger.error("InfluxDB connection to ${influxDBExtensionProperties.uri} is not OK")
        }
    }

    override fun safe(code: InfluxDB.() -> Unit) {
        current?.apply {
            try {
                code()
            } catch (ex: Exception) {
                logger.error("Issue with running against InfluxDB", ex)
            }
        } ?: logger.error("InfluxDB connection is not available")
    }

    override val current: InfluxDB?
        get() = checkAndGet()

    override val isValid: Boolean
        get() = internalConnection?.check() ?: false

    private fun checkAndGet(): InfluxDB? {
        val influxDB = getOrCreate()
        if (influxDB != null) {
            if (lastCheck == null || Duration.between(lastCheck, Time.now()) > influxDBExtensionProperties.validity) {
                if (!influxDB.check()) {
                    logger.info("Connection to InfluxDB is not valid any longer, attempting to renew it.")
                    return renew()
                } else {
                    lastCheck = Time.now()
                }
            }
        }
        return influxDB
    }

    private fun InfluxDB.check(): Boolean {
        val pong = ping()
        return pong?.isGood ?: false
    }

    private fun getOrCreate(): InfluxDB? = internalConnectionLock.read {
        internalConnection ?: create()
    }

    private fun renew(): InfluxDB? = create()

    private fun create(): InfluxDB? {
        return internalConnectionLock.write {
            doCreate()?.apply {
                if (!check()) {
                    logger.warn("InfluxDB connection was created/renewed but is not valid. Recreation will be attempted in ${influxDBExtensionProperties.validity}.")
                }
                lastCheck = Time.now()
                internalConnection = this
            }
        }
    }

    private fun doCreate(): InfluxDB? {
        try {
            logger.info("InfluxDB URI = ${influxDBExtensionProperties.uri}")
            logger.info("InfluxDB database = ${influxDBExtensionProperties.db}")

            var builder = OkHttpClient.Builder()
            if (!influxDBExtensionProperties.ssl.hostCheck) {
                builder = builder.hostnameVerifier { _, _ -> true }
            }

            val influxDB = InfluxDBFactory.connect(
                influxDBExtensionProperties.uri,
                influxDBExtensionProperties.username,
                influxDBExtensionProperties.password,
                builder
            )
            influxDB.setDatabase(influxDBExtensionProperties.db)
            if (influxDBExtensionProperties.create) {
                influxDB.createDatabase(influxDBExtensionProperties.db)
            }
            influxDB.setLogLevel(influxDBExtensionProperties.log)
            influxDB.enableBatch(BatchOptions.DEFAULTS)

            return influxDB
        } catch (ex: Exception) {
            logger.error("Cannot create the InfluxDB connection", ex)
            return null
        }
    }
}