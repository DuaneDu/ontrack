package net.nemerosa.ontrack.kdsl.spec.extension.av

/**
 * List of ways the auto approval is managed.
 */
enum class AutoApprovalMode(
    val displayName: String,
) {

    /**
     * Managed at client level, by Ontrack.
     */
    CLIENT("Managed by Ontrack"),

    /**
     * Delegated to the SCM, for example by using the auto merge feature in GitHub.
     */
    SCM("Managed by SCM");

}

