import hudson.model.Result

/**
 * This method allows to set the build result of the current job in an arbitrary way.
 * The default setter only allows results to get worse.
 *
 * @see <a href="https://github.com/jenkinsci/jenkins/blob/578d6ba/core/src/main/java/hudson/model/Run.java " >setResult()</a>
 * @param options Map of options:
 * <ul>
 *     <li><b>state:</b> Result to be set, one of ["NOT_BUILT", "UNSTABLE", "SUCCESS", "FAILURE", "ABORTED"]</li>
 *     <li><b>reason:</b> Reason for setting changing the build state.</li>
 * </ul>
 */
def call(Map<String, Object> options) {
	String state = options.get("state")
	if (state == null) {
		raise new IllegalArgumentException("'state' argument is mandatory.")
	}

	String reason = options.get("reason")
	if (reason == null) {
		raise new IllegalArgumentException("'reason' argument is mandatory.")
	}

	echo("[jenlib] Setting build state to '${state}'. Reason: ${reason}")
	currentBuild.rawBuild.@result = Result.metaClass.getAttribute(Result, state)
}

/**
 * This method allows to set the build result of the current job in an arbitrary way.
 * The default setter only allows results to get worse.
 *
 * @see <a href="https://github.com/jenkinsci/jenkins/blob/578d6ba/core/src/main/java/hudson/model/Run.java">setResult()</a>
 * @param state Result to be set, one of ["NOT_BUILT", "UNSTABLE", "SUCCESS", "FAILURE", "ABORTED"]
 */
def call(String state) {
	currentBuild.rawBuild.@result = Result.metaClass.getAttribute(Result, state)
}
