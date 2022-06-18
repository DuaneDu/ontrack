package net.nemerosa.ontrack.kdsl.acceptance.tests.github.system

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import net.nemerosa.ontrack.json.parse
import net.nemerosa.ontrack.kdsl.acceptance.tests.github.gitHubPlaygroundClient
import net.nemerosa.ontrack.kdsl.acceptance.tests.github.gitHubPlaygroundEnv
import net.nemerosa.ontrack.kdsl.acceptance.tests.support.uid
import net.nemerosa.ontrack.kdsl.acceptance.tests.support.waitUntil
import net.nemerosa.ontrack.kdsl.spec.Branch
import net.nemerosa.ontrack.kdsl.spec.Ontrack
import net.nemerosa.ontrack.kdsl.spec.Project
import net.nemerosa.ontrack.kdsl.spec.extension.git.GitBranchConfigurationProperty
import net.nemerosa.ontrack.kdsl.spec.extension.git.gitBranchConfigurationProperty
import net.nemerosa.ontrack.kdsl.spec.extension.github.GitHubConfiguration
import net.nemerosa.ontrack.kdsl.spec.extension.github.GitHubProjectConfigurationProperty
import net.nemerosa.ontrack.kdsl.spec.extension.github.gitHub
import net.nemerosa.ontrack.kdsl.spec.extension.github.gitHubConfigurationProperty
import org.apache.commons.codec.binary.Base64
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpClientErrorException.NotFound
import java.util.*
import kotlin.test.assertTrue
import kotlin.test.fail

fun withTestGitHubRepository(
    autoMerge: Boolean = false,
    code: GitHubRepositoryContext.() -> Unit,
) {
    // Unique name for the repository
    val uuid = UUID.randomUUID().toString()
    val repo = "ontrack-auto-versioning-test-$uuid"

    // Creates the repository
    gitHubPlaygroundClient.postForObject(
        "/orgs/${gitHubPlaygroundEnv.organization}/repos",
        mapOf(
            "name" to repo,
            "description" to "Test repository for auto versioning - can be deleted at any time",
            "private" to false,
            "has_issues" to false,
            "has_projects" to false,
            "has_wiki" to false
        ),
        Unit::class.java
    )

    try {

        // Creating a dummy file
        // createFile(
        //     branch = "master", path = "README.md", content = listOf(
        //         "Test repository - can be deleted"
        //     )
        // )

        // Context
        val context = GitHubRepositoryContext(repo)

        // Running the code
        context.code()


    } finally {
        // Deleting the repository
        gitHubPlaygroundClient.delete("/repos/${gitHubPlaygroundEnv.organization}/$repo")
    }
}

