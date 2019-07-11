import hudson.model.Cause

/**
 * Test whether the current build or any of its upstream triggers has been triggered manually (including Replays).
 */
boolean call() {
	return isTriggeredBy(Cause.UserIdCause)
}
