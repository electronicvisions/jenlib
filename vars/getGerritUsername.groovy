/**
 * Return the username to be used for logging into gerrit.
 */
String call() {
	return runOnSlave(label: "frontend") {
		String gitCommand

		// Local git config
		gitCommand = "git config gitreview.username"

		if (!jesh(script: gitCommand, returnStatus: true)) {
			return jesh(script: gitCommand, returnStdout: true).trim()
		}

		// System git config
		gitCommand = "git config --global gitreview.username"
			if (!jesh(script: gitCommand, returnStatus: true)) {
				return jesh(script: gitCommand, returnStdout: true).trim()
		}
		// 'hudson' is our fallback
		return "hudson"
	}
}
