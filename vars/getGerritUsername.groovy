/**
 * Return the username to be used for logging into gerrit.
 */
String call() {
	String gitCommand

	// Local git config
	gitCommand = "git config gitreview.username"
	if (!sish(script: gitCommand, returnStatus: true)) {
		return sish(script: gitCommand, returnStdout: true).trim()
	}

	// System git config
	gitCommand = "git config --global gitreview.username"
	if (!sish(script: gitCommand, returnStatus: true)) {
		return sish(script: gitCommand, returnStdout: true).trim()
	}

	// 'hudson' is our fallback
	return "hudson"
}
