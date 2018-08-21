/**
 * SiSH -> Singularity-aware Shell
 *
 * This shell is to be used whenever some command <i>might</i> be run within a singularity container.
 * It checks whether the {@code JENLIB_CONTAINER_IMAGE} environment variable is not empty and uses the so-defined
 * container image if not.
 *
 * Other respected environment variables are {@code JENLIB_CONTAINER_APP} and {@code JENLIB_CONTAINER_ARGS}.
 *
 * @param options Same as {@code sh} pipeline step
 * @return Whatever the {@code sh} pipeline step returns
 */
def call(Map<String, Object> options = [:]) {
	if (!options.get("script")) {
		throw new IllegalArgumentException("Argument 'script' is mandatory.")
	}

	if (env.JENLIB_CONTAINER_IMAGE?.length()) {
		options["script"] = options.get("script").replace("'", "'\\''")  // Escape ' for the shell command below
		options["script"] = "singularity exec " +
		                    "--app ${env.JENLIB_CONTAINER_APP} " +
		                    "${env.JENLIB_CONTAINER_ARGS} " +
		                    "${env.JENLIB_CONTAINER_IMAGE} " +
		                    "bash -c '${options.get("script")}'"

		return sh(options)
	} else {
		return sh(options)
	}
}

/**
 * SiSH -> Singularity-aware Shell
 *
 * This shell is to be used whenever some command <i>might</i> be run within a singularity container.
 * It checks whether the {@code JENLIB_CONTAINER_IMAGE} environment variable is not empty and uses the so-defined
 * container image if not.
 *
 * Other respected environment variables are {@code JENLIB_CONTAINER_APP} and {@code JENLIB_CONTAINER_ARGS}.
 *
 * @param command Shell command to be executed
 */
def call(String command) {
	return call(script: command)
}
