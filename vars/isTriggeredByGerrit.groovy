import com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritCause

/**
 * Test whether the current build or any of its upstream triggers has been triggered by Gerrit.
 */
boolean call() {
	return isTriggeredBy(GerritCause)
}
