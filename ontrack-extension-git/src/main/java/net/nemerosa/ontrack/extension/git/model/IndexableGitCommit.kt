package net.nemerosa.ontrack.extension.git.model

import net.nemerosa.ontrack.common.Time
import net.nemerosa.ontrack.git.model.GitCommit

class IndexableGitCommit(
        val commit: GitCommit,
        val timestamp: Long = Time.toEpochMillis(commit.commitTime)
)
