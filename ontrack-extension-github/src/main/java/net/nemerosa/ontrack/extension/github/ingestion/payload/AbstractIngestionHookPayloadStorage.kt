package net.nemerosa.ontrack.extension.github.ingestion.payload

import net.nemerosa.ontrack.common.Time
import net.nemerosa.ontrack.model.security.GlobalSettings
import net.nemerosa.ontrack.model.security.SecurityService
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

abstract class AbstractIngestionHookPayloadStorage(
    protected val securityService: SecurityService,
) : IngestionHookPayloadStorage {

    abstract fun internalStore(payload: IngestionHookPayload)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun store(payload: IngestionHookPayload) {
        securityService.checkGlobalFunction(GlobalSettings::class.java)
        internalStore(payload)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun routing(payload: IngestionHookPayload, routing: String) {
        securityService.checkGlobalFunction(GlobalSettings::class.java)
        // Gets the old payload
        val old = getByUUID(payload.uuid)
        // Saves the new version
        internalStore(
            IngestionHookPayload(
                uuid = payload.uuid,
                timestamp = old.timestamp,
                gitHubDelivery = old.gitHubDelivery,
                gitHubEvent = old.gitHubEvent,
                gitHubHookID = old.gitHubHookID,
                gitHubHookInstallationTargetID = old.gitHubHookInstallationTargetID,
                gitHubHookInstallationTargetType = old.gitHubHookInstallationTargetType,
                payload = old.payload,
                repository = old.repository,
                status = old.status,
                started = old.started,
                message = old.message,
                completion = old.completion,
                routing = routing,
                queue = old.queue,
            )
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun queue(payload: IngestionHookPayload, queue: String) {
        securityService.checkGlobalFunction(GlobalSettings::class.java)
        // Gets the old payload
        val old = getByUUID(payload.uuid)
        // Saves the new version
        internalStore(
            IngestionHookPayload(
                uuid = payload.uuid,
                timestamp = old.timestamp,
                gitHubDelivery = old.gitHubDelivery,
                gitHubEvent = old.gitHubEvent,
                gitHubHookID = old.gitHubHookID,
                gitHubHookInstallationTargetID = old.gitHubHookInstallationTargetID,
                gitHubHookInstallationTargetType = old.gitHubHookInstallationTargetType,
                payload = old.payload,
                repository = old.repository,
                status = old.status,
                started = old.started,
                message = old.message,
                completion = old.completion,
                routing = old.routing,
                queue = queue,
            )
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun start(payload: IngestionHookPayload) {
        securityService.checkGlobalFunction(GlobalSettings::class.java)
        // Gets the old payload
        val old = getByUUID(payload.uuid)
        // Saves the new version
        internalStore(
            IngestionHookPayload(
                uuid = payload.uuid,
                timestamp = old.timestamp,
                gitHubDelivery = old.gitHubDelivery,
                gitHubEvent = old.gitHubEvent,
                gitHubHookID = old.gitHubHookID,
                gitHubHookInstallationTargetID = old.gitHubHookInstallationTargetID,
                gitHubHookInstallationTargetType = old.gitHubHookInstallationTargetType,
                payload = old.payload,
                repository = old.repository,
                status = IngestionHookPayloadStatus.PROCESSING,
                started = Time.now(),
                message = null,
                completion = null,
                routing = old.routing,
                queue = old.queue,
            )
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun finished(payload: IngestionHookPayload) {
        securityService.checkGlobalFunction(GlobalSettings::class.java)
        // Gets the old payload
        val old = getByUUID(payload.uuid)
        // Saves the new version
        internalStore(
            IngestionHookPayload(
                uuid = payload.uuid,
                timestamp = old.timestamp,
                gitHubDelivery = old.gitHubDelivery,
                gitHubEvent = old.gitHubEvent,
                gitHubHookID = old.gitHubHookID,
                gitHubHookInstallationTargetID = old.gitHubHookInstallationTargetID,
                gitHubHookInstallationTargetType = old.gitHubHookInstallationTargetType,
                payload = old.payload,
                repository = old.repository,
                status = IngestionHookPayloadStatus.COMPLETED,
                started = old.started,
                message = null,
                completion = Time.now(),
                routing = old.routing,
                queue = old.queue,
            )
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun error(payload: IngestionHookPayload, any: Throwable) {
        securityService.checkGlobalFunction(GlobalSettings::class.java)
        // Gets the old payload
        val old = getByUUID(payload.uuid)
        // Saves the new version
        internalStore(
            IngestionHookPayload(
                uuid = payload.uuid,
                timestamp = old.timestamp,
                gitHubDelivery = old.gitHubDelivery,
                gitHubEvent = old.gitHubEvent,
                gitHubHookID = old.gitHubHookID,
                gitHubHookInstallationTargetID = old.gitHubHookInstallationTargetID,
                gitHubHookInstallationTargetType = old.gitHubHookInstallationTargetType,
                payload = old.payload,
                repository = old.repository,
                status = IngestionHookPayloadStatus.ERRORED,
                started = old.started,
                message = ExceptionUtils.getStackTrace(any),
                completion = Time.now(),
                routing = old.routing,
                queue = old.queue,
            )
        )
    }

    private fun getByUUID(uuid: UUID) = findByUUID(uuid.toString())
        ?: throw IngestionHookPayloadUUIDNotFoundException(uuid)

}