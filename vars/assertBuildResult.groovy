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
 * @param expected_result: Expected build state. One of ["NOT_BUILT", "UNSTABLE", "SUCCESS", "FAILURE", "ABORTED"]
 * @param content: Code that is supposed to change the build state to {@code expected_result}
 */
def call(String expected_result, Closure content) {
	// Start clean
	if (currentBuild.currentResult) {
		assert (currentBuild.currentResult == "SUCCESS")
	}

	// Execute the content, it may throw if we are expecting a failure
	try {
		content()
	} catch (Exception e) {
		if (expected_result == "FAILURE") {
			echo "${e.toString()} was raised, expected failure occured."
			setBuildState("SUCCESS")
			return
		} else {
			throw e
		}
	}

	// Check for the correct result
	assert (currentBuild.currentResult == expected_result)

	// If we didn't raise so far, everything's good
	setBuildState("SUCCESS")
}
