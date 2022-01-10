package net.nemerosa.ontrack.service

import net.nemerosa.ontrack.it.AbstractDSLTestJUnit4Support
import net.nemerosa.ontrack.model.structure.BranchDisplayNameService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class BranchDisplayNameServiceIT : AbstractDSLTestJUnit4Support() {

    @Autowired
    private lateinit var branchDisplayNameService: BranchDisplayNameService

    @Test
    fun `Branch name by default`() {
        project {
            branch("release-1.0") {
                assertEquals(
                        "release-1.0",
                        branchDisplayNameService.getBranchDisplayName(this)
                )
            }
        }
    }

}