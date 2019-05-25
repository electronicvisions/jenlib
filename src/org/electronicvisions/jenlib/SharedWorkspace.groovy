package org.electronicvisions.jenlib

import hudson.model.Run

import java.util.regex.Matcher


/**
 * Manager for build-unique shared workspaces between nodes.
 *
 * This class may be used for managing workspaces of jobs that use a common network filesystem for
 * data exchange between nodes: Each build gets an unique, fixed workspace that may be used on all
 * nodes.
 *
 * Workspace names will consist of the project name as a prefix and a base64url encoded postfix encoding
 * the build's unique ID. The delimiter in use is '#'.
 */
class SharedWorkspace {
	/**
	 * Filesystem root relative to which workspaces are managed
	 */
	private static final File workspaceRoot = new File("/jenkins/jenlib_workspaces")

	/**
	 * Get the shared workspace for the current build.
	 *
	 * @param steps Pipeline context
	 * @return Path to the workspace to be used
	 */
	static String getWorkspace(steps) {
		return build2File(steps).getCanonicalPath()
	}

	/**
	 * Cleanup all workspaces managed by this class.
	 *
	 * Workspaces are deleted as soon as the respective build is no longer running.
	 *
	 * @param steps Pipeline context
	 */
	static void cleanup(steps) {
		List<File> currentDirs = getCurrentWorkspaces(steps)

		for (File workspace : currentDirs) {
			if (file2Build(workspace)?.isBuilding()) {
				continue
			}
			steps.echo("[Jenlib SharedWorkspace] Cleanup: Removing '${workspace}'")
			steps.jesh("rm -rf '${workspace}'")
		}
	}

	/**
	 * Get the workspace directory file for the current build.
	 *
	 * Workspace names follow the convention projectName#base64url(buildId)
	 *
	 * @param steps Pipeline context
	 * @return Workspace directory
	 */
	private static File build2File(steps) {
		String projectName = steps.currentBuild.rawBuild.getParent().getDisplayName()
		assert (!projectName.contains("#")): "Jenkins project name must not contain '#'!"
		String identifier = steps.currentBuild.rawBuild.getExternalizableId()
		String encodedId = Base64.getUrlEncoder().withoutPadding().encodeToString(identifier.getBytes())

		// We prepend the encoded id by the project name for readability
		// '#' is not part of the base64url alphabet
		String workspaceName = projectName + "#" + encodedId
		return new File(workspaceRoot, workspaceName)
	}

	/**
	 * Get the matching jenkins build for some workspace directory.
	 *
	 * @param workspace Workspace directory the build is searched for
	 * @return Matching jenkins build, null if no matching build is found
	 */
	private static file2Build(File workspace) {
		Matcher idMatcher = (workspace.getName() =~ /^.+#(?<id>.+?)(__.+)?$/)
		if (!idMatcher.matches()) {
			return null
		}
		String identifier = new String(Base64.getUrlDecoder().decode(idMatcher.group("id")))

		try {
			def build = getBuild(identifier)
			if (build != null) {
				return build
			}
		} catch (IllegalArgumentException ignored) {
			return null
		}
		return null
	}

	/**
	 * List all files and folders that are direct descendants of {@link SharedWorkspace#workspaceRoot}.
	 * Nested nodes are not listed.
	 *
	 * @param steps Pipeline context
	 * @return List of files in {@link SharedWorkspace#workspaceRoot}
	 */
	private static List<File> getCurrentWorkspaces(steps) {
		return steps.jesh(script: "ls -1 ${workspaceRoot}", returnStdout: true).
				split("\n").collect { new File(workspaceRoot, it.trim()) }
	}

	/**
	 * Retrieve a Jenkins build from a given unique identifier in the form 'job/path#build_number'.
	 *
	 * This method may be overwritten for unit-testing without available Jenkins instances.
	 *
	 * @param externalizableId ID the Jenkins build is searched for
	 * @return Jenkins build if found, else null
	 */
	static protected getBuild(String externalizableId) {
		return Run.fromExternalizableId(externalizableId)
	}
}
