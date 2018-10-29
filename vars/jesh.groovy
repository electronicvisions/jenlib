import org.electronicvisions.ShellManipulator

/**
 * JeSH -> Jenlib Shell
 *
 * This shell is to be used whenever some command <i>might</i> be run within a context that requires
 * shell command modifications (e.g. {@code inSingularity}).
 * It  uses an instance of {@link ShellManipulator} to determine the required modifications.
 *
 * @param options Same as {@code sh} pipeline step
 * @return Whatever the {@code sh} pipeline step returns
 */
def call(Map<String, Object> options = [:]) {
	if (!options.get("script")) {
		throw new IllegalArgumentException("Argument 'script' is mandatory.")
	}

	String command = options["script"]

	ShellManipulator manipulator = new ShellManipulator(this)
	options["script"] = "bash " + manipulator.constructScriptStack(command)

	echo("[jesh] Running command: $command")
	return sh(options)
}

/**
 * JeSH -> Jenlib Shell
 *
 * This shell is to be used whenever some command <i>might</i> be run within a context that requires
 * shell command modifications (e.g. {@code inSingularity}).
 * It  uses an instance of {@link ShellManipulator} to determine the required modifications.
 *
 * @param command Shell command to be executed
 */
def call(String command) {
	return call(script: command)
}
