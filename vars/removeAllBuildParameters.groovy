import hudson.model.ParametersDefinitionProperty

/**
 * Remove all build parameters from the job in whose context this step is executed.
 */
void call() {
	lock("JENLIB_JOB_CONFIGURATION_UPDATE") {
		currentBuild.rawBuild.getParent().removeProperty(ParametersDefinitionProperty)
	}
}
