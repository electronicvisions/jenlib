/**
 * This pipeline step handles periodic cleanup of global (possibly stale) locks.
 */

import com.cloudbees.groovy.cps.NonCPS
import groovy.transform.Field
import hudson.model.Result
import jenkins.model.CauseOfInterruption
import org.jenkins.plugins.lockableresources.LockableResourcesManager
import org.jenkins.plugins.lockableresources.LockableResource

@Field final int MAX_LOCKING_TIME_MILLIS = 1000 * 60 * 10 // 10 min

/**
 * Main cleanup call: Find locks that have been locked for too long and abort the responsible build.
 */
@NonCPS
def call() {
	final LockableResourcesManager manager = LockableResourcesManager.get()

	for (final LockableResource resource : manager.resources) {
		final Date lockTimestamp = resource.reservedTimestamp
		if (lockTimestamp == null) {
			continue
		}

		final Date now = new Date()
		final long lockedTimeMillis = now.time - lockTimestamp.time

		if (lockedTimeMillis <= MAX_LOCKING_TIME_MILLIS) {
			continue
		}

		resource.build.executor.interrupt(Result.FAILURE, new CauseOfInterruption.UserInterruption(
				"[jenlib] Lock '${resource.name}' held for too long. Timeout."
		))
		println("[Jenlib] Forefully unlocked '${resource.name}' after ${lockedTimeMillis / 1000}s.")
	}
}
