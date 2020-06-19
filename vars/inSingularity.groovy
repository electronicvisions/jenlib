import org.electronicvisions.jenlib.ShellManipulator

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
 *                              <li>app: [optional, defaults to no app] Singularity container app to be used.</li>
 *                              <li>singularityArgs: [optional, defaults to ""] Additional singularity arguments.</li>
 *                          </ul>
 * @param content Code to be executed in the context of a container instance.
 */
def call(Map<String, String> containerOptions = [:], Closure content) {

	/**
	 * Argument to be be passed to singularity for specifying the app.
	 */
	String appArgument = ""

	if (containerOptions.get("app") != null){
		appArgument = "--app=${containerOptions.get("app")}"
	}

	// Lazy evaluate getDefaultContainerPath: Computing it is not necessary if an image argument is given
	String containerImage = containerOptions.get("image")
	if (containerImage == null) {
		containerImage = getDefaultContainerPath()
	}

	String cmdPrefix = "singularity exec " +
	                   "${appArgument} " +
	                   "${containerOptions.get("singularityArgs", "")} " +
	                   "${containerImage}"

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
