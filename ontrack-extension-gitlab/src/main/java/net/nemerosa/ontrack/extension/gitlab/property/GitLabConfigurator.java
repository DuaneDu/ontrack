package net.nemerosa.ontrack.extension.gitlab.property;

import net.nemerosa.ontrack.extension.git.model.GitConfiguration;
import net.nemerosa.ontrack.extension.git.model.GitConfigurator;
import net.nemerosa.ontrack.extension.git.model.GitPullRequest;
import net.nemerosa.ontrack.extension.gitlab.GitLabIssueServiceExtension;
import net.nemerosa.ontrack.extension.gitlab.model.GitLabIssueServiceConfiguration;
import net.nemerosa.ontrack.extension.issues.IssueServiceRegistry;
import net.nemerosa.ontrack.extension.issues.model.ConfiguredIssueService;
import net.nemerosa.ontrack.extension.issues.model.IssueServiceConfigurationRepresentation;
import net.nemerosa.ontrack.model.structure.Project;
import net.nemerosa.ontrack.model.structure.PropertyService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GitLabConfigurator implements GitConfigurator {

    private final PropertyService propertyService;
    private final IssueServiceRegistry issueServiceRegistry;
    private final GitLabIssueServiceExtension issueServiceExtension;

    @Autowired
    public GitLabConfigurator(PropertyService propertyService, IssueServiceRegistry issueServiceRegistry, GitLabIssueServiceExtension issueServiceExtension) {
        this.propertyService = propertyService;
        this.issueServiceRegistry = issueServiceRegistry;
        this.issueServiceExtension = issueServiceExtension;
    }

    @Override
    public boolean isProjectConfigured(@NotNull Project project) {
        return propertyService.hasProperty(project, GitLabProjectConfigurationPropertyType.class);
    }

    @Nullable
    @Override
    public GitConfiguration getConfiguration(@NotNull Project project) {
        return propertyService.getProperty(project, GitLabProjectConfigurationPropertyType.class)
                .option()
                .map(this::getGitConfiguration)
                .orElse(null);
    }

    @Nullable
    @Override
    public Integer toPullRequestID(@NotNull String key) {
        // TODO #690
        throw new UnsupportedOperationException("Pull requests not supported yet for GitLab");
    }

    @Nullable
    @Override
    public GitPullRequest getPullRequest(@NotNull GitConfiguration configuration, int id) {
        // TODO #690
        throw new UnsupportedOperationException("Pull requests not supported yet for GitLab");
    }

    private GitConfiguration getGitConfiguration(GitLabProjectConfigurationProperty property) {
        return new GitLabGitConfiguration(
                property,
                getConfiguredIssueService(property)
        );
    }

    private ConfiguredIssueService getConfiguredIssueService(GitLabProjectConfigurationProperty property) {
        String identifier = property.getIssueServiceConfigurationIdentifier();
        if (IssueServiceConfigurationRepresentation.isSelf(identifier)) {
            return new ConfiguredIssueService(
                    issueServiceExtension,
                    new GitLabIssueServiceConfiguration(
                            property.getConfiguration(),
                            property.getRepository()
                    )
            );
        } else {
            return issueServiceRegistry.getConfiguredIssueService(identifier);
        }
    }
}
