package net.nemerosa.ontrack.extension.bitbucket.cloud.configuration

import net.nemerosa.ontrack.extension.support.ConfigurationConnectorStatusIndicator
import net.nemerosa.ontrack.model.security.SecurityService
import net.nemerosa.ontrack.model.support.ConnectionResult
import net.nemerosa.ontrack.model.support.Connector
import net.nemerosa.ontrack.model.support.ConnectorDescription
import org.springframework.stereotype.Component

@Component
class BitbucketCloudConnectorStatusIndicator(
    private val configurationService: BitbucketCloudConfigurationService,
    securityService: SecurityService
) : ConfigurationConnectorStatusIndicator<BitbucketCloudConfiguration>(configurationService, securityService) {

    override fun connect(config: BitbucketCloudConfiguration) {
        val result = configurationService.test(config)
        if (result.type != ConnectionResult.ConnectionResultType.OK) {
            throw IllegalStateException(result.message)
        }
    }

    override fun connectorDescription(config: BitbucketCloudConfiguration) = ConnectorDescription(
        connector = Connector(type, config.name),
        connection = config.workspace
    )

    override val type: String = "bitbucket-cloud"
}
