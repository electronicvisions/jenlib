import hudson.model.ParametersDefinitionProperty

/**
 * Remove all build parameters from the job in whose context this step is executed.
 */
void call() {
	lock(resource: "JENLIB_CONFIG_UPDATE_${JOB_NAME}") {
		currentBuild.rawBuild.getParent().removeProperty(ParametersDefinitionProperty)
	}
}
