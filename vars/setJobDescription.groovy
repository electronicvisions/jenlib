/**
 * Set the description of the job (not current build) from within a Jenkinsfile.
 */
def call(String description) {
	currentBuild.rawBuild.project.description = description
}
