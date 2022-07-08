package net.nemerosa.ontrack.service

import net.nemerosa.ontrack.job.*
import net.nemerosa.ontrack.model.metrics.MetricsReexportJobProvider
import net.nemerosa.ontrack.model.structure.RunInfoService
import net.nemerosa.ontrack.model.support.JobProvider
import net.nemerosa.ontrack.model.support.RestorationJobs
import org.springframework.stereotype.Component

/**
 * Job used to re-export all run infos into registered listeners.
 */
@Component
class RunInfoRestorationJob(
        private val runInfoService: RunInfoService
): JobProvider, Job, MetricsReexportJobProvider {

    override fun getStartingJobs(): Collection<JobRegistration> =
            listOf(
                    JobRegistration(
                            this,
                            Schedule.NONE // Manually only
                    )
            )

    override fun isDisabled(): Boolean = false

    override fun getReexportJobKey(): JobKey = key

    override fun getKey(): JobKey =
            RestorationJobs.RESTORATION_JOB_TYPE.getKey("run-info-restoration")

    override fun getDescription(): String = "Run Info Restoration"

    override fun getTask() = JobRun { listener ->
        runInfoService.restore {
            listener.message(it)
        }
    }

}