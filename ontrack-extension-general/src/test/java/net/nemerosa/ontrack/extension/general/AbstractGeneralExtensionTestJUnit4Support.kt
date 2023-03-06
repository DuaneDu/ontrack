package net.nemerosa.ontrack.extension.general

import net.nemerosa.ontrack.it.AbstractDSLTestJUnit4Support
import net.nemerosa.ontrack.model.labels.MainBuildLinksService
import net.nemerosa.ontrack.model.structure.Build
import net.nemerosa.ontrack.model.structure.Project
import org.springframework.beans.factory.annotation.Autowired

@Deprecated("JUnit 4 is deprecated", replaceWith = ReplaceWith("AbstractGeneralExtensionTestSupport"))
abstract class AbstractGeneralExtensionTestJUnit4Support : AbstractDSLTestJUnit4Support() {


    @Autowired
    protected lateinit var mainBuildLinksService: MainBuildLinksService

    /**
     * Release property
     */
    protected var Build.releaseProperty: String?
        get() = property(ReleasePropertyType::class)?.name
        set(value) = if (value != null) {
            property(ReleasePropertyType::class, ReleaseProperty(value))
        } else {
            property(ReleasePropertyType::class, null)
        }

    protected fun Project.setMainBuildLinksProperty(
            labels: List<String>,
            overrideGlobal: Boolean = false
    ) {
        setProperty(
                this,
                MainBuildLinksProjectPropertyType::class.java,
                MainBuildLinksProjectProperty(
                        labels,
                        overrideGlobal
                )
        )
    }
}