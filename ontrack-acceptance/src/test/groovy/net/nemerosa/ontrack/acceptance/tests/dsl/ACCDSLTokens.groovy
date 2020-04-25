package net.nemerosa.ontrack.acceptance.tests.dsl

import net.nemerosa.ontrack.acceptance.AbstractACCDSL
import org.junit.Test

import static net.nemerosa.ontrack.test.TestUtils.uid

/**
 * Connecting with tokens & managing the tokens.
 */
class ACCDSLTokens extends AbstractACCDSL {

    @Test
    void 'Connecting with a token'() {
        // Creating a project
        def name = uid("P")
        ontrack.project(name, "Test project")
        // Generates a token for current user
        def token = ontrackAsAnyUser.tokens.generate()
        // Creates a connection using this token
        def ontrackWithToken = ontrackBuilder.authenticate(token.value).build()
        // Checks we can still connect, and get the list of projects
        def project = ontrackWithToken.projects.find { it.name == name }
        assert project != null: "Project still accessible through token authentication"
    }

}
