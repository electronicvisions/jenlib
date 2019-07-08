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
	 * Get filesystem root relative to which workspaces are managed
	 *
	 * @param steps Pipeline context
	 * @return Path to the workspace root
	 */
	static File getWorkspaceRoot(steps) {
		String group = steps.isAsicJenkins() ? "s5" : "f9"
		return new File("/jenkins/jenlib_workspaces_${group}")
	}

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
	 * Workspace names follow the convention projectName.base64url(buildId).
	 *
	 * @param steps Pipeline context
	 * @return Workspace directory
	 */
	private static File build2File(steps) {
		String projectName = steps.currentBuild.rawBuild.getParent().getDisplayName()
		assert (!projectName.contains(".")): "Jenkins project name must not contain '.'!"
		String identifier = steps.currentBuild.rawBuild.getExternalizableId()
		String encodedId = Base64.getUrlEncoder().withoutPadding().encodeToString(identifier.getBytes())

		// We prepend the encoded id by the project name for readability
		// Id-delimiting '.'s are not part of the base64url alphabet
		// Append an arbitrary character "x", some tools don't like folders ending with "."
		String workspaceName = projectName + "." + encodedId + "." + "x"
		return new File(getWorkspaceRoot(steps), workspaceName)
	}

	/**
	 * Get the matching jenkins build for some workspace directory.
	 *
	 * @param workspace Workspace directory the build is searched for
	 * @return Matching jenkins build, null if no matching build is found
	 */
	private static file2Build(File workspace) {
		// Jenkins postfixes workspaces => actual id is delimited by '.'s
		Matcher idMatcher = (workspace.getName() =~ /^.+?\.(?<id>.+?)(\..*)?$/)
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
	 * List all files and folders that are direct descendants of {@link SharedWorkspace#getWorkspaceRoot}.
	 * Nested nodes are not listed.
	 *
	 * @param steps Pipeline context
	 * @return List of files in {@link SharedWorkspace#getWorkspaceRoot}
	 */
	private static List<File> getCurrentWorkspaces(steps) {
		String dirListing = steps.jesh(script: "ls -1 ${getWorkspaceRoot(steps).getCanonicalPath()}",
		                               returnStdout: true).trim()

		// Splitting a zero-length string results in a list of length 1
		if (dirListing.length() == 0) {
			return new ArrayList<File>()
		}

		return dirListing
				.split("\n")
				.collect { new File(getWorkspaceRoot(steps), it.trim()) }
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
