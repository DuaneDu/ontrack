package net.nemerosa.ontrack.repository

import net.nemerosa.ontrack.model.structure.ID
import net.nemerosa.ontrack.model.structure.Project
import net.nemerosa.ontrack.repository.support.AbstractJdbcRepository
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class ProjectJdbcRepository(dataSource: DataSource) : AbstractJdbcRepository(dataSource),
    ProjectJdbcRepositoryAccessor {

    override fun getProject(id: ID): Project =
        getFirstItem(
            """
                SELECT *
                FROM  projects
                WHERE id = :id
            """,
            params("id", id.value)
        ) { rs, _ ->
            Project(
                id = id(rs),
                name = rs.getString("name"),
                description = rs.getString("description"),
                isDisabled = rs.getBoolean("disabled"),
                signature = readSignature(rs),
            )
        }

}