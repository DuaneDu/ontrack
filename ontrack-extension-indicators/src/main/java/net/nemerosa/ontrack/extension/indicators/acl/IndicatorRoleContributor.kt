package net.nemerosa.ontrack.extension.indicators.acl

import net.nemerosa.ontrack.model.security.GlobalFunction
import net.nemerosa.ontrack.model.security.ProjectFunction
import net.nemerosa.ontrack.model.security.RoleContributor
import net.nemerosa.ontrack.model.security.Roles
import org.springframework.stereotype.Component

/**
 * Links this extension's security functions to roles
 */
@Component
class IndicatorRoleContributor : RoleContributor {

    override fun getGlobalFunctionContributionsForGlobalRoles(): Map<String, List<Class<out GlobalFunction>>> =
            mapOf(
                    Roles.GLOBAL_ADMINISTRATOR to listOf(
                            IndicatorPortfolioManagement::class.java,
                            IndicatorTypeManagement::class.java
                    )
            )

    override fun getProjectFunctionContributionsForGlobalRoles(): Map<String, List<Class<out ProjectFunction>>> =
            mapOf(
                    Roles.GLOBAL_ADMINISTRATOR to listOf(
                            IndicatorEdit::class.java
                    )
            )

    override fun getProjectFunctionContributionsForProjectRoles(): Map<String, List<Class<out ProjectFunction>>> =
            mapOf(
                    Roles.PROJECT_MANAGER to listOf(
                            IndicatorEdit::class.java
                    ),
                    Roles.PROJECT_OWNER to listOf(
                            IndicatorEdit::class.java
                    )
            )
}