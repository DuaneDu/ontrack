package net.nemerosa.ontrack.extension.av.processing

import net.nemerosa.ontrack.common.replaceGroup
import net.nemerosa.ontrack.extension.av.config.AutoApprovalMode
import net.nemerosa.ontrack.extension.av.config.AutoVersioningSourceConfig
import net.nemerosa.ontrack.extension.av.config.AutoVersioningTargetFileService
import net.nemerosa.ontrack.extension.av.dispatcher.AutoVersioningOrder
import net.nemerosa.ontrack.extension.av.postprocessing.PostProcessing
import net.nemerosa.ontrack.extension.av.postprocessing.PostProcessingNotFoundException
import net.nemerosa.ontrack.extension.av.postprocessing.PostProcessingRegistry
import net.nemerosa.ontrack.extension.av.properties.FilePropertyType
import net.nemerosa.ontrack.extension.scm.service.SCMDetector
import net.nemerosa.ontrack.extension.scm.service.uploadLines
import net.nemerosa.ontrack.model.structure.NameDescription
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AutoVersioningProcessingServiceImpl(
    private val scmDetector: SCMDetector,
    private val autoVersioningTargetFileService: AutoVersioningTargetFileService,
    private val postProcessingRegistry: PostProcessingRegistry,
) : AutoVersioningProcessingService {

    private val logger: Logger = LoggerFactory.getLogger(AutoVersioningProcessingServiceImpl::class.java)

    override fun process(order: AutoVersioningOrder): AutoVersioningProcessingOutcome {
        logger.info("Processing auto versioning order: {}", order)
        // TODO Audit
        val branch = order.branch
        // Gets the SCM configuration for the project
        val scm = scmDetector.getSCM(branch.project)
        val scmBranch: String? = scm?.getSCMBranch(branch)
        if (scm != null && scmBranch != null) {
            // Gets the clone URL for the repository
            val repositoryUri = scm.repositoryURI
            // Gets the repository credentials ID to use on post processing
            // TODO val repositoryCredentials = scm.repositoryCredentialsId
            // Sanitization of the version (for the branch name)
            val sanitizedVersion: String = NameDescription.escapeName(order.targetVersion)
            // Name of the upgrade branch
            val upgradeBranch = AutoVersioningSourceConfig.getUpgradeBranch(
                upgradeBranchPattern = order.upgradeBranchPattern,
                project = order.sourceProject,
                version = sanitizedVersion,
                branch = scmBranch,
                branchHash = true // Target branch name as a hash (to avoid too long names)
            )
            // Commit ID of the new branch (not null when target branch is actually created)
            val commitId: String by lazy {
                // Creates an upgrade branch and gets its commit ID
                try {
                    logger.info(
                        "Processing auto versioning order creating branch {}: {}",
                        upgradeBranch,
                        order
                    )
                    // TODO Audit
                    // Creating the branch
                    scm.createBranch(scmBranch, upgradeBranch)
                } catch (e: Exception) {
                    // TODO Notification of error
                    throw e
                }
            }
            // For each target path
            val targetPathUpdated: List<Boolean> = try {
                order.targetPaths.map { targetPath ->
                    // Gets the content of the target file
                    val lines = scm.download(scmBranch, targetPath)
                        ?.toString()
                        ?.lines()
                        ?: emptyList()
                    // Gets the current version in this file
                    val currentVersion: String = autoVersioningTargetFileService.readVersion(order, lines)
                        ?: throw AutoVersioningVersionNotFoundException(targetPath)
                    // If different version
                    if (currentVersion != order.targetVersion) {
                        // Changes the version
                        val updatedLines = order.replaceVersion(lines)
                        // Pushes the change to the branch
                        logger.info(
                            "Processing auto versioning order pushing upgrade to {}/{}: {}",
                            upgradeBranch,
                            targetPath,
                            order
                        )
                        // TODO Audit
                        // Uploads of the file content
                        scm.uploadLines(upgradeBranch, commitId, targetPath, updatedLines)
                        // Post processing
                        if (!order.postProcessing.isNullOrBlank()) {
                            // Gets the post processor
                            val postProcessing = postProcessingRegistry.getPostProcessingById<Any>(order.postProcessing)
                            // If processing cannot be found, we consider this an error
                            if (postProcessing == null) {
                                throw PostProcessingNotFoundException(order.postProcessing)
                            }
                            // Launching the post processing
                            else {
                                logger.info("Processing auto versioning order launching post processing: {}", order)
                                // TODO autoVersioningAuditService.onPostProcessingStart(prCreationOrder, upgradeBranch)
                                measureAndLaunchPostProcessing(
                                    postProcessing,
                                    order,
                                    repositoryUri,
                                    upgradeBranch
                                )
                                logger.info("Processing auto versioning order end of post processing: {}", order)
                                // TODO autoVersioningAuditService.onPostProcessingEnd(prCreationOrder, upgradeBranch)
                            }
                        }
                        // Changed
                        true
                    } else {
                        // Not changed
                        false
                    }
                }
            } catch (e: Exception) {
                // TODO Notification
                throw e
            }

            // If at least one path updated
            if (targetPathUpdated.any { it }) {

                // Creates a PR with auto approval
                try {
                    logger.info("Processing auto versioning order creating PR: {}", order)
                    // TODO Audit
                    // PR creation
                    val pr = scm.createPR(
                        from = upgradeBranch,
                        to = scmBranch,
                        title = "Upgrade of ${order.sourceProject} to version ${order.targetVersion}",
                        // Change log as description?
                        description = "Upgrade of ${order.sourceProject} to version ${order.targetVersion}",
                        // Auto approval
                        autoApproval = order.autoApproval,
                        // Remote auto merge?
                        remoteAutoMerge = (order.autoApprovalMode == AutoApprovalMode.SCM)
                    )
                    // If auto approval mode = CLIENT and PR is not merged, we had a timeout
                    logger.info("Processing auto versioning order end of PR process: {}", order)
                    if (order.autoApproval) {
                        when (order.autoApprovalMode) {
                            AutoApprovalMode.SCM -> TODO("Audit only")
                            AutoApprovalMode.CLIENT -> if (!pr.merged) {
                                logger.info("Processing auto versioning order PR timed out: {}", order)
                                // TODO autoVersioningAuditService.onPRTimeout(
                                // TODO Notification
                            } else {
                                // TODO Audit
                            }
                        }
                    } else {
                        // TODO Audit
                    }
                } catch (e: Exception) {
                    // TODO Notification
                    throw e
                }

                // OK
                return AutoVersioningProcessingOutcome.CREATED

            } else {
                logger.info("Processing auto versioning order same version: {}", order)
                // TODO autoVersioningAuditService.onProcessingAborted(prCreationOrder, "Same version")
                return AutoVersioningProcessingOutcome.SAME_VERSION
            }
        } else {
            logger.info("Processing auto versioning order no config: {}", order)
            // TODO autoVersioningAuditService.onProcessingAborted(prCreationOrder, "Target branch is not configured")
            return AutoVersioningProcessingOutcome.NO_CONFIG
        }
    }

    private fun <T> measureAndLaunchPostProcessing(
        postProcessing: PostProcessing<T>,
        order: AutoVersioningOrder,
        repositoryUri: String,
        upgradeBranch: String,
    ) {
        val tags = arrayOf(
            "sourceProject" to order.sourceProject,
            "targetProject" to order.branch.project.name,
            "targetBranch" to order.branch.name,
            "postProcessingId" to postProcessing.id
        )
        // Count
        // meterRegistry.increment(PRCreationMetrics.METRIC_ONTRACK_COLLIBRA_PRCREATION_POST_PROCESSING_COUNT, *tags)
        try {
            // Timing
            // meterRegistry.time(
            //     PRCreationMetrics.METRIC_ONTRACK_COLLIBRA_PRCREATION_POST_PROCESSING_TIME, *tags) {
            launchPostProcessing(
                postProcessing,
                order,
                repositoryUri,
                upgradeBranch
            )
            // }
            // Success
            // meterRegistry.increment(PRCreationMetrics.METRIC_ONTRACK_COLLIBRA_PRCREATION_POST_PROCESSING_SUCCESS_COUNT, *tags)
        } catch (ex: Exception) {
            // Metric for the error
            // meterRegistry.increment(PRCreationMetrics.METRIC_ONTRACK_COLLIBRA_PRCREATION_POST_PROCESSING_ERROR_COUNT, *tags)
            // Going on with error processing
            throw ex
        }
    }

    private fun <T> launchPostProcessing(
        postProcessing: PostProcessing<T>,
        order: AutoVersioningOrder,
        repositoryUri: String,
        upgradeBranch: String,
    ) {
        // Parsing and validation of the configuration
        val config: T = postProcessing.parseAndValidate(order.postProcessingConfig)
        // Launching the post processing
        postProcessing.postProcessing(config, order, repositoryUri, upgradeBranch)
    }

    private fun AutoVersioningOrder.replaceVersion(content: List<String>): List<String> {
        val type = filePropertyType
        val actualValue = if (targetPropertyRegex.isNullOrBlank()) {
            targetVersion
        } else {
            val regex = targetPropertyRegex.toRegex()
            val previousValue = type.readProperty(content, targetProperty) ?: error("Cannot find target property")
            regex.replaceGroup(previousValue, 1, targetVersion)
        }
        return type.replaceProperty(content, targetProperty, actualValue)
    }

    private val AutoVersioningOrder.filePropertyType: FilePropertyType
        get() = autoVersioningTargetFileService.getFilePropertyType(this)

}