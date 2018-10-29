/**
 * Special stage that reports unstable results back to jenkins before
 * the build is finished.
 *
 * Note that this is <i>not</i> a drop-in replacement for {@code stage()} blocks
 * in declarative pipelines (in scripted ones it is!): Jenkins is missing a feature
 * that allows redefinition of declarative stages, c.f. JENKINS-50548.
 */
def call(String stageName, Closure content) {
	stage(stageName) {
		content()
	}

	// Ensure we are only trying to report if we're in a gerrit build
	if (env.GERRIT_PORT && env.GERRIT_HOST && env.GERRIT_PATCHSET_REVISION) {
		// Report everything that is no success
		if (currentBuild.currentResult != "SUCCESS") {
			jesh(script: "ssh -p $GERRIT_PORT ${getGerritUsername()}@$GERRIT_HOST " +
			             "gerrit review --label Code-Review=-1 " +
			             "-m '\"Early failure notification for Jenkins build $BUILD_URL\"' " +
			             "$GERRIT_PATCHSET_REVISION")
		}
	}
}
