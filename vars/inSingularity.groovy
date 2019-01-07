import org.electronicvisions.ShellManipulator

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Run a section of code within a specified singularity container.
 * This makes {@code jesh} steps being executed inside a bash within the given singularity container.
 *
 *  The order of which singularity image is invoked is the following:
 *  <ul>
 *      <li>If nothing is specified -> use /container/stable/latest</li>
 *      <li>If "In-Container: /path/to/image" is present in commit message, use "/path/to/image" instead of default path.</li>
 *      <li>If containerOptions contains "image", *always* use that image (highest priority, trumps "In-Container:"-tag).</li>
 *  </ul>
 *
 * @param containerOptions Keys:
 *                          <ul>
 *                              <li>image: [optional, defaults to "/containers/stable/latest"] Singularity container image to be used.
 *                              If the commit being built contains "In-Container: /path/to/image", the default will be change to "/path/to/image".</li>
 *                              <li>app: [optional, defaults to "visionary-defaults"] Singularity container app to be used.</li>
 *                              <li>singularityArgs: [optional, defaults to ""] Additional singularity arguments.</li>
 *                          </ul>
 * @param content Code to be executed in the context of a container instance.
 */
def call(Map<String, String> containerOptions = [:], Closure content) {

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

	String cmdPrefix = "singularity exec " +
	                   "--app ${containerOptions.get("app", "visionary-defaults")} " +
	                   "${containerOptions.get("singularityArgs", "")} " +
	                   "${containerOptions.get("image", defaultImage)}"

	ShellManipulator manipulator = new ShellManipulator(this)
	manipulator.add(cmdPrefix, "")

	try {
		content()
	} catch (Throwable anything) {
		throw anything
	} finally {
		manipulator.restore()
	}
}
