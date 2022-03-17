package net.nemerosa.ontrack.service.job

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.nemerosa.ontrack.it.AbstractServiceTestSupport
import net.nemerosa.ontrack.job.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultJobSchedulerIT : AbstractServiceTestSupport() {

    @Autowired
    private lateinit var jobScheduler: JobScheduler

    @Test
    fun `Job timeout`() {
        var count = 0
        var completed = false
        val longRunningJob = object : Job {
            override fun getKey(): JobKey = JobCategory.of("test").getType("jobs").getKey("timeout")

            override fun getTask() = JobRun {
                runBlocking {
                    repeat(30) {
                        count++
                        println("Task ($it)")
                        delay(100)
                    }
                    println("Done")
                    completed = true
                }
            }

            override fun getDescription(): String = "Long running job"

            override fun isDisabled(): Boolean = false

            override fun getTimeout() = Duration.ofMillis(500)
        }
        jobScheduler.schedule(longRunningJob, Schedule.NONE)
        jobScheduler.fireImmediately(longRunningJob.key)
        runBlocking {
            delay(1000)
        }
        val stopped = jobScheduler.checkForTimeouts()
        runBlocking {
            delay(500)
        }
        assertTrue(count > 0, "At least some tasks had started to run")
        assertFalse(completed, "The job was interrupted")
        assertTrue(stopped > 0, "At least one job was stopped")

    }

}