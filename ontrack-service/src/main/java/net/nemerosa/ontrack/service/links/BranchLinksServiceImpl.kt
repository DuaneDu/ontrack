package net.nemerosa.ontrack.service.links

import net.nemerosa.ontrack.common.Time
import net.nemerosa.ontrack.extension.api.BranchLinksDecorationExtension
import net.nemerosa.ontrack.extension.api.ExtensionManager
import net.nemerosa.ontrack.model.buildfilter.BuildFilterService
import net.nemerosa.ontrack.model.links.*
import net.nemerosa.ontrack.model.metrics.MetricsExportService
import net.nemerosa.ontrack.model.settings.CachedSettingsService
import net.nemerosa.ontrack.model.structure.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Service
@Transactional(readOnly = true)
class BranchLinksServiceImpl(
    private val cachedSettingsService: CachedSettingsService,
    private val buildFilterService: BuildFilterService,
    private val structureService: StructureService,
    private val extensionManager: ExtensionManager,
    private val metricsExportService: MetricsExportService
) : BranchLinksService {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val providers: Collection<BranchLinksDecorationExtension> by lazy {
        extensionManager.getExtensions(BranchLinksDecorationExtension::class.java)
    }

    override fun getBranchLinks(branch: Branch, direction: BranchLinksDirection): BranchLinksNode {
        // Settings
        val settings: BranchLinksSettings = cachedSettingsService.getCachedSettings(BranchLinksSettings::class.java)
        // Graph in progress
        val graph = Node(branch.project)
        // Index of nodes per project
        val index = mutableMapOf<ID, Node>()
        index[branch.project.id] = graph
        // Processing stack
        val stack = MaxArrayDeque<Item>()
        // Index of branches which have already been processed
        val alreadyProcessed = mutableSetOf<ID>()

        // Starting the processing
        val start = System.currentTimeMillis()
        // Puts the current project on the stack
        stack.push(Item(0, branch))
        // Starts processing the stack
        while (stack.isNotEmpty()) {
            // Gets the current item
            val item = stack.pop()
            // Processing this branch
            alreadyProcessed += item.branch.id
            // Processing logging
            if (logger.isDebugEnabled) {
                logger.debug("item={}", item)
            }
            // Gets the corresponding node in the graph
            val node = index[item.branch.project.id]
                ?: error("Cannot find indexed node for ${item.branch.project.entityDisplayName}")
            // Gets the following branches using the current direction
            if (item.depth < settings.depth) {
                // Gets the next branches using the build links
                val nextBranches: List<Branch> = getNextBranches(item.branch, direction, settings.history, settings.maxLinksPerLevel)
                // For every next branch...
                nextBranches.forEach { nextBranch ->
                    // If not already processed
                    if (nextBranch.id !in alreadyProcessed) {
                        // Make sure we have a node for its project
                        val nextNode = index.getOrPut(nextBranch.project.id) {
                            Node(nextBranch.project)
                        }
                        // Links this node to the current one (if not already there)
                        if (node.projects.none { it.project.id == nextBranch.project.id }) {
                            node.projects += nextNode
                        }
                        // Adds the branch to the stack
                        stack.push(Item(item.depth + 1, nextBranch))
                    }
                }
            }
        }
        // Converts the graph in progress to a node
        val node = graphToNode(graph, direction)
        // Metrics
        val end = System.currentTimeMillis()
        metricsExportService.exportMetrics(
            metric = METRIC_BRANCH_GRAPH,
            tags = mapOf(
                "project" to branch.project.name,
                "branch" to branch.name
            ),
            fields = mapOf(
                "elapsedMs" to (end - start).toDouble(),
                "stack" to stack.max.toDouble(),
                "branches" to alreadyProcessed.size.toDouble()
            ),
            timestamp = Time.now()
        )
        // OK
        return node
    }

    override fun getBuildLinks(build: Build, direction: BranchLinksDirection): BranchLinksNode {
        // Gets the model for the branch
        val branchGraph = getBranchLinks(build.branch, direction)
        // Visits the whole graph and replace the node & edges as we go
        return populate(branchGraph, build, direction)
    }

    private fun populate(node: BranchLinksNode, build: Build, direction: BranchLinksDirection): BranchLinksNode {
        // Recomputes the edges
        val edges = node.edges.map { edge ->
            populate(edge, BranchLinksNode(node.project, build, node.edges), direction)
        }
        // OK
        return BranchLinksNode(
            node.project,
            build,
            edges
        )
    }

    private fun populate(edge: BranchLinksEdge, source: BranchLinksNode, direction: BranchLinksDirection): BranchLinksEdge {
        // Gets the target build if any
        val target = source.build?.let { getEdgeBuild(it, edge.linkedTo.project, direction) }
        // If no build, we return the edge as it is
        return if (target == null) {
            edge
        } else {
            val newLinkedNode = populate(edge.linkedTo, target, direction)
            val decorations = providers.mapNotNull { provider ->
                provider.getDecoration(source, newLinkedNode, direction)
            }
            BranchLinksEdge(
                direction = direction,
                linkedTo = newLinkedNode,
                decorations = decorations
            )
        }
    }

    private fun getNextBranches(branch: Branch, direction: BranchLinksDirection, history: Int, maxLinksPerLevel: Int): List<Branch> {
        // List of branches
        val branches = mutableMapOf<ID, Branch>()
        // Gets the first N builds for this branch
        val builds = getBuilds(branch, history)
        // For each build, collects the links
        builds.forEach { build ->
            val nextBuilds = getNextBuilds(build, direction, maxLinksPerLevel)
            // Collects the branches for each of those next builds
            nextBuilds.forEach { nextBuild ->
                branches[nextBuild.branch.id] = nextBuild.branch
            }
        }
        // OK
        return branches.values.toList()
    }

    private fun getBuilds(branch: Branch, history: Int): List<Build> =
        buildFilterService.standardFilterProviderData(history).build().filterBranchBuilds(branch)

    private fun getNextBuilds(build: Build, direction: BranchLinksDirection, maxLinksPerLevel: Int): List<Build> =
        when (direction) {
            BranchLinksDirection.USING ->
                structureService.getBuildsUsedBy(build, 0, maxLinksPerLevel).pageItems
            BranchLinksDirection.USED_BY ->
                structureService.getBuildsUsing(build, 0, maxLinksPerLevel).pageItems
        }

    /**
     * Gets the build target for the same project than the one defined by the branch graph.
     */
    private fun getEdgeBuild(build: Build, target: Project, direction: BranchLinksDirection): Build? =
        when (direction) {
            BranchLinksDirection.USING ->
                structureService.getBuildsUsedBy(build, 0, 1) {
                    it.project.id == target.id
                }.pageItems.firstOrNull()
            BranchLinksDirection.USED_BY ->
                structureService.getBuildsUsing(build, 0, 1) {
                    it.project.id == target.id
                }.pageItems.firstOrNull()
        }

    private class Item(
        val depth: Int,
        val branch: Branch
    ) {
        override fun toString(): String = "${branch.entityDisplayName} (depth = $depth)"
    }

    private class Node(
        val project: Project,
        val projects: MutableList<Node> = mutableListOf()
    )

    private fun graphToNode(node: Node, direction: BranchLinksDirection): BranchLinksNode =
        BranchLinksNode(
            project = node.project,
            build = null,
            edges = node.projects.map { child ->
                graphToNode(child, direction).run {
                    val decorations = providers.mapNotNull { provider ->
                        provider.getDecoration(BranchLinksNode(node.project, null, emptyList()), this, direction)
                    }
                    BranchLinksEdge(
                        direction = direction,
                        linkedTo = this,
                        decorations = decorations
                    )
                }
            }
        )

    private class MaxArrayDeque<T>: ArrayDeque<T>() {

        private val _max = AtomicInteger(0)

        override fun push(e: T) {
            super.push(e)
            _max.incrementAndGet()
        }

        val max: Int get() = _max.get()
    }

    companion object {
        const val METRIC_BRANCH_GRAPH = "ontrack_graph_branch"
    }

}