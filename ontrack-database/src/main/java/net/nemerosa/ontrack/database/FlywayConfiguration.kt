package net.nemerosa.ontrack.database

import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.stereotype.Component

@Component
class FlywayConfiguration: FlywayConfigurationCustomizer {
    override fun customize(configuration: FluentConfiguration) {
        configuration.table("schema_version")
    }
}