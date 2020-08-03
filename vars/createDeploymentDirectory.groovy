import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.Instant

/**
 * Create a 'stable' or 'testing' deployment directory at a given deploymentRoot path (e.g. <code>/my/files</code>).
 *
 * Non-merge builds triggered by gerrit will result in a <code>testing/</code> subfolder containing directories named
 * after the triggering change- and patchset as well as the current date.
 *
 * Merge-triggered builds as well as non-gerrit triggered builds will result in a <code>stable/</code> subfolder
 * containing directories named after the current date.
 *
 * @param deploymentRoot Folder path, <code>testing/</code> as well as <code>stable/</code> subdirectories will be
 *                       created there.
 * @return Absolute path to the deployment directory that has been created.
 */
String call(String deploymentRoot) {

	/**
	 * Conditions for stable deployment.
	 *
	 * Any of these will trigger stable deployment (as opposed to testing deployment).
	 */
	List<Boolean> stableDeploymentConditions = [
			!isTriggeredByGerrit(),
			env.GERRIT_EVENT_TYPE == "change-merged"
	]

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd")
	formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
	final String date = formatter.format(Date.from(Instant.now()))

	if (stableDeploymentConditions.any { it }) {
		return createEnumeratedDirectory(Paths.get(deploymentRoot, "stable", date).toString())
	}

	if (env.GERRIT_CHANGE_NUMBER == null) {
		throw new IllegalStateException("Cannot create 'testing' deployment directory without information about the " +
		                                "triggering gerrit changeset in the environment!")
	}

	final String changePatchId = "c${env.GERRIT_CHANGE_NUMBER}p${env.GERRIT_PATCHSET_NUMBER}"
	return createEnumeratedDirectory(Paths.get(deploymentRoot, "testing", "${changePatchId}_${date}").toString())
}
