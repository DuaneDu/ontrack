package net.nemerosa.ontrack.model.structure


data class ValidationRunRequest
@JvmOverloads
constructor(
    val validationStampName: String,
    val validationRunStatusId: ValidationRunStatusID? = null,
    val dataTypeId: String? = null,
    val data: Any? = null,
    val description: String? = null,
    val properties: List<PropertyCreationRequest> = listOf(),
    val signature: Signature? = null,
)
