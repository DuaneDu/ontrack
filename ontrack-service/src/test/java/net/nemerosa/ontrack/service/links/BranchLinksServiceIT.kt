package net.nemerosa.ontrack.service.links

import net.nemerosa.ontrack.it.AbstractDSLTestSupport
import net.nemerosa.ontrack.model.links.BranchLinksDirection
import net.nemerosa.ontrack.model.links.BranchLinksNode
import net.nemerosa.ontrack.model.links.BranchLinksService
import net.nemerosa.ontrack.model.links.BranchLinksSettings
import net.nemerosa.ontrack.model.structure.Branch
import net.nemerosa.ontrack.model.structure.Build
import net.nemerosa.ontrack.model.structure.NameDescription
import net.nemerosa.ontrack.model.structure.Project
import net.nemerosa.ontrack.test.TestUtils.uid
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.*

class BranchLinksServiceIT : AbstractDSLTestSupport() {

    @Autowired
    private lateinit var branchLinksService: BranchLinksService

    @Test
    fun `No build links makes for one single node for the abstract graph`() {
        val build = build("build")
        asUserWithView(build) {
            val node = branchLinksService.getBranchLinks(build.branch, BranchLinksDirection.USING)
            assertSame(build.branch, node.branch)
            assertNull(node.build, "No build")
            assertTrue(node.edges.isEmpty(), "No edge")
        }
    }

    @Test
    fun `Abstract graph for regular links in the 'using' direction`() {
        val library = build("library")
        val component = build("component")
        val project = build("project")
        asAdmin {
            project.linkTo(component)
            component.linkTo(library)
        }

        asUserWithView(project, component, library) {
            val projectNode = branchLinksService.getBranchLinks(project.branch, BranchLinksDirection.USING)
            assertEquals(project.branch.id, projectNode.branch.id)
            assertNull(projectNode.build, "Abstract graph nodes don't contain builds")
            assertEquals(1, projectNode.edges.size)

            val projectEdge = projectNode.edges.first()
            assertEquals(BranchLinksDirection.USING, projectEdge.direction)

            val componentNode = projectEdge.linkedTo
            assertEquals(component.branch.id, componentNode.branch.id)
            assertNull(componentNode.build, "Abstract graph nodes don't contain builds")
            assertEquals(1, componentNode.edges.size)

            val componentEdge = componentNode.edges.first()
            assertEquals(BranchLinksDirection.USING, componentEdge.direction)

            val libraryNode = componentEdge.linkedTo
            assertEquals(library.branch.id, libraryNode.branch.id)
            assertNull(libraryNode.build, "Abstract graph nodes don't contain builds")
            assertEquals(0, libraryNode.edges.size)
        }
    }

    @Test
    fun `Abstract graph for regular links in the 'usedBy' direction`() {
        val library = build("library")
        val component = build("component")
        val project = build("project")
        asAdmin {
            project.linkTo(component)
            component.linkTo(library)
        }

        asUserWithView(project, component, library) {
            val libraryNode = branchLinksService.getBranchLinks(library.branch, BranchLinksDirection.USED_BY)
            assertEquals(library.branch.id, libraryNode.branch.id)
            assertNull(libraryNode.build, "Abstract graph nodes don't contain builds")
            assertEquals(1, libraryNode.edges.size)

            val componentEdge = libraryNode.edges.first()
            assertEquals(BranchLinksDirection.USED_BY, componentEdge.direction)

            val componentNode = componentEdge.linkedTo
            assertEquals(component.branch.id, componentNode.branch.id)
            assertNull(componentNode.build, "Abstract graph nodes don't contain builds")
            assertEquals(1, componentNode.edges.size)

            val projectEdge = componentNode.edges.first()
            assertEquals(BranchLinksDirection.USED_BY, projectEdge.direction)

            val projectNode = projectEdge.linkedTo
            assertEquals(project.branch.id, projectNode.branch.id)
            assertNull(projectNode.build, "Abstract graph nodes don't contain builds")
            assertEquals(0, projectNode.edges.size)
        }
    }

    @Test
    fun `Depth limit`() {
        withBranchLinkSettings(depth = 1) {
            val library = build("library")
            val component = build("component")
            val project = build("project")
            asAdmin {
                project.linkTo(component)
                component.linkTo(library)
            }

            val libraryNode = branchLinksService.getBranchLinks(library.branch, BranchLinksDirection.USED_BY)
            assertEquals(library.branch.id, libraryNode.branch.id)
            assertNull(libraryNode.build, "Abstract graph nodes don't contain builds")
            assertEquals(1, libraryNode.edges.size)

            val componentEdge = libraryNode.edges.first()
            assertEquals(BranchLinksDirection.USED_BY, componentEdge.direction)

            val componentNode = componentEdge.linkedTo
            assertEquals(component.branch.id, componentNode.branch.id)
            assertNull(componentNode.build, "Abstract graph nodes don't contain builds")
            assertEquals(0, componentNode.edges.size) // We don't go further because depth = 1
        }
    }

