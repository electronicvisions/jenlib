import hudson.model.Cause

/**
 * Test whether the current build or any of its upstream triggers has been triggered by a specific cause.
 */
boolean call(Class<?> cause) {
	return currentBuild.rawBuild.getCauses().any { containsCause((Cause) it, cause) }
}

/**
 * Recursively check whether some build cause (including potential upstream triggers) matches some given type of build
 * causes.
 *
 * @param buildCause Build cause to be checked
 * @param cause Cause type to be matched against
 * @return true if a {@code cause} has been found in {@code buildCause}
 */
private boolean containsCause(Cause buildCause, Class<?> cause) {
	if (buildCause instanceof Cause.UpstreamCause) {
		return buildCause.upstreamCauses.any { containsCause(it, cause) }
	}

	return cause.isAssignableFrom(buildCause.getClass())
}
