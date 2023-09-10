package net.nemerosa.ontrack.kdsl.spec.extension.av

import com.fasterxml.jackson.databind.JsonNode

data class AutoVersioningSourceConfig(
    val sourceProject: String,
    val sourceBranch: String,
    val sourcePromotion: String,
    val targetPath: String,
    val targetRegex: String? = null,
    val targetProperty: String? = null,
    val targetPropertyRegex: String? = null,
    val targetPropertyType: String? = null,
    val autoApproval: Boolean? = null,
    val upgradeBranchPattern: String? = null,
    val postProcessing: String? = null,
    val postProcessingConfig: JsonNode? = null,
    val validationStamp: String? = null,
    val autoApprovalMode: AutoApprovalMode? = AutoApprovalMode.CLIENT,
    val notifications: List<AutoVersioningNotification>? = null,
    val backValidation: String? = null,
)