    @Test
    fun `History must be taken into account`() {
        val a = build()
        val b = build()
        val c = build()
        project {
            val branch = branch {
                build {
                    linkTo(a)
                }
                build {
                    linkTo(b)
                }
                build {
                    linkTo(c)
                }
            }
            val node = branchLinksService.getBranchLinks(branch, BranchLinksDirection.USING)
            assertEquals(
                listOf(a, b, c).map { it.branch.name }.toSet(),
                node.edges.map { it.linkedTo.branch.name }.toSet()
            )
        }
    }

    @Test
    fun `History limit`() {
        val a = build()
        val b = build()
        val c = build()
        project {
            val branch = branch {
                build {
                    linkTo(a)
                }
                build {
                    linkTo(b)
                }
                build {
                    linkTo(c)
                }
            }
            withBranchLinkSettings(history = 2) {
                val node = branchLinksService.getBranchLinks(branch, BranchLinksDirection.USING)
                assertEquals(
                    listOf(b, c).map { it.branch.name }.toSet(), // History = 2, 3rd build's not taken
                    node.edges.map { it.linkedTo.branch.name }.toSet()
                )
            }
        }
    }

    @Test
    fun `Max links per level`() {
        val dependencies = (1..10).map { build("$it") }
        project {
            val branch = branch {
                build {
                    dependencies.forEach { linkTo(it) }
                }
            }
            withBranchLinkSettings(maxLinksPerLevel = 5) {
                val node = branchLinksService.getBranchLinks(branch, BranchLinksDirection.USING)
                assertEquals(
                    dependencies.takeLast(5).map { it.branch.name }.toSet(), // Taking only the five first links
                    node.edges.map { it.linkedTo.branch.name }.toSet()
                )
            }
        }
    }

    @Test
    fun `One layer branch graph`() {
        withLinks {
            build("component", 1) linkTo build("library", 1)

            assertBranchLinks(branch("component"), BranchLinksDirection.USING) {
                assertLinkedTo(branch("library"))
            }

            assertBranchLinks(branch("library"), BranchLinksDirection.USED_BY) {
                assertLinkedTo(branch("component"))
            }
        }
    }

    @Test
    fun `Deep branch graph`() {
        withLinks {

            build("chart", 1) linkTo build("aggregator", 2)

            build("aggregator", 1) linkTo build("project", 1)
            build("aggregator", 2) linkTo build("project", 3)

            build("project", 1) linkTo build("component", 1)
            build("project", 1) linkTo build("component", 1)
            build("project", 2) linkTo build("component", 2)
            build("project", 3) linkTo build("component", 2)
            build("project", 3) linkTo build("other-library", 1)

            build("component", 1) linkTo build("library", 1)
            build("component", 2) linkTo build("library", 3)

            assertBranchLinks(branch("chart"), BranchLinksDirection.USING) {
                assertLinkedTo(branch("aggregator")) {
                    assertLinkedTo(branch("project")) {
                        assertLinkedTo(branch("component")) {
                            assertLinkedTo(branch("library"))
                        }
                        assertLinkedTo(branch("other-library"))
                    }
                }
            }

            assertBranchLinks(branch("library"), BranchLinksDirection.USED_BY) {
                assertLinkedTo(branch("component")) {
                    assertLinkedTo(branch("project")) {
                        assertLinkedTo(branch("aggregator")) {
                            assertLinkedTo(branch("chart"))
                        }
                    }
                }
            }

            assertBranchLinks(branch("other-library"), BranchLinksDirection.USED_BY) {
                assertLinkedTo(branch("project")) {
                    assertLinkedTo(branch("aggregator")) {
                        assertLinkedTo(branch("chart"))
                    }
                }
            }
        }
    }

    @Test
    fun `Build graph with one layer`() {
        withLinks {

            build("component", 1) linkTo build("library", 1)

            assertBuildLinks(build("component", 1), BranchLinksDirection.USING) {
                assertLinkedTo(build("library", 1))
            }
        }
    }

    @Test
    fun `Build graph with one unfilled layer`() {
        withLinks {

            build("component", 1) linkTo build("library", 1)
            build("component", 2)

            assertBuildLinks(build("component", 2), BranchLinksDirection.USING) {
                assertLinkedToNoBuild(branch("library"))
            }

            // Filling the gap
            build("component", 2) linkTo build("library", 1)
            assertBuildLinks(build("component", 2), BranchLinksDirection.USING) {
                assertLinkedTo(build("library", 1))
            }
        }
    }

    @Test
    fun `Build graph with two layers`() {
        withLinks {
            build("project", 1) linkTo build("component", 1)
            build("component", 1) linkTo build("library", 1)
            assertBuildLinks(build("project", 1), BranchLinksDirection.USING) {
                assertLinkedTo(build("component", 1)) {
                    assertLinkedTo(build("library", 1))
                }
            }
        }
    }

