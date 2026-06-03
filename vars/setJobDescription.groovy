/**
 * Set the description of the job (not current build) from within a Jenkinsfile.
 */
def call(String description) {
	// with check, changing description not atomic.
	lock(resource: "JENLIB_CONFIG_UPDATE_${JOB_NAME}"){
		// only change if not already set. this helps keep the number of config changes small
		if (currentBuild.rawBuild.project.description != description) {
			currentBuild.rawBuild.project.description = description
		}
	}
}
