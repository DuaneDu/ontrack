package net.nemerosa.ontrack.acceptance.tests.dsl

import net.nemerosa.ontrack.acceptance.AbstractACCDSL
import net.nemerosa.ontrack.acceptance.support.AcceptanceTestSuite
import org.junit.Test

import static net.nemerosa.ontrack.test.TestUtils.uid

@AcceptanceTestSuite
class ACCDSLSonarQube extends AbstractACCDSL {

    @Test
    void 'SonarQube configuration'() {
        def name = uid('S')
        ontrack.configure {
            sonarQubeWithToken name, 'http://sonarqube.nemerosa.net', 'xxx'
        }
        assert ontrack.config.sonarQube.find { it == name } != null
    }

    @Test
    void 'Global settings'() {
        ontrack.config.sonarQubeSettings = [
                measures: ["measure-1", "measure-2"],
                disabled: false
        ]
        def settings = ontrack.config.sonarQubeSettings
        assert settings.measures == ["measure-1", "measure-2"]
        assert !settings.disabled
    }

    @Test
    void 'Project default property'() {
        // General settings
        ontrack.config.sonarQubeSettings = [
                measures: ["measure-1", "measure-2"],
                disabled: false
        ]
        // Creating a configuration
        def name = uid('S')
        ontrack.configure {
            sonarQubeWithToken name, 'http://sonarqube.nemerosa.net', 'xxx'
        }
        // Configuring a project
        String projectName = uid("P")
        ontrack.project(projectName) {
            config.sonarQube(
                    configuration: name,
                    key: "project:key",
            )
            // Checks the property
            def prop = config.sonarQube
            assert prop.configuration.name == name
            assert prop.key == "project:key"
            assert prop.validationStamp == "sonarqube"
            assert prop.measures == []
            assert prop.override == false
            assert prop.branchModel == false
            assert prop.branchPattern == null
        }
    }

    @Test
    void 'Project custom property'() {
        // General settings
        ontrack.config.sonarQubeSettings = [
                measures: ["measure-1", "measure-2"],
                disabled: false
        ]
        // Creating a configuration
        def name = uid('S')
        ontrack.configure {
            sonarQubeWithToken name, 'http://sonarqube.nemerosa.net', 'xxx'
        }
        // Configuring a project
        String projectName = uid("P")
        ontrack.project(projectName) {
            config.sonarQube(
                    configuration: name,
                    key: "project:key",
                    validationStamp: "some-stamp",
                    measures: ["measure-3"],
                    override: true,
                    branchModel: true,
                    branchPattern: "release-2\\..*"
            )
            // Checks the property
            def prop = config.sonarQube
            assert prop.configuration.name == name
            assert prop.key == "project:key"
            assert prop.validationStamp == "some-stamp"
            assert prop.measures == ["measure-3"]
            assert prop.override == true
            assert prop.branchModel == true
            assert prop.branchPattern == "release-2\\..*"
        }
    }

}
