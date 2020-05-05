import hudson.model.ParameterDefinition
import hudson.model.ParametersDefinitionProperty
import org.jenkinsci.plugins.structs.describable.UninstantiatedDescribable

/**
 * Add a new build parameter to the job in whose context this step is executed.
 * Existing parameters will not be changed, existing default values will be overwritten unless
 * <code>overwriteDefault=false</code>.
 *
 * This step needs the <a href="https://wiki.jenkins.io/display/JENKINS/Lockable+Resources+Plugin">Lockable Resources Plugin</a>
 * with registered resource {@code JENLIB_JOB_CONFIGURATION_UPDATE}.
 *
 * @param parameter Build parameter to be added
 * @param overwriteDefault Overwrite existing parameter defaults
 */
void call(ParameterDefinition parameter, boolean overwriteDefault = true) {
	// Build parameter changes are not atomic: Make sure parallel builds do not interfere
	// NOTE: 'JENLIB_JOB_CONFIGURATION_UPDATE' needs to be a registered 'Lockable Resource'!
	lock("JENLIB_JOB_CONFIGURATION_UPDATE") {
		// Get existing parameters
		List<ParameterDefinition> oldParams = currentBuild.rawBuild.getParent().
				getProperty(ParametersDefinitionProperty)?.getParameterDefinitions()

		ParameterDefinition updatedParam = oldParams?.find { (it.getName() == parameter.getName()) }
		if (!overwriteDefault && updatedParam != null) {
			parameter = parameter.copyWithDefaultValue(updatedParam.defaultParameterValue)
		}
		oldParams?.removeElement(updatedParam)

		List<ParameterDefinition> newParams = [parameter]

		// Add existing parameters, if there were any
		if (oldParams != null) {
			newParams += oldParams
		}

		// We cannot access the actual ParametersDefinitionProperty object through a public API,
		// so we remove and re-add it
		currentBuild.rawBuild.getParent().removeProperty(ParametersDefinitionProperty)
		currentBuild.rawBuild.getParent().addProperty(new ParametersDefinitionProperty(newParams))
	}
}

/**
 * Add a new build parameter to the job in whose context this step is executed.
 * Existing parameters will not be changed, existing default values will be overwritten unless
 * <code>overwriteDefault=false</code>.
 *
 * This method takes parameters in the form {@code string(name:parameterName,defaultValue:parameterValue)}
 * and thereby does not depend on instantiating {@link ParameterDefinition} objects in (secure) scripts
 *
 * @param parameter Build parameter to be added
 * @param overwriteDefault Overwrite existing parameter defaults
 */
void call(UninstantiatedDescribable parameter, boolean overwriteDefault = true) {
	call((ParameterDefinition) parameter.instantiate(), overwriteDefault)
}
