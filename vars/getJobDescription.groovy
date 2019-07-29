/**
 * Return the description of the job (not current build) from within a Jenkinsfile.
 */
String call() {
	return currentBuild.rawBuild.project.description
}
