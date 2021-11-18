package net.nemerosa.ontrack.kdsl.spec

import net.nemerosa.ontrack.kdsl.connector.Connected
import net.nemerosa.ontrack.kdsl.connector.Connector
import net.nemerosa.ontrack.kdsl.connector.graphql.schema.FindByProjectByNameQuery
import net.nemerosa.ontrack.kdsl.connector.graphqlConnector

class Ontrack(connector: Connector) : Connected(connector) {

    /**
     * Getting a project using its name
     *
     * @param name Name to look for
     * @return Project or null if not found
     */
    fun findProjectByName(name: String): Project? = graphqlConnector.query(
        FindByProjectByNameQuery(name)
    )?.projects()?.firstOrNull()
        ?.fragments()?.projectFragment()?.run {
            Project(
                connector = connector,
                id = id().toUInt(),
                name = name()!!,
                description = description(),
            )
        }

    /**
     * Getting a build using its name
     *
     * @param project Project name
     * @param branch Branch name
     * @param build Build name
     * @return Project or null if not found
     */
    fun findBuildByName(project: String, branch: String, build: String): Build? = TODO()

}