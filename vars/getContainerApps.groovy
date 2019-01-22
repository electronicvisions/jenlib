/**
 * Create a list of all apps available for a given singularity container.
 *
 * @param containerPath Path to the container in question. {@code null} leads to the default path being used.
 * @return list of apps in a given singularity container
 */
List<String> call(String containerPath) {
	String containerImage = containerPath ?: getDefaultContainerPath()

	return jesh(script: "singularity apps $containerImage", returnStdout: true).split("\n").collect { it.trim() }
}

List<String> call() {
	call(null)
}
