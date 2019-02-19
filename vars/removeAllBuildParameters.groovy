import hudson.model.ParametersDefinitionProperty

/**
 * Remove all build parameters from the job in whose context this step is executed.
 */
void call() {
	currentBuild.rawBuild.getParent().removeProperty(ParametersDefinitionProperty)
}
