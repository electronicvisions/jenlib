import org.electronicvisions.ShellManipulator

/**
 * Run a section of code within a specified singularity container.
 * This makes {@code jesh} steps being executed inside a bash within the given singularity container.
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

	String cmdPrefix = "singularity exec " +
	                   "--app ${containerOptions.get("app", "visionary-defaults")} " +
	                   "${containerOptions.get("singularityArgs", "")} " +
	                   "${containerOptions.get("image", "/containers/stable/latest")}"

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
