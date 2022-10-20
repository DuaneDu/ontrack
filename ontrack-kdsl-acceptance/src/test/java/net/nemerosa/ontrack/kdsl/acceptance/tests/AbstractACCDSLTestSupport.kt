package net.nemerosa.ontrack.kdsl.acceptance.tests

import net.nemerosa.ontrack.kdsl.acceptance.tests.support.uid
import net.nemerosa.ontrack.kdsl.acceptance.tests.support.waitUntil
import net.nemerosa.ontrack.kdsl.spec.Branch
import net.nemerosa.ontrack.kdsl.spec.Build
import net.nemerosa.ontrack.kdsl.spec.Project
import net.nemerosa.ontrack.kdsl.spec.admin.admin

abstract class AbstractACCDSLTestSupport : AbstractACCTestSupport() {

    /**
     * Wrapper code to create a project.
     *
     * @param name Name of the project to create (generated by default)
     * @param description Description of the project
     * @param code Code to run against the project
     */
    protected fun <T> project(
        name: String = uid("p"),
        description: String = "",
        code: Project.() -> T,
    ): T = ontrack.createProject(name, description).code()

    /**
     * Wrapper code to create a branch inside a project.
     *
     * @param name Name of the branch to create (generated by default)
     * @param description Description of the branch
     * @param code Code to run against the branch
     */
    protected fun <T> Project.branch(
        name: String = uid("b"),
        description: String = "",
        code: Branch.() -> T,
    ): T = createBranch(name, description).code()

    /**
     * Wrapper code to create a promotion level inside a branch.
     *
     * @param name Name of the promotion level to create (generated by default)
     * @param description Description of the promotion level
     */
    protected fun Branch.promotion(
        name: String = uid("pl"),
        description: String = "",
    ) = createPromotionLevel(name, description)

    /**
     * Wrapper code to create a validation stamp inside a branch.
     *
     * @param name Name of the validation stamp to create (generated by default)
     * @param description Description of the validation stamp
     * @param runTime Run time in seconds
     */
    protected fun Branch.validationStamp(
        name: String = uid("vs"),
        description: String = "",
    ) = createValidationStamp(name, description)

    /**
     * Wrapper code to create a build inside a branch.
     *
     * @param name Name of the build to create (generated by default)
     * @param description Description of the build
     * @param code Code to run against the build
     */
    protected fun <T> Branch.build(
        name: String = uid("bd"),
        description: String = "",
        runTime: Int? = null,
        code: Build.() -> T,
    ): T = createBuild(
        name = name,
        description = description,
        runTime = runTime
    ).code()

    /**
     * Checks that a given message has been logged as error in Ontrack.
     */
    protected fun checkErrorMessageLogged(
        message: String,
        timeout: Long = 20_000L,
        interval: Long = 5_000L,
    ) {
        waitUntil(
            task = "Logged message = $message",
            timeout = timeout,
            interval = interval,
        ) {
            ontrack.admin.logEntries().find {
                it.detailList.any { detail ->
                    val description = detail.description
                    detail.name == "message" && description != null && message in description
                }
            } != null
        }
    }

}