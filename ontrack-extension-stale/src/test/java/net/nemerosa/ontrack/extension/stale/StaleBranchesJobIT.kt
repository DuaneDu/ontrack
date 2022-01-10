package net.nemerosa.ontrack.extension.stale

import net.nemerosa.ontrack.common.Time
import net.nemerosa.ontrack.it.AbstractDSLTestJUnit4Support
import net.nemerosa.ontrack.job.JobRunListener
import net.nemerosa.ontrack.model.structure.Project
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StaleBranchesJobIT : AbstractDSLTestJUnit4Support() {

    @Autowired
    private lateinit var staleJobService: StaleJobService

    @Test
    fun `Deleting a branch using last build time`() {
        project {
            staleBranches(disabling = 5, deleting = 10)
            branch {
                build {
                    updateBuildSignature(time = Time.now().minusDays(16))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNull(structureService.findBranchByID(id), "Branch has been deleted")
            }
        }
    }

    @Test
    fun `Disabling only a branch using last build time when deletion time is not set`() {
        project {
            staleBranches(disabling = 5, deleting = null)
            branch {
                build {
                    updateBuildSignature(time = Time.now().minusDays(11))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNotNull(structureService.findBranchByID(id), "Branch has not been deleted") {
                    assertTrue(it.isDisabled, "Branch has been disabled")
                }
            }
        }
    }

    @Test
    fun `Disabling a branch using last build time`() {
        project {
            staleBranches(disabling = 5, deleting = 10)
            branch {
                build {
                    updateBuildSignature(time = Time.now().minusDays(6))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNotNull(structureService.findBranchByID(id), "Branch has not been deleted") {
                    assertTrue(it.isDisabled, "Branch has been disabled")
                }
            }
        }
    }

    @Test
    fun `Not touching a branch using last build time`() {
        project {
            staleBranches(disabling = 5, deleting = 10)
            branch {
                build {
                    updateBuildSignature(time = Time.now().minusDays(4))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNotNull(structureService.findBranchByID(id), "Branch has not been deleted") {
                    assertFalse(it.isDisabled, "Branch has been disabled")
                }
            }
        }
    }

    @Test
    fun `Deleting a branch using branch creation time`() {
        project {
            staleBranches(disabling = 5, deleting = 10)
            branch {
                updateBranchSignature(time = Time.now().minusDays(16))
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNull(structureService.findBranchByID(id), "Branch has been deleted")
            }
        }
    }

    @Test
    fun `Disabling a branch using branch creation time`() {
        project {
            staleBranches(disabling = 5, deleting = 10)
            branch {
                updateBranchSignature(time = Time.now().minusDays(6))
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNotNull(structureService.findBranchByID(id), "Branch has not been deleted") {
                    assertTrue(it.isDisabled, "Branch has been disabled")
                }
            }
        }
    }

    @Test
    fun `Not disabling a branch because of promotions`() {
        project {
            branch {
                updateBranchSignature(time = Time.now().minusDays(7))
                val pl = promotionLevel()
                staleBranches(disabling = 5, deleting = 10, promotionsToKeep = listOf(pl.name))
                build {
                    promote(pl)
                    updateBuildSignature(time = Time.now().minusDays(6))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNotNull(structureService.findBranchByID(id), "Branch has not been deleted") {
                    assertFalse(it.isDisabled, "Branch has not been disabled")
                }
            }
        }
    }

    @Test
    fun `Disabling a branch even if promoted when not configured`() {
        project {
            branch {
                updateBranchSignature(time = Time.now().minusDays(7))
                val pl = promotionLevel()
                staleBranches(disabling = 5, deleting = 10, promotionsToKeep = null)
                build {
                    promote(pl)
                    updateBuildSignature(time = Time.now().minusDays(6))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNotNull(structureService.findBranchByID(id), "Branch has not been deleted") {
                    assertTrue(it.isDisabled, "Branch has been disabled")
                }
            }
        }
    }

    @Test
    fun `Not deleting a branch because of promotions`() {
        project {
            branch {
                updateBranchSignature(time = Time.now().minusDays(12))
                val pl = promotionLevel()
                staleBranches(disabling = 5, deleting = 10, promotionsToKeep = listOf(pl.name))
                build {
                    promote(pl)
                    updateBuildSignature(time = Time.now().minusDays(11))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNotNull(structureService.findBranchByID(id), "Branch has not been deleted") {
                    assertFalse(it.isDisabled, "Branch has not been disabled")
                }
            }
        }
    }

    @Test
    fun `Not deleting a branch because of includes rule`() {
        project {
            branch("release-1.0") {
                updateBranchSignature(time = Time.now().minusDays(20))
                val pl = promotionLevel()
                staleBranches(disabling = 5, deleting = 10, includes = "release-.*")
                build {
                    promote(pl)
                    updateBuildSignature(time = Time.now().minusDays(18))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNotNull(structureService.findBranchByID(id), "Branch has not been deleted") {
                    assertFalse(it.isDisabled, "Branch has not been disabled")
                }
            }
        }
    }

    @Test
    fun `Not deleting a branch because of includes rule and excludes rule`() {
        project {
            branch("release-2.0") {
                updateBranchSignature(time = Time.now().minusDays(20))
                val pl = promotionLevel()
                staleBranches(disabling = 5, deleting = 10, includes = "release-.*", excludes = "release-1.*")
                build {
                    promote(pl)
                    updateBuildSignature(time = Time.now().minusDays(18))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNotNull(structureService.findBranchByID(id), "Branch has not been deleted") {
                    assertFalse(it.isDisabled, "Branch has not been disabled")
                }
            }
        }
    }

    @Test
    fun `Deleting a branch because of includes rule and excludes rule`() {
        project {
            branch("release-1.0") {
                updateBranchSignature(time = Time.now().minusDays(20))
                val pl = promotionLevel()
                staleBranches(disabling = 5, deleting = 10, includes = "release-.*", excludes = "release-1.*")
                build {
                    promote(pl)
                    updateBuildSignature(time = Time.now().minusDays(18))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNull(structureService.findBranchByID(id), "Branch has been deleted")
            }
        }
    }

    @Test
    fun `Deleting a branch even if promoted when not configured`() {
        project {
            branch {
                updateBranchSignature(time = Time.now().minusDays(6))
                val pl = promotionLevel()
                staleBranches(disabling = 5, deleting = 10, promotionsToKeep = null)
                build {
                    promote(pl)
                    updateBuildSignature(time = Time.now().minusDays(16))
                }
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNull(structureService.findBranchByID(id), "Branch has been deleted")
            }
        }
    }

    @Test
    fun `Not touching a branch using branch creation time`() {
        project {
            staleBranches(disabling = 5, deleting = 10)
            branch {
                updateBranchSignature(time = Time.now().minusDays(4))
                staleJobService.detectAndManageStaleBranches(JobRunListener.out(), project)
                assertNotNull(structureService.findBranchByID(id), "Branch has not been deleted") {
                    assertFalse(it.isDisabled, "Branch has not been disabled")
                }
            }
        }
    }

    private fun Project.staleBranches(
        disabling: Int = 0,
        deleting: Int? = null,
        promotionsToKeep: List<String>? = null,
        includes: String? = null,
        excludes: String? = null,
    ) {
        setProperty(project, StalePropertyType::class.java, StaleProperty(
            disablingDuration = disabling,
            deletingDuration = deleting,
            promotionsToKeep = promotionsToKeep,
            includes = includes,
            excludes = excludes,
        ))
    }

}
