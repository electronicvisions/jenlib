import org.electronicvisions.jenlib.ShellManipulator

/**
 * Run a section of code in a shell with {@code $path/bin/activate} sourced.
 *
 * @param options Keys:
 *                      <ul>
 *                          <li>path: Path to the (existing!) virtualenv/venv (e.g. {@code venv/})</li>
 *                      </ul>
 * @param content Code to be executed with the virtualenv active
 */
def call(Map<String, Object> options = [:], Closure content) {
	Map<String, Object> internalOptions = options.clone()
	String path = internalOptions.get("path")
	if (path == null) {
		throw new IllegalArgumentException("'path' parameter is mandatory.")
	}

	ShellManipulator manipulator = ShellManipulator.fromEnvironment(this)
	manipulator.add(". ${path}/bin/activate && ", "")

	try {
		content()
	} finally {
		manipulator.restore()
	}
}
