package net.nemerosa.ontrack.extension.jenkins.indicator

import net.nemerosa.ontrack.extension.indicators.computing.IndicatorComputedCategory
import net.nemerosa.ontrack.extension.indicators.computing.IndicatorComputedType
import net.nemerosa.ontrack.extension.indicators.computing.IndicatorComputedValue
import net.nemerosa.ontrack.extension.indicators.model.*
import net.nemerosa.ontrack.extension.jenkins.JenkinsExtensionFeature
import net.nemerosa.ontrack.extension.scm.indicator.AbstractSCMIndicatorComputer
import net.nemerosa.ontrack.extension.scm.service.SCMService
import net.nemerosa.ontrack.extension.scm.service.SCMServiceDetector
import net.nemerosa.ontrack.model.structure.Project
import org.springframework.stereotype.Component

@Component
class JenkinsPipelineLibraryIndicatorComputer(
    extension: JenkinsExtensionFeature,
    scmServiceDetector: SCMServiceDetector,
    jenkinsPipelineLibraryIndicatorSourceProvider: JenkinsPipelineLibraryIndicatorSourceProvider,
    private val jenkinsPipelineLibraryIndicatorValueType: JenkinsPipelineLibraryIndicatorValueType,
) : AbstractSCMIndicatorComputer(extension, scmServiceDetector) {

    /**
     * Parallelisation of the computation is not necessary for this indicator.
     */
    override val perProject: Boolean = false

    override val name: String = "Jenkins pipeline libraries"

    override val source: IndicatorSource = jenkinsPipelineLibraryIndicatorSourceProvider.createSource("")

    override fun computeSCMIndicators(
        project: Project,
        scmService: SCMService,
        scmBranch: String
    ): List<IndicatorComputedValue<*, *>> {
        // Gets the content of the Jenkinsfile (or returns no indicator)
        val jenkinsfile = scmService.download(project, scmBranch, "Jenkinsfile") ?: return emptyList()
        // Parsing of the Jenkinsfile to extract the library versions
        val libraries = JenkinsPipelineLibrary.extractLibraries(jenkinsfile)
        // Converts to indicators
        return libraries.map {
            convertToIndicator(it)
        }
    }

    private fun convertToIndicator(library: JenkinsPipelineLibrary) =
        IndicatorComputedValue(
            type = getIndicatorType(library),
            value = library.version,
            comment = null,
        )

    private fun getIndicatorType(library: JenkinsPipelineLibrary) =
        IndicatorComputedType(
            category = indicatorCategory,
            id = library.name,
            name = "Using the ${library.name} Jenkins pipeline library",
            link = null,
            valueType = jenkinsPipelineLibraryIndicatorValueType,
            valueConfig = JenkinsPipelineLibraryIndicatorValueTypeConfig(
                // TODO Makes the configuration configurable per library (settings)
                versionRequired = false,
                versionMinimum = null,
            ),
        )

    private val indicatorCategory = IndicatorComputedCategory(
        id = "jenkins-pipeline-library",
        name = "Jenkins pipeline libraries"
    )

}