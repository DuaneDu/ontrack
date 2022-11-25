package net.nemerosa.ontrack.extension.av.config

import net.nemerosa.ontrack.extension.av.event.AutoVersioningEvents

enum class AutoVersioningNotificationScope {

    ALL,
    SUCCESS,
    ERROR,
    PR_TIMEOUT;

    companion object {
        fun toEvents(scope: List<AutoVersioningNotificationScope>): Set<String> {
            val result = mutableSetOf<String>()
            scope.forEach {
                when (it) {
                    ALL -> result += setOf(
                        AutoVersioningEvents.AUTO_VERSIONING_SUCCESS.id,
                        AutoVersioningEvents.AUTO_VERSIONING_PR_MERGE_TIMEOUT_ERROR.id,
                        AutoVersioningEvents.AUTO_VERSIONING_ERROR.id,
                        AutoVersioningEvents.AUTO_VERSIONING_POST_PROCESSING_ERROR.id,
                    )

                    SUCCESS -> result += AutoVersioningEvents.AUTO_VERSIONING_SUCCESS.id

                    PR_TIMEOUT -> result += AutoVersioningEvents.AUTO_VERSIONING_PR_MERGE_TIMEOUT_ERROR.id

                    ERROR -> result += setOf(
                        AutoVersioningEvents.AUTO_VERSIONING_ERROR.id,
                        AutoVersioningEvents.AUTO_VERSIONING_POST_PROCESSING_ERROR.id,
                    )

                }
            }
            return result
        }
    }

}