class GitHubRepositoryContext(
    val repository: String,
) {

    fun repositoryFile(
        path: String,
        branch: String = "main",
        content: () -> String,
    ) {
        createBranchIfNotExisting(branch)

        val existingFile = getRawFile(path, branch)

        val body = mutableMapOf(
            "message" to "Creating $path",
            "content" to Base64.encodeBase64String(content().toByteArray()),
            "branch" to branch,
        )

        if (existingFile != null) {
            body["sha"] = existingFile.sha
        }

        gitHubPlaygroundClient.put(
            "/repos/${gitHubPlaygroundEnv.organization}/${repository}/contents/$path",
            body
        )
    }

    private fun createBranchIfNotExisting(branch: String, base: String = "main") {
        if (branch != base) {
            // Checks if the branch exists
            val gitHubBranch = getBranch(branch)
            if (gitHubBranch == null) {
                // Gets the last commit of the base branch
                val baseBranch = getBranch(base) ?: throw IllegalStateException("Cannot find base branch $base")
                val baseCommit = baseBranch.commit.sha
                // Creates the branch
                gitHubPlaygroundClient.postForObject(
                    "/repos/${gitHubPlaygroundEnv.organization}/${repository}/git/refs",
                    mapOf(
                        "ref" to "refs/heads/$branch",
                        "sha" to baseCommit
                    ),
                    JsonNode::class.java
                )
            }
        }
    }

    fun createGitHubConfiguration(ontrack: Ontrack): String {
        val name = uid("gh")
        ontrack.gitHub.createConfig(
            GitHubConfiguration(
                name = name,
                url = "https://github.com",
                oauth2Token = gitHubPlaygroundEnv.token,
                autoMergeToken = gitHubPlaygroundEnv.autoMergeToken,
            )
        )
        return name
    }

    fun Project.configuredForGitHub(ontrack: Ontrack) {
        val config = createGitHubConfiguration(ontrack)
        gitHubConfigurationProperty = GitHubProjectConfigurationProperty(
            configuration = config,
            repository = "${gitHubPlaygroundEnv.organization}/$repository",
        )
    }

    fun Branch.configuredForGitHubRepository(
        ontrack: Ontrack,
        scmBranch: String = "main",
    ) {
        project.configuredForGitHub(ontrack)
        gitBranchConfigurationProperty = GitBranchConfigurationProperty(branch = scmBranch)
    }

    fun assertThatGitHubRepository(
        code: AssertionContext.() -> Unit,
    ) {
        val context = AssertionContext()
        context.code()
    }

    private fun getPR(from: String?, to: String?): GitHubPR? {
        var url = "/repos/${gitHubPlaygroundEnv.organization}/$repository/pulls?state=all"
        if (from != null) {
            url += "&head=$from"
        }
        if (to != null) {
            url += "&base=$to"
        }
        return gitHubPlaygroundClient.getForObject(
            url,
            JsonNode::class.java
        )?.firstOrNull()?.parse()
    }

    private fun getPRReviews(pr: GitHubPR): List<GitHubPRReview> =
        gitHubPlaygroundClient.getForObject(
            "/repos/${gitHubPlaygroundEnv.organization}/$repository/pulls/${pr.number}/reviews",
            JsonNode::class.java
        )?.map {
            it.parse<GitHubPRReview>()
        } ?: emptyList()

    private fun getFile(path: String, branch: String): List<String> =
        getRawFile(path, branch)?.content?.run { String(Base64.decodeBase64(this)) }?.lines()
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private fun getRawFile(path: String, branch: String): GitHubContentsResponse? =
        try {
            gitHubPlaygroundClient.getForObject(
                "/repos/${gitHubPlaygroundEnv.organization}/$repository/contents/$path?ref=$branch",
                GitHubContentsResponse::class.java
            )
        } catch (ex: NotFound) {
            null
        }

    private fun getBranch(branch: String) =
        try {
            gitHubPlaygroundClient.getForObject(
                "/repos/${gitHubPlaygroundEnv.organization}/$repository/branches/$branch",
                GitHubBranch::class.java
            )
        } catch (ex: HttpClientErrorException.NotFound) {
            null
        }

    inner class AssertionContext {

        fun hasNoBranch(branch: String) {
            val gitHubBranch = getBranch(branch)
            if (gitHubBranch != null) {
                fail("Branch $branch exists when it was expected not to.")
            }
        }

        private fun checkPR(from: String?, to: String?, checkPR: (GitHubPR?) -> Boolean): GitHubPR? {
            var pr: GitHubPR? = null
            waitUntil(
                task = "Checking the PR from $from to $to"
            ) {
                pr = getPR(from, to)
                checkPR(pr)
            }
            return pr
        }

        fun hasPR(from: String, to: String): GitHubPR =
            checkPR(from = from, to = to) {
                it != null
            } ?: error("PR cannot be null after the check")

        fun hasNoPR(to: String) {
            checkPR(from = null, to = to) {
                it == null
            }
        }

        fun checkPRIsNotApproved(pr: GitHubPR) {
            val reviews = getPRReviews(pr)
            assertTrue(
                reviews.none { it.state == "APPROVED" },
                "No review was approved"
            )
        }

        fun fileContains(
            path: String,
            branch: String = "main",
            content: () -> String,
        ) {
            val expectedContent = content()
            waitUntil(
                task = "Waiting for file $path on branch $branch to have a given content.",
                onTimeout = {
                    val actualContent = getFile(path, branch).joinToString("\n")
                    fail(
                        """
Expected the following content for the $path file on the $branch branch:

$expectedContent

but got:

$actualContent
""".trimIndent()
                    )
                }
            ) {
                val actualContent = getFile(path, branch).joinToString("\n")
                expectedContent in actualContent
            }
        }

    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
class GitHubPR(
    val number: Int,
    val state: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class GitHubPRReview(
    val user: GitHubUser,
    val state: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private class GitHubContentsResponse(
    val content: String,
    val sha: String,
)


@JsonIgnoreProperties(ignoreUnknown = true)
class GitHubBranch(
    val name: String,
    val commit: GitHubCommit,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class GitHubCommit(
    val sha: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class GitHubUser(
    val login: String,
)