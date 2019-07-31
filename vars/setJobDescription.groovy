/**
 * Set the description of the job (not current build) from within a Jenkinsfile.
 */
def call(String description) {
	// with check, changing description not atomic. note that the ressoure
	// JENLIB_JOB_CONFIGURATION_UPDATE must be a registered lockable ressource
	lock("JENLIB_JOB_CONFIGURATION_UPDATE"){
		// only change if not already set. this helps keep the number of config changes small
		if (currentBuild.rawBuild.project.description != description) {
			currentBuild.rawBuild.project.description = description
		}
	}
}
