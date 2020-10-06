import org.electronicvisions.jenlib.ShellManipulator

/**
 * Run a section of code with some loaded modules.
 * This makes {@code jesh} steps being executed in a shell that has the specified modules
 * loaded.
 *
 * @param options Keys:
 *                      <ul>
 *                          <li>modules: List of modules to be loaded (e.g. {@code ["git/2.6.2"]})</li>
 *                          <li>purge: [optional, defaults to {@code false}] Purge existing module before loading new ones.</li>
 *                          <li>prependModulePath: [optional] Path to prepended to {@code MODULEPATH}.</li>
 *                      </ul>
 * @param content Code to be executed in the context of some modules.
 */
def call(Map<String, Object> options = [:], Closure content) {
	if (!(options.get("modules") instanceof List)) {
		throw new IllegalArgumentException("modules have to be a list.")
	}
	List<String> modules = options.get("modules") as List<String>

	if (!(options.get("purge", false) instanceof Boolean)) {
		throw new IllegalArgumentException("purge has to be boolean.")
	}
	Boolean purge = options.get("purge")

	if (!(options.get("prependModulePath", "") instanceof CharSequence)) {
		throw new IllegalArgumentException("prependModulePath has to be a string.")
	}
	String prependModulePath = options.get("prependModulePath")

	/**
	 * 'Builder' for the final (long...) command prepended to every shell call.
	 */
	List<String> prefixCommands = new ArrayList()

	// More sanity for the shell...
	prefixCommands.add('set -euo pipefail')

	// Sanity check: MODULESHOME must be defined.
	prefixCommands.add('[ -n "${MODULESHOME:-}" ] || ' +
	                   '{ echo MODULESHOME is not defined or empty. Check environment.>&2; exit 1; }')

	// Get 'module' command for the current shell
	prefixCommands.add('source "$MODULESHOME"/init/$(readlink -f /proc/$$/exe | xargs basename)')

	// Module command may not yet be exported. Don't fail if it's not a function.
	prefixCommands.add('export -f module || true')

	if (prependModulePath.length() > 0) {
		// Set MODULEPATH, don't add a colon if it had been empty
		prefixCommands.add("export MODULEPATH=\"${prependModulePath}\${MODULEPATH:+:\$MODULEPATH}\"")
	}

	if (purge) {
		prefixCommands.add('module purge')
	}

	for (String module in modules) {
		prefixCommands.add("module load $module")
	}

	// Sanity check: Loaded modules must be in list of loaded modules
	for (module in modules) {
		prefixCommands.add("module list |& grep \"${module}\" >/dev/null || " +
		                   "{ echo Module ${module} did not load correctly.>&2; exit 1; }")
	}

	ShellManipulator manipulator = ShellManipulator.fromEnvironment(this)
	manipulator.add(prefixCommands.join(" && ") + " &&", "")

	try {
		content()
	} finally {
		manipulator.restore()
	}
}
