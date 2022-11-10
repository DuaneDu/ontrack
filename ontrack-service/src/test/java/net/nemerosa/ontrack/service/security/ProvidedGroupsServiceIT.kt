package net.nemerosa.ontrack.service.security

import net.nemerosa.ontrack.it.AbstractDSLTestSupport
import net.nemerosa.ontrack.model.security.AuthenticationSource
import net.nemerosa.ontrack.model.security.ProvidedGroupsService
import net.nemerosa.ontrack.test.TestUtils.uid
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProvidedGroupsServiceIT : AbstractDSLTestSupport() {

    @Autowired
    private lateinit var providedGroupsService: ProvidedGroupsService

    @Test
    fun `Saving and retrieving the groups`() {
        val account = doCreateAccount()
        asAdmin {
            // No group by default
            assertTrue(providedGroupsService.getProvidedGroups(account.id(), AuthenticationSource.none()).isEmpty())
            // Saves some groups
            providedGroupsService.saveProvidedGroups(account.id(), AuthenticationSource.none(), setOf("admin", "user"))
            assertEquals(
                setOf("admin", "user"),
                providedGroupsService.getProvidedGroups(account.id(), AuthenticationSource.none())
            )
            // Changes the groups
            providedGroupsService.saveProvidedGroups(
                account.id(),
                AuthenticationSource.none(),
                setOf("user", "manager")
            )
            assertEquals(
                setOf("user", "manager"),
                providedGroupsService.getProvidedGroups(account.id(), AuthenticationSource.none())
            )
        }
    }

    @Test
    fun `Filtering out group names which are too long`() {
        val account = doCreateAccount()
        asAdmin {
            val longGroupName = "g".repeat(81)
            providedGroupsService.saveProvidedGroups(
                account.id(),
                AuthenticationSource.none(),
                setOf(longGroupName, "user")
            )
            assertEquals(
                setOf("user"),
                providedGroupsService.getProvidedGroups(account.id(), AuthenticationSource.none())
            )
        }
    }

    @Test
    fun `Getting distinct suggested groups`() {
        val groupPrefix = uid("group-")
        val accounts = (0..5).map { doCreateAccount() }
        asAdmin {
            // Saving the provided groups
            (0..2).forEach { no ->
                val account = accounts[no]
                providedGroupsService.saveProvidedGroups(
                    account.id(),
                    AuthenticationSource.none(),
                    setOf("$groupPrefix-1")
                )
            }
            (3..4).forEach { no ->
                val account = accounts[no]
                providedGroupsService.saveProvidedGroups(
                    account.id(),
                    AuthenticationSource.none(),
                    setOf("$groupPrefix-2")
                )
            }
            (5..5).forEach { no ->
                val account = accounts[no]
                providedGroupsService.saveProvidedGroups(
                    account.id(),
                    AuthenticationSource.none(),
                    setOf("$groupPrefix-3")
                )
            }
            // Getting the suggested groups for this prefix
            assertEquals(
                (1..3).map { "$groupPrefix-$it" },
                providedGroupsService.getSuggestedGroups(AuthenticationSource.none(), groupPrefix)
            )
        }
    }

}