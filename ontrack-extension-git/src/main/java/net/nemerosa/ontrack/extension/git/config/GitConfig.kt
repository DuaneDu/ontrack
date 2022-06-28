package net.nemerosa.ontrack.extension.git.config

import net.nemerosa.ontrack.extension.git.GitConfigProperties
import net.nemerosa.ontrack.git.GitRepositoryClientFactory
import net.nemerosa.ontrack.git.support.GitRepositoryClientFactoryImpl
import net.nemerosa.ontrack.model.support.EnvService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GitConfig(
    private val envService: EnvService,
    private val gitConfigProperties: GitConfigProperties,
) {
    @Bean
    fun gitRepositoryClientFactory(): GitRepositoryClientFactory {
        val repositories = envService.getWorkingDir("git", "repositories")
        return GitRepositoryClientFactoryImpl(
            root = repositories,
            timeout = gitConfigProperties.remote.timeout,
            operationTimeout = gitConfigProperties.remote.operationTimeout,
            retries = gitConfigProperties.remote.retries,
            interval = gitConfigProperties.remote.interval,
        )
    }

}
