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
 *                          <li>moduleInitPath: [optional] Path to sourceable file that provides the {@code module} command.</li>
 *                          <li>prependModulePath: [optional] Path to prepended to {@code MODULEPATH}.</li>
 *                      </ul>
 * @param content Code to be executed in the context of some modules.
 */
def call(Map<String, Object> options = [:], Closure content) {
	if (!(options.get("modules") instanceof List)) {
		throw new IllegalArgumentException("modules have to be a list.")
	}

	if (!(options.get("purge", true) instanceof Boolean)) {
		throw new IllegalArgumentException("purge has to be boolean.")
	}

	if (!(options.get("moduleInitPath", "") instanceof CharSequence)) {
		throw new IllegalArgumentException("moduleInitPath has to be a string.")
	}

	if (!(options.get("prependModulePath", "") instanceof CharSequence)) {
		throw new IllegalArgumentException("prependModulePath has to be a string.")
	}

	/**
	 * LUT for the paths to files that may be sourced to make the module command available on different machines.
	 * Keys are regex expressions that are supposed to match the respective hostname.
	 */
	HashMap<String, String> moduleInitPaths = ["helvetica"                      : "/wang/environment/software/Modules/bashrc",
	                                           "AMTHost\\d+"                    : "/wang/environment/software/Modules/bashrc",
	                                           "HBPHost\\d+"                    : "/wang/environment/software/Modules/bashrc",
	                                           "ome.*|vrhea.*|vtitan.*|uranus.*": "/usr/local/Modules/current/init/bash"]

	List<String> prefixCommands = new ArrayList()

	// Get module command
	if (((String) options.get("moduleInitPath"))?.length()) {
		prefixCommands.add("source ${options.get("moduleInitPath")}")
	} else {
		if (jesh(script: "which module >/dev/null 2>&1", returnStatus: true)) {
			// We don't already have a module command, try to get one
			String hostname = jesh(script: "hostname", returnStdout: true).trim()
			String moduleInitPath

			// Search for a module init command that belongs to the hostname
			int foundInitPaths = 0
			for (String hostRegex in moduleInitPaths.keySet()) {
				if (hostname.matches(hostRegex)) {
					moduleInitPath = moduleInitPaths.get(hostRegex)
					foundInitPaths += 1
				}
			}

			if (moduleInitPath == null) {
				throw new IllegalStateException("No module init file registered for host $hostname.")
			}

			if (foundInitPaths > 1) {
				echo "[withModules] Multiple module init files found for host $hostname, chosing $moduleInitPath"
			}

			if (!fileExists(moduleInitPath)) {
				throw new InternalError("Expected file $moduleInitPath not found on host $hostname.")
			}

			prefixCommands.add("source $moduleInitPath")
		}
	}

	// For runs in container, use module command from spack
	prefixCommands.add("[ -f /opt/spack/bin/spack ] && " +
	                   "source \$(/opt/spack/bin/spack location -i \"environment-modules\")/Modules/init/bash || " +
	                   "true")

	// Keep module command alive if it's not yet exported. Don't fail if it's not a function.
	prefixCommands.add("export -f module || true")

	if (options.get("prependModulePath")?.length()) {
		prefixCommands.add("export MODULEPATH=${options.get("prependModulePath")}\${MODULEPATH:+:\${MODULEPATH}}")
	}

	if ((boolean) options.get("purge", false)) {
		prefixCommands.add("module purge")
	}

	for (String module in (List<String>) options.get("modules")) {
		prefixCommands.add("module load $module")
	}

	ShellManipulator manipulator = new ShellManipulator(this)
	manipulator.add(prefixCommands.join(" && ") + " &&", "")

	// module load sometimes fails with exit code 0, make sure all modules load correctly
	String modules = jesh(script: "module list 2>&1", returnStdout: true)
	for (String module in (List<String>) options.get("modules")) {
		if (!modules.contains(module)) {
			manipulator.restore()
			throw new IllegalStateException("[withModules] module load was not successful! $module is missing.")
		}
	}

	try {
		content()
	} catch (Throwable anything) {
		throw anything
	} finally {
		manipulator.restore()
	}
}
