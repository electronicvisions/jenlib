import hudson.model.StringParameterDefinition

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Get the the default path to a fixture in the file system.
 *
 * Upon first call, the realpath of the fixture is evaluated and from there on used for all subsequent calls with the
 * same set of options.
 *
 * This function generates the path to a fixture based on three parameters:
 * <ul>
 *     <li> A plain default path, given as string. E.g. <code>/containers/stable/latest</code></li>
 *     <li> A magic key/value pair in the commit message as passed by gerrit, e.g. <code>In-Container:
 *         /my/personal/container</code></li>
 *     <li> A build parameter, e.g. <code>OVERWRITE_DEFAULT_CONTAINER_PATH=/my/other/container</code></li>
 * </ul>
 *
 * @param options Map of options, mandatory keys are <code>defaultPathCanonical</code>, <code>commitKey</code>,
 *                <code>parameterName</code>.
 */
String call(Map<String, String> options) {
	String optionCacheKey = "FIXTURE_PATH_CACHE-" + options.values().join()

	if (env[optionCacheKey] != null) {
		return env[optionCacheKey]
	}

	// We need to be on a node to resolve the realpath to our fixture.
	// Here we assume that a frontend has all mount points.
	runOnSlave(label: "frontend") {
		env[optionCacheKey] = jesh(script: "readlink -f ${getDefaultPath(options)}",
		                           returnStdout: true).trim()
	}

	echo("[Jenlib] Using fixture: '${env[optionCacheKey]}'")
	return env[optionCacheKey]
}

private String getDefaultPath(Map<String, String> options) {
	String defaultPathCanonical = options["defaultPathCanonical"]
	String commitKey = options["commitKey"]
	String parameterName = options["parameterName"]

	String defaultPath = defaultPathCanonical

	if (isTriggeredByGerrit()) {
		assert env.GERRIT_CHANGE_COMMIT_MESSAGE != null: "Commit message not found in build triggered by gerrit!"
	}

	// env-based conditional enables testability in non-gerrit-triggered setups
	if (env.GERRIT_CHANGE_COMMIT_MESSAGE != null) {
		String commitMessage = decodeBase64(env.GERRIT_CHANGE_COMMIT_MESSAGE)

		Pattern regex = Pattern.compile("^${commitKey}:\\s*(.*?)\\s*\$")

		for (String line : commitMessage.normalize().readLines()) {
			Matcher match = regex.matcher(line)

			if (match.find()) {
				if (defaultPath != defaultPathCanonical) {
					throw new IllegalArgumentException("'${commitKey}:' specified multiple times in commit messsage.")
				}
				defaultPath = match.group(1)
			}
		}
	}

	// Add build parameter
	addBuildParameter(new StringParameterDefinition(parameterName,
	                                                "",
	                                                "${commitKey} value to be used as default.",
	                                                true))

	// Parameter overwrites everything if set
	if (params.get(parameterName)?.length()) {
		defaultPath = params.get(parameterName)
	}

	return defaultPath
}
