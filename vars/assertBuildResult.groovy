/**
 * Ensure that some code changes the build result of the current job according to a given expectation.
 *
 * Example:
 * 	<pre>
 * 	    assertBuildResult("UNSTABLE"){
 * 	        raise_warnings
 * 	    }
 *	</pre>
 *
 * @param expected_result Expected build state. One of ["NOT_BUILT", "UNSTABLE", "SUCCESS", "FAILURE", "ABORTED"]
 * @param content Code that is supposed to change the build state to {@code expected_result}
 */
def call(String expected_result, Closure content) {
	// Start clean
	if (currentBuild.currentResult) {
		assert (currentBuild.currentResult == "SUCCESS"):
				"Current build status should be 'SUCCESS' (is: ${currentBuild.currentResult})"
	}

	// Execute the content, it may throw if we are expecting a failure
	try {
		content()
	} catch (Throwable t) {
		if (expected_result == "FAILURE") {
			echo "${t.toString()} was raised, expected failure occured."
			setBuildState("SUCCESS")
			return
		} else {
			throw t
		}
	}

	// Check for the correct result
	assert (currentBuild.currentResult == expected_result):
			"Current build status should be '${expected_result}' (is: ${currentBuild.currentResult})"

	// If we didn't raise so far, everything's good
	setBuildState("SUCCESS")
}