    @Test
    fun `Build graph with two layers with progressive fill`() {
        withLinks {
            build("project", 1) linkTo build("component", 1)
            build("component", 1) linkTo build("library", 1)

            val library = build("library", 2)
            assertBuildLinks(library, BranchLinksDirection.USED_BY) {
                assertLinkedToNoBuild(branch("component")) {
                    assertLinkedToNoBuild(branch("project"))
                }
            }

            val component = build("component", 2)
            assertBuildLinks(library, BranchLinksDirection.USED_BY) {
                assertLinkedToNoBuild(branch("component")) {
                    assertLinkedToNoBuild(branch("project"))
                }
            }

            component linkTo library
            assertBuildLinks(library, BranchLinksDirection.USED_BY) {
                assertLinkedTo(component) {
                    assertLinkedToNoBuild(branch("project"))
                }
            }

            val project = build("project", 2)
            assertBuildLinks(library, BranchLinksDirection.USED_BY) {
                assertLinkedTo(component) {
                    assertLinkedToNoBuild(branch("project"))
                }
            }

            project linkTo component
            assertBuildLinks(library, BranchLinksDirection.USED_BY) {
                assertLinkedTo(component) {
                    assertLinkedTo(project)
                }
            }
        }
    }

    private fun withLinks(
        code: WithLinksContext.() -> Unit
    ) {
        asAdmin {
            WithLinksContext().code()
        }
    }

    private inner class WithLinksContext {

        val project = mutableMapOf<String, Project>()
        val branches = mutableMapOf<String, Branch>()
        val builds = mutableMapOf<Pair<String, Int>, Build>()

        fun project(id: String): Project =
            project.getOrPut(id) {
                project(NameDescription.nd(id + uid("x"), ""))
            }

        fun branch(id: String): Branch =
            branches.getOrPut(id) {
                project(id).branch("main")
            }

        fun build(id: String, no: Int): Build =
            builds.getOrPut(id to no) {
                branch(id).build("$id-$no")
            }

        fun assertBranchLinks(branch: Branch, direction: BranchLinksDirection, code: BranchNodeTestContext.() -> Unit) {
            val node = branchLinksService.getBranchLinks(branch, direction)
            assertEquals(branch.id, node.branch.id, "Node on the same branch")
            BranchNodeTestContext(node).code()
        }

        fun assertBuildLinks(build: Build, direction: BranchLinksDirection, code: BuildNodeTestContext.() -> Unit) {
            val node = branchLinksService.getBuildLinks(build, direction)
            assertEquals(build.branch.id, node.branch.id, "Node on the same branch")
            assertEquals(build.id, node.build?.id, "Node on the same build")
            BuildNodeTestContext(node).code()
        }

    }

    private class BranchNodeTestContext(
        private val node: BranchLinksNode
    ) {
        fun assertLinkedTo(target: Branch, code: BranchNodeTestContext.() -> Unit = {}) {
            val edge = node.edges.find { it.linkedTo.branch.id == target.id }
            assertNotNull(edge,
                "Cannot find any link between ${node.branch.entityDisplayName} and ${target.entityDisplayName}") {
                BranchNodeTestContext(it.linkedTo).code()
            }
        }
    }

    private class BuildNodeTestContext(
        private val node: BranchLinksNode
    ) {
        fun assertLinkedTo(target: Build, code: BuildNodeTestContext.() -> Unit = {}) {
            val edge = node.edges.find { it.linkedTo.branch.id == target.branch.id }
            assertNotNull(edge,
                "Cannot find any link between ${node.branch.entityDisplayName} and ${target.branch.entityDisplayName}") {
                // Checks the build
                assertEquals(target.id, it.linkedTo.build?.id, "Expected ${target.entityDisplayName} under node ${target.branch.entityDisplayName}")
                // Going on
                BuildNodeTestContext(it.linkedTo).code()
            }
        }

        fun assertLinkedToNoBuild(target: Branch, code: BuildNodeTestContext.() -> Unit = {}) {
            val edge = node.edges.find { it.linkedTo.branch.id == target.id }
            assertNotNull(edge,
                "Cannot find any link between ${node.branch.entityDisplayName} and ${target.entityDisplayName}") {
                assertNull(it.linkedTo.build, "Node for ${target.entityDisplayName} has no build")
                // Going on
                BuildNodeTestContext(it.linkedTo).code()
            }
        }
    }

    private fun build(name: String = "build") = project<Build> {
        branch<Build> {
            build(name)
        }
    }

    private fun withBranchLinkSettings(
        depth: Int = BranchLinksSettings.DEFAULT_DEPTH,
        history: Int = BranchLinksSettings.DEFAULT_HISTORY,
        maxLinksPerLevel: Int = BranchLinksSettings.DEFAULT_MAX_LINKS_PER_LEVEL,
        code: () -> Unit
    ) {
        asAdmin {
            withSettings<BranchLinksSettings> {
                settingsManagerService.saveSettings(
                    BranchLinksSettings(
                        depth = depth,
                        history = history,
                        maxLinksPerLevel = maxLinksPerLevel
                    )
                )
                code()
            }
        }
    }

}