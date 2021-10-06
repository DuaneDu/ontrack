package net.nemerosa.ontrack.extension.gitlab.property;

import net.nemerosa.ontrack.extension.git.model.GitConfiguration;
import net.nemerosa.ontrack.extension.issues.model.ConfiguredIssueService;
import net.nemerosa.ontrack.git.GitRepositoryAuthenticator;
import net.nemerosa.ontrack.git.UsernamePasswordGitRepositoryAuthenticator;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;

public class GitLabGitConfiguration implements GitConfiguration {

    public static final String CONFIGURATION_REPOSITORY_SEPARATOR = ":";

    private final GitLabProjectConfigurationProperty property;
    private final ConfiguredIssueService configuredIssueService;

    public GitLabGitConfiguration(GitLabProjectConfigurationProperty property, ConfiguredIssueService configuredIssueService) {
        this.property = property;
        this.configuredIssueService = configuredIssueService;
    }

    @Override
    public String getType() {
        return "gitlab";
    }

    @Override
    public String getName() {
        return property.getConfiguration().getName();
    }

    public GitLabProjectConfigurationProperty getProperty() {
        return property;
    }

    @Override
    public String getRemote() {
        return format(
                "%s/%s.git",
                property.getConfiguration().getUrl(),
                property.getRepository()
        );
    }

    @Nullable
    @Override
    public GitRepositoryAuthenticator getAuthenticator() {
        return new UsernamePasswordGitRepositoryAuthenticator(
                property.getConfiguration().getUser(),
                property.getConfiguration().getPassword()
        );
    }

    @Override
    public String getCommitLink() {
        return format(
                "%s/%s/commit/{commit}",
                property.getConfiguration().getUrl(),
                property.getRepository()
        );
    }

    @Override
    public String getFileAtCommitLink() {
        return format(
                "%s/%s/blob/{commit}/{path}",
                property.getConfiguration().getUrl(),
                property.getRepository()
        );
    }

    @Override
    public int getIndexationInterval() {
        return property.getIndexationInterval();
    }

    @Nullable
    @Override
    public ConfiguredIssueService getConfiguredIssueService() {
        return configuredIssueService;
    }
}
