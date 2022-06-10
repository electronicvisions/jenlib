/**
 * This pipeline step handles periodic cleanup of orphaned slurm slaves and stale jobs.
 *
 * Slaves might either
 * <ul>
 *     <li> Not shut down correctly, leading to orphaned jenkins nodes with no corresponding slurm job
 *     <li> Not have come up successfully, leading to stale jobs that need to be aborted
 *     <li> Stay alive, even the jobs spawning them was terminated; we detect via idle time and stop manually
 * </ul>
 *
 * This step handles both cases by either removing the orphaned node or canceling the respective job.
 *
 * Since direct interaction with the (non-serializable) Jenkins instance is necessary as well as with pipeline
 * steps (which must not be called from non-serializable methods), results from querying Jenkins are cached in
 * lists of serializable wrappers.
 */

import com.cloudbees.groovy.cps.NonCPS
import hudson.model.Computer
import hudson.model.Queue
import jenkins.model.Jenkins
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Serializable abstraction for Jenkins queue items waiting for slurm slaves.
 */
class SlurmQueueItem implements Serializable {
	long jenkinsId
	int slurmId
	String name

	SlurmQueueItem(long jenkinsId, int slurmId, String name) {
		this.jenkinsId = jenkinsId
		this.slurmId = slurmId
		this.name = name
	}
}


/**
 * Serializable abstraction for slurm-based Jenkins computers.
 */
class SlurmComputer implements Serializable {
	Computer computer
	int slurmId

	SlurmComputer(Computer computer, int slurmId) {
		this.computer = computer
		this.slurmId = slurmId
	}
}


/**
 * Get a list of all slurm-slave-based computers currently known to Jenkins.
 * @return List of slurm-slave-based computers
 */
@NonCPS
static List<SlurmComputer> getSlurmComputers() {
	List<SlurmComputer> ret = []

	for (Computer computer : Jenkins.getInstanceOrNull().computers) {
		Pattern slurmIdPattern = Pattern.compile("slurm_(\\d+)")
		Matcher m = slurmIdPattern.matcher(computer.getName())
		if (m.matches()) {
			int slurmId = m.group(1).toInteger()
			SlurmComputer slurmComputer = new SlurmComputer(computer, slurmId)
			ret.add(slurmComputer)
		}
	}

	return ret
}

/**
 * Get a list of all slurm-slave-based computers that are idle for at least idleSeconds.
 * @param idleSeconds Seconds a computer must be idle before being reported as such
 * @return List of slurm-slave-based computers
 */
@NonCPS
static List<SlurmComputer> getIdleSlurmComputers(long idleSeconds) {
	List<SlurmComputer> ret = []

	for (SlurmComputer sc : getSlurmComputers()) {
		if (sc.computer.isIdle() && (System.currentTimeMillis() - sc.computer.getIdleStartMilliseconds() > (1000 * idleSeconds))) {
			ret.add(sc)
		}
	}

	return ret
}

/**
 * Get a list of all items in the Jenkins queue which are waiting for a slurm slave.
 * @return List of queued items waiting for a slurm slave.
 */
@NonCPS
static List<SlurmQueueItem> getJobsWaitingforSlurm() {
	List<SlurmQueueItem> ret = []

	for (Queue.Item queuedJob : Jenkins.getInstanceOrNull().queue.getItems()) {

		/*
		 *  Build "whys" are something like:
		 *  <ul>
		 *      <li> There are no nodes with the label ‘slurm_2792714’
		 *      <li> ‘slurm_2791407’ is offline
		 *  </ul>
		 */
		Pattern waitingForSlurmPattern = Pattern.compile(".*\\bslurm_(\\d+)\\b.*")
		Matcher m = waitingForSlurmPattern.matcher(queuedJob.getWhy())

		if (m.matches()) {
			int slurmId = m.group(1).toInteger()
			SlurmQueueItem queueItem = new SlurmQueueItem(queuedJob.getId(),
			                               slurmId,
			                               queuedJob.task.subTasks[0].getDisplayName())
			ret.add(queueItem)
		}
	}

	return ret
}

/**
 * Check if the slurm queue state is still available for a given job (i.e. the job isn't already removed from the
 * queue due to having completed, cancelled, ...).
 *
 * @param jobId Slurm job id whose queue state is to be checked
 * @return True if the job is still known to the slurm queue (i.e. not dead for long)
 */
boolean jobRemovedFromQueue(int jobId) {
	runOnSlave(label: "frontend") {
		boolean queueStateNotAvailable = jesh(script: "scontrol show jobid ${jobId}",
		                                      returnStatus: true)
		return queueStateNotAvailable
	}
}


/**
 * Main cleanup call: Deletes orphaned slaves and aborts stuck jobs.
 */
def call() {
	for (SlurmComputer computer : getSlurmComputers()) {
		if (jobRemovedFromQueue(computer.slurmId)) {
			echo("[Jenlib] Found orphaned slurm node for job ID ${computer.slurmId}, deleting...")
			assert computer.computer.getName().startsWith("slurm_")
			computer.computer.doDoDelete()
		}
	}

	for (SlurmQueueItem queueItem : getJobsWaitingforSlurm()) {
		if (jobRemovedFromQueue(queueItem.slurmId)) {
			echo("[Jenlib] Found stale queue item for ${queueItem.name}, canceling...")
			Jenkins.getInstanceOrNull().queue.doCancelItem(queueItem.jenkinsId)
		}
	}

	// drop idle slurm computers
	for (SlurmComputer computer : getIdleSlurmComputers(60)) {
		println("[Jenlib] Removing long-idling slave with job ID $computer.slurmId")
		node("frontend") {
			jesh("scancel $computer.slurmId")
		}
	}
}
