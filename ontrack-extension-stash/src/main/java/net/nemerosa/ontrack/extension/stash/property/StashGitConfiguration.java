package net.nemerosa.ontrack.extension.stash.property;

import net.nemerosa.ontrack.extension.git.model.GitConfiguration;
import net.nemerosa.ontrack.extension.issues.model.ConfiguredIssueService;
import net.nemerosa.ontrack.model.support.UserPassword;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;

public class StashGitConfiguration implements GitConfiguration {

    private final StashProjectConfigurationProperty property;
    private final ConfiguredIssueService configuredIssueService;

    public StashGitConfiguration(StashProjectConfigurationProperty property, ConfiguredIssueService configuredIssueService) {
        this.property = property;
        this.configuredIssueService = configuredIssueService;
    }

    @Override
    public String getType() {
        return "stash";
    }

    @Override
    public String getName() {
        return property.getConfiguration().getName();
    }

    public StashProjectConfigurationProperty getProperty() {
        return property;
    }

    /**
     * Checks if this configuration denotes any Bitbucket Cloud instance
     */
    protected boolean isCloud() {
        return property.getConfiguration().isCloud();
    }

    @Override
    public String getRemote() {
        return format(
                getRemoteFormat(),
                property.getConfiguration().getUrl(),
                property.getProject(),
                property.getRepository()
        );
    }

    @Nullable
    @Override
    public UserPassword getCredentials() {
        return new UserPassword(
                property.getConfiguration().getUser(),
                property.getConfiguration().getPassword()
        );
    }

    @Override
    public String getCommitLink() {
        return format(
                getCommitLinkFormat(),
                property.getConfiguration().getUrl(),
                property.getProject(),
                property.getRepository()
        );
    }

    @Override
    public String getFileAtCommitLink() {
        return format(
                getFileAtCommitLinkFormat(),
                property.getConfiguration().getUrl(),
                property.getProject(),
                property.getRepository()
        );
    }

    @Override
    public int getIndexationInterval() {
        return property.getIndexationInterval();
    }

    @Override
    public ConfiguredIssueService getConfiguredIssueService() {
        return configuredIssueService;
    }

    protected String getFileAtCommitLinkFormat() {
        return isCloud() ? "%s/%s/%s/src/{commit}/{path}" : "%s/projects/%s/repos/%s/browse/{path}?at={commit}";
    }

    protected String getCommitLinkFormat() {
        return isCloud() ? "%s/%s/%s/commits/{commit}" : "%s/projects/%s/repos/%s/commits/{commit}";
    }

    protected String getRemoteFormat() {
        return isCloud() ? "%s/%s/%s.git" : "%s/scm/%s/%s.git";
    }
}
