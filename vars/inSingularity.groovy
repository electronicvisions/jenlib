/**
 * Run a section of code within a specified singularity container.
 * This makes {@code sish} steps being executed inside a bash within the given singularity container.
 *
 * @param containerOptions Keys:
 *                          <ul>
 *                              <li>image: [optional, defaults to "/containers/stable/latest"] Singularity container image to be used.</li>
 *                              <li>app: [optional, defaults to "visionary-defaults"] Singularity container app to be used.</li>
 *                              <li>singularityArgs: [optional, defaults to ""] Additional singularity arguments.</li>
 *                          </ul>
 * @param content Code to be executed in the context of a container instance.
 */
def call(Map<String, String> containerOptions = [:], Closure content) {
	if (env.JENLIB_CONTAINER_IMAGE?.length()) {
		throw new IllegalStateException("Cannot nest singularity instances: ${env.JENLIB_CONTAINER_IMAGE}")
	}

	env.JENLIB_CONTAINER_IMAGE = containerOptions.get("image", "/containers/stable/latest")
	env.JENLIB_CONTAINER_APP = containerOptions.get("app", "visionary-defaults")
	env.JENLIB_CONTAINER_ARGS = containerOptions.get("singularityArgs", "")

	try {
		content()
	} catch (Throwable anything) {
		throw anything
	} finally {
		env.JENLIB_CONTAINER_IMAGE = ""
		env.JENLIB_CONTAINER_APP = ""
		env.JENLIB_CONTAINER_ARGS = ""
	}
}
