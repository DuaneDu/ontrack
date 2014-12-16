package net.nemerosa.ontrack.extension.github.client;

import net.nemerosa.ontrack.extension.github.model.*;
import net.nemerosa.ontrack.common.Time;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.IssueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DefaultOntrackGitHubClient implements OntrackGitHubClient {

    private final Logger logger = LoggerFactory.getLogger(OntrackGitHubClient.class);

    @Override
    public GitHubIssue getIssue(String project, GitHubClientConfigurator configurator, int id) {
        // Logging
        logger.debug("[github] Getting issue {}/{}", project, id);
        // GitHub client (non authentified)
        GitHubClient client = new GitHubClient() {
            @Override
            protected HttpURLConnection configureRequest(HttpURLConnection request) {
                HttpURLConnection connection = super.configureRequest(request);
                connection.setRequestProperty(HEADER_ACCEPT, "application/vnd.github.v3.full+json");
                return connection;
            }
        };
        configurator.configure(client);
        // Issue service using this client
        IssueService service = new IssueService(client);
        // Gets the repository for this project
        String owner = StringUtils.substringBefore(project, "/");
        String name = StringUtils.substringAfter(project, "/");
        Issue issue;
        try {
            issue = service.getIssue(owner, name, id);
        } catch (RequestException ex) {
            if (ex.getStatus() == 404) {
                return null;
            } else {
                throw new OntrackGitHubClientException(ex);
            }
        } catch (IOException e) {
            throw new OntrackGitHubClientException(e);
        }
        // Conversion
        return new GitHubIssue(
                id,
                issue.getHtmlUrl(),
                issue.getTitle(),
                issue.getBodyText(),
                issue.getBodyHtml(),
                toUser(issue.getAssignee()),
                toLabels(issue.getLabels()),
                toState(issue.getState()),
                toMilestone(project, issue.getMilestone()),
                toDateTime(issue.getCreatedAt()),
                toDateTime(issue.getUpdatedAt()),
                toDateTime(issue.getClosedAt())
        );
    }

    private LocalDateTime toDateTime(Date date) {
        if (date == null) {
            return null;
        } else {
            return Time.from(date, null);
        }
    }

    private GitHubMilestone toMilestone(String project, Milestone milestone) {
        if (milestone != null) {
            return new GitHubMilestone(
                    milestone.getTitle(),
                    toState(milestone.getState()),
                    milestone.getNumber(),
                    String.format("https://github.com/%s/issues?milestone=%d&state=open", project, milestone.getNumber())
            );
        } else {
            return null;
        }
    }

    private GitHubState toState(String state) {
        return GitHubState.valueOf(state);
    }

    private List<GitHubLabel> toLabels(List<Label> labels) {
        return labels.stream()
                .map(label -> new GitHubLabel(
                        label.getName(),
                        label.getColor()
                ))
                .collect(Collectors.toList());
    }

    private GitHubUser toUser(User user) {
        if (user == null) {
            return null;
        } else {
            return new GitHubUser(
                    user.getLogin(),
                    user.getHtmlUrl()
            );
        }
    }

}
