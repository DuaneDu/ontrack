package net.nemerosa.ontrack.graphql

import net.nemerosa.ontrack.common.getOrNull
import net.nemerosa.ontrack.json.isNullOrNullNode
import net.nemerosa.ontrack.model.structure.BranchFavouriteService
import net.nemerosa.ontrack.model.structure.NameDescription
import net.nemerosa.ontrack.model.structure.ValidationRunStatusID
import net.nemerosa.ontrack.test.TestUtils.uid
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import kotlin.test.*

class ProjectGraphQLIT : AbstractQLKTITSupport() {

    @Autowired
    private lateinit var branchFavouriteService: BranchFavouriteService

    @Test
    fun `Creating a project`() {
        asAdmin {
            val name = uid("P")
            val data = run("""
                mutation CreateProject(${'$'}name: String!) {
                    createProject(input: {name: ${'$'}name}) {
                        project {
                            id
                            name
                        }
                        errors {
                            message
                        }
                    }
                }
            """, mapOf("name" to name))
            // Checks the project has been created
            assertNotNull(
                    structureService.findProjectByName(name).getOrNull(),
                    "Project has been created"
            )
            // Checks the data
            val project = data["createProject"]["project"]
            assertTrue(project["id"].asInt() > 0, "ID is set")
            assertEquals(name, project["name"].asText(), "Name is OK")
            assertTrue(data["createProject"]["errors"].isNullOrNullNode(), "No error")
        }
    }

    @Test
    fun `Creating a project but name already exist`() {
        asAdmin {
            project {
                val data = run("""
                    mutation CreateProject(${'$'}name: String!) {
                        createProject(input: {name: ${'$'}name}) {
                            project {
                                id
                                name
                            }
                            errors {
                                message
                            }
                        }
                    }
                """, mapOf("name" to name))
                // Checks the errors
                val message = data["createProject"]["errors"][0]["message"].asText()
                assertEquals("Project name already exists: $name", message)
                assertTrue(data["createProject"]["project"].isNullOrNullNode(), "Project not returned")
            }
        }
    }

    @Test
    fun `Favourite branches for project`() {
        val account = doCreateAccount()
        project {
            branch {}
            val fav = branch {
                asConfigurableAccount(account).withView(this).execute {
                    branchFavouriteService.setBranchFavourite(this, true)
                }
            }
            // Gets the favourite branches in project
            val data = asConfigurableAccount(account).withView(this).call {
                run("""
                    {
                        projects(id: ${this.id}) {
                            branches(favourite: true) {
                                id
                            }
                        }
                    }
                """)
            }
            val branchIds: Set<Int> = data["projects"][0]["branches"].map { it["id"].asInt() }.toSet()
            assertEquals(
                    setOf(fav.id()),
                    branchIds
            )
        }
    }

    @Test
    fun `All projects`() {
        val p = doCreateProject()
        val data = run("{projects { id name }}")
        assertNotNull(data["projects"].find { it["name"].asText() == p.name }) {
            assertEquals(p.id(), it["id"].asInt())
        }
    }

    @Test
    fun `Project by name when not authorized must throw an authentication exception`() {
        // Creates a project
        val project = doCreateProject()
        // Looks for this project by name, with a not authorized user
        assertFailsWith(AccessDeniedException::class, "Access denied") {
            withNoGrantViewToAll {
                asUser().call {
                    run("""{
                |  projects(name: "${project.name}") {
                |    id
                |  }
                |}""".trimMargin())
                }
            }
        }
    }

    @Test
    fun `Last promoted build`() {
        // Creating a promotion level
        val pl = doCreatePromotionLevel()
        // Creating a first promoted build
        val build1 = doCreateBuild(pl.branch, NameDescription.nd("1", ""))
        doPromote(build1, pl, "One")
        // Creating a second promoted build
        val build2 = doCreateBuild(pl.branch, NameDescription.nd("2", ""))
        doPromote(build2, pl, "Two")
        // Run a GraphQL query at project level and gets the last promotion run
        val data = run("""{
            |   projects(id: ${pl.project.id}) {
            |      branches {
            |          promotionLevels {
            |              name
            |              promotionRuns(first: 1) {
            |                build {
            |                  name
            |                }
            |              }
            |          }
            |      }
            |   }
            |}
        """.trimMargin())
        // Checks that the build associated with the promotion is the last one
        val plNode = data["projects"][0]["branches"][0]["promotionLevels"][0]
        assertEquals(pl.name, plNode["name"].asText())
        val runNodes = plNode["promotionRuns"]
        assertEquals(1, runNodes.size())
        val build = runNodes[0]["build"]
        assertEquals(build2.name, build["name"].asText())
    }

    @Test
    fun `Validation run statuses for a run for a validation stamp`() {
        project {
            branch {
                val vs = validationStamp()
                build("1") {
                    validate(vs, ValidationRunStatusID.STATUS_FAILED, description = "Validation failed").apply {
                        validationStatus(ValidationRunStatusID.STATUS_INVESTIGATING, "Investigating")
                        validationStatus(ValidationRunStatusID.STATUS_EXPLAINED, "Explained")
                    }
                    val data = run("""{
                        projects (id: ${project.id}) {
                            branches (name: "${branch.name}") {
                                validationStamps {
                                    name
                                    validationRuns {
                                        validationRunStatuses {
                                            statusID {
                                                id
                                            }
                                            description
                                        }
                                    }
                                }
                            }
                        }
                    }""")
                    val validationRunStatuses = data["projects"][0]["branches"][0]["validationStamps"][0]["validationRuns"][0]["validationRunStatuses"]
                    assertEquals(
                            listOf("EXPLAINED", "INVESTIGATING", "FAILED"),
                            validationRunStatuses.map { it["statusID"]["id"].asText() }
                    )
                    assertEquals(
                            listOf("Explained", "Investigating", "Validation failed"),
                            validationRunStatuses.map { it["description"].asText() }
                    )
                }
            }
        }
    }

}