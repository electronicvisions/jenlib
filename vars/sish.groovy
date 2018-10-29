/**
 * ---------- DEPRECATED! ----------
 *
 * SiSH -> Singularity-aware Shell
 *
 * This shell is to be used whenever some command <i>might</i> be run within a singularity container.
 * It wraps {@code jesh}.
 *
 * @param options Same as {@code sh} pipeline step
 * @return Whatever the {@code sh} pipeline step returns
 */
def call(Map<String, Object> options = [:]) {
	echo "[WARNING] Singularity Shell 'sish' is deprecated. Use 'jesh' instead!"
	return jesh(options)
}

/**
 * ---------- DEPRECATED! ----------
 *
 * SiSH -> Singularity-aware Shell
 *
 * This shell is to be used whenever some command <i>might</i> be run within a singularity container.
 * It wraps {@code jesh}.
 *
 * @param command Shell command to be executed
 */
def call(String command) {
	return call(script: command)
}
