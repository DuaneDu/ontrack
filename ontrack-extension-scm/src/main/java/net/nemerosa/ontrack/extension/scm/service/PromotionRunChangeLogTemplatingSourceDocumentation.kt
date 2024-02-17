package net.nemerosa.ontrack.extension.scm.service

import net.nemerosa.ontrack.model.annotations.APIDescription

data class PromotionRunChangeLogTemplatingSourceDocumentation(
    @APIDescription("By default, if a previous promotion is not found on the current branch, it'll be looked for in all branches of the projects. Set this parameter to `false` to disable this behaviour.")
    val acrossBranches: Boolean = true,
    @APIDescription("Comma-separated list of project links to follow one by one for a get deep change log. Each item in the list is either a project name, or a project name and qualifier separated by a colon (:).")
    val projects: List<String> = emptyList(),
    @APIDescription("Include a title for the change log")
    val title: Boolean = false,
)
