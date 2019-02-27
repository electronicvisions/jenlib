import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Evaluate the default container path to be used.
 *
 * This usually evaluates to {@code /containers/stable/latest}, this value can however be overwritten by a single line
 * in the commit message of gerrit-triggered commits starting with {@code In-Container: /some/other/image}.
 *
 * Multiple such lines will result in an {@code IllegalArgumentException}.
 */
String call() {
	String defaultImageCanonical = "/containers/stable/latest"
	String defaultImage = defaultImageCanonical

	if (env.GERRIT_CHANGE_COMMIT_MESSAGE != null) {
		String commitMessage = decodeBase64(env.GERRIT_CHANGE_COMMIT_MESSAGE)

		Pattern regex = Pattern.compile('^In-Container:\\s*(.*?)\\s*$')

		for (String line : commitMessage.normalize().readLines()) {
			Matcher match = regex.matcher(line)

			if (match.find()) {
				if (defaultImage != defaultImageCanonical) {
					throw new IllegalArgumentException("'In-Container:' specified multiple times in commit messsage.")
				}
				defaultImage = match.group(1)
			}
		}
	}

	return defaultImage
}
