import hudson.model.Cause
import com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritCause

/**
 * Test whether the current build or any of its upstream triggers has been triggered by Gerrit.
 */
boolean call() {
	return currentBuild.rawBuild.getCauses().any { containsGerritCause((Cause) it) }
}

/**
 * Recursively check whether some build cause (including potential upstream triggers) contains a {@link GerritCause}.
 *
 * @param cause Build cause to be checked
 * @return true if a {@link GerritCause} has been found
 */
private boolean containsGerritCause(Cause cause) {
	if (cause instanceof Cause.UpstreamCause) {
		return cause.upstreamCauses.any { containsGerritCause(it) }
	}

	return (cause instanceof GerritCause)
}
