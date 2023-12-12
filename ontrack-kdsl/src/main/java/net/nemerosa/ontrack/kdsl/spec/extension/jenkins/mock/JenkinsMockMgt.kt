package net.nemerosa.ontrack.kdsl.spec.extension.jenkins.mock

import net.nemerosa.ontrack.kdsl.connector.Connected
import net.nemerosa.ontrack.kdsl.connector.Connector

class JenkinsMockMgt(connector: Connector) : Connected(connector) {

    fun job(path: String): MockJenkinsJob = MockJenkinsJob(connector, path)

}