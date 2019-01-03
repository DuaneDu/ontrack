package net.nemerosa.ontrack.repository

interface LabelRepository {
    /**
     * Gets list of all labels, ordered by category and name
     */
    val labels: List<LabelRecord>
}

class LabelRecord(
        val id: Int,
        val category: String?,
        val name: String,
        val description: String?,
        val color: String,
        val computedBy: String?
)
