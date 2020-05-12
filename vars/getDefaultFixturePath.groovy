import hudson.model.StringParameterDefinition

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Get the the default path to a fixture in the file system.
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
String call(Map<String, Object> options) {
	String defaultPathCanonical = options["defaultPathCanonical"]
	String commitKey = options["commitKey"]
	String parameterName = options["parameterName"]

	String defaultPath = defaultPathCanonical

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
