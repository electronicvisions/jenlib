/**
 * Create a list of all apps available for a given singularity container.
 *
 * @param containerPath Path to the container in question. {@code null} leads to the default path being used.
 * @return list of apps in a given singularity container
 */
List<String> call(String containerPath) {
	String containerImage = containerPath ?: getDefaultContainerPath()

	// modern singularity versions (starting 3.4) and all apptainer versions do not support the apps subcommand anymore
	// examples:
	// apptainer version 1.3.6
	// singularity version 3.1.0-rc2.37.ge3f4c52
	if (jesh(script: "singularity --version", returnStdout: true).contains("apptainer")) {
		return jesh(script: "singularity -s inspect --list-apps $containerImage", returnStdout: true).split("\n").collect { it.trim() }
	} else {
		return jesh(script: "singularity apps $containerImage", returnStdout: true).split("\n").collect { it.trim() }
	}
}

List<String> call() {
	call(null)
}
