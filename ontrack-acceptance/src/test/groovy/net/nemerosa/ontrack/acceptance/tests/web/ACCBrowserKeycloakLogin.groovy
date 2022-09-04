package net.nemerosa.ontrack.acceptance.tests.web

import net.nemerosa.ontrack.acceptance.AcceptanceTestClient
import net.nemerosa.ontrack.acceptance.browser.pages.HomePage
import net.nemerosa.ontrack.acceptance.browser.pages.LoginPage
import net.nemerosa.ontrack.acceptance.support.AcceptanceTestSuite
import org.junit.Test
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.UsersResource
import org.keycloak.representations.idm.*

import static net.nemerosa.ontrack.test.TestUtils.uid

@AcceptanceTestSuite
class ACCBrowserKeycloakLogin extends AcceptanceTestClient {

    @Test
    void 'Login with Keycloak'() {
        withKeycloakConfigured { realm, _, userSimple ->
            browser { browser ->
                def loginPage = goTo(LoginPage, [:])
                assert loginPage.hasExtension(realm): "OIDC extension is present"
                def keycloakLoginPage = loginPage.useExtension(realm)
                def homePage = keycloakLoginPage.login(userSimple, "secret")
                def userName = homePage.header.userName
                assert userName == "User ${userSimple}"
            }
        }
    }

    @Test
    void 'Login with Keycloak is not possible when account is disabled'() {
        withKeycloakConfigured { realm, _, userSimple ->
            browser { browser ->
                // Login once to register the user
                def loginPage = goTo(LoginPage, [:])
                assert loginPage.hasExtension(realm): "OIDC extension is present"
                def keycloakLoginPage = loginPage.useExtension(realm)
                def homePage = keycloakLoginPage.login(userSimple, "secret")
                def userName = homePage.header.userName
                assert userName == "User ${userSimple}"
                // Logout
                homePage.logout()
                // Disabling the user
                ontrack.admin.findAccountByName("${userSimple.toLowerCase()}@nemerosa.net").disable()
                // Trying to log again
                loginPage = goTo(LoginPage, [:])
                loginPage.useExtensionWithoutCheck(realm)
                // Keycloak is still OK
                // keycloakLoginPage.login(userSimple, "secret")
                assert loginPage.invalidCredentialsDisplayed: "Login is rejected"
            }
        }
    }

    @Test
    void 'Login with Keycloak and sets as admin'() {
        withMaxProjects(100) {
            withKeycloakConfigured { String realm, userAdmin, _ ->
                browser { browser ->
                    // Initial login
                    def loginPage = goTo(LoginPage, [:])
                    assert loginPage.hasExtension(realm): "OIDC extension is present"
                    def keycloakLoginPage = loginPage.useExtension(realm)
                    def homePage = keycloakLoginPage.login(userAdmin, "secret")
                    def userName = homePage.header.userName
                    assert userName == "Admin ${userAdmin}"
                    loginPage = homePage.logout()
                    // Setup of group mappings
                    ontrack.admin.setGroupMapping("oidc", realm, "ontrack-admin", "Administrators")
                    // Re-login
                    assert loginPage.hasExtension(realm): "OIDC extension is present"
                    homePage = loginPage.useExtension(realm, HomePage) // We are already authenticated in Keycloak, going directly to the Home page
                    // Checks the user can create a project
                    def projectName = uid('P')
                    homePage.createProject {
                        name = projectName
                        description = "Project ${projectName}"
                    }
                    // Checks the project is visible in the list
                    assert homePage.isProjectPresent(projectName)
                }
            }
        }
    }

    private void withKeycloakConfigured(Closure code) {

        Keycloak adminClient = KeycloakBuilder.builder()
                .serverUrl("${configRule.config.keycloakUri}/auth")
                .realm("master")
                .clientId("admin-cli")
                .clientSecret(null)
                .username(configRule.config.keycloakUsername)
                .password(configRule.config.keycloakPassword)
                .build()

        def realm = uid("r")
        def representation = new RealmRepresentation()
        representation.realm = realm
        representation.enabled = true
        adminClient.realms().create(representation)

        def realmClient = adminClient.realm(realm)

        try {

            // Creates three roles
            def roles = realmClient.roles()
            Map<String, String> roleIds = ["ontrack-admin", "ontrack-user", "other-role"].collectEntries { role ->
                def rep = new RoleRepresentation()
                rep.setName(role)
                roles.create(rep)
                String id = roles.list().find { it.name == role }.id
                [role, id]
            }

            // Creates two users
            def users = realmClient.users()

            def userAdmin = createUser(
                    users,
                    "Admin",
                    ["ontrack-admin", "ontrack-user", "other-role"],
                    roleIds
            )

            def userSimple = createUser(
                    users,
                    "User",
                    ["ontrack-user", "other-role"],
                    roleIds
            )

            // Creates an application
            def clientAdmin = realmClient.clients()
            def clientId = uid("C")
            def client = new ClientRepresentation()
            client.protocol = "openid-connect"
            client.clientId = clientId
            client.baseUrl = baseURL
            client.enabled = true
            client.redirectUris = [
                    "${baseURL}/*" as String
            ]
            client.baseUrl = "${baseURL}/login/oauth2/code/$realm" as String
            client.webOrigins = [baseURL]
            client.directAccessGrantsEnabled = true

            // Groups mapper
            def groupProtocolMapperRepresentation = new ProtocolMapperRepresentation()
            // groupProtocolMapperRepresentation.consentRequired = false
            groupProtocolMapperRepresentation.name = "groups"
            groupProtocolMapperRepresentation.protocol = "openid-connect"
            groupProtocolMapperRepresentation.protocolMapper = "oidc-usermodel-realm-role-mapper"
            groupProtocolMapperRepresentation.config = [
                   "access.token.claim": "true",
                   "claim.name": "groups",
                   "id.token.claim": "true",
                   "jsonType.label": "String",
                   "multivalued": "true",
                   // "user.attribute	"foo"
            ]
            client.protocolMappers = [ groupProtocolMapperRepresentation ]

            clientAdmin.create(client)

            ontrack.config.oidcSettings.createProvider(
                    realm,
                    "Test $realm",
                    "",
                    "${configRule.config.keycloakUri}/auth/realms/$realm",
                    clientId,
                    "",
                    ".*"
            )
            try {
                code(realm, userAdmin, userSimple)
            } finally {
                if (configRule.config.keycloakCleanup) {
                    ontrack.config.oidcSettings.deleteProvider(realm)
                }
            }

        } finally {
            if (configRule.config.keycloakCleanup) {
                realmClient.remove()
            }
        }
    }

    private static String createUser(
            UsersResource users,
            String firstName,
            List<String> roles,
            Map<String, String> roleIds
    ) {

        def username = uid("U")

        def user = new UserRepresentation()
        user.requiredActions = []
        user.username = username
        user.firstName = firstName
        user.lastName = username
        user.email = "${username}@nemerosa.net"
        user.emailVerified = true
        user.enabled = true
        users.create(user)

        def userId = users.search(username).first().id
        def userClient = users.get(userId)

        def roleMappingClient = userClient.roles().realmLevel()
        def roleRepresentations = roles.collect { String role ->
            def roleRep = new RoleRepresentation()
            roleRep.setId(roleIds[role])
            roleRep.setName(role)
            roleRep
        }
        roleMappingClient.add(roleRepresentations)

        def credentials = new CredentialRepresentation()
        credentials.type = CredentialRepresentation.PASSWORD
        credentials.temporary = false
        credentials.value = "secret"
        userClient.resetPassword(credentials)

        return username
    }

}
