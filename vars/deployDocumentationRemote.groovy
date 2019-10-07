import static java.util.UUID.randomUUID
import java.nio.file.Paths

/**
 * Deploy a list of folders to a specified git repository.
 *
 * @param options Map of options:
 *                <ul>
 *                    <li><b>repositoryUrl</b> URL of the repository to deploy to
 *                    <li><b>upstreamBranch</b> Upstream branch name
 *                    <li><b>keyFile</b> SSH keyfile
 *                    <li><b>folders</b> List of folders to deploy
 *                </ul>
 */
void call(Map<String, Object> options = [:]) {
	if (options.get("upstreamBranch") == null) {
		throw new IllegalArgumentException("Upstream branch is a mandatory argument.")
	}

	if (options.get("repositoryUrl") == null) {
		throw new IllegalArgumentException("Repository URL is a mandatory argument.")
	}

	if (options.get("folders") == null) {
		throw new IllegalArgumentException("Folders is a mandatory argument.")
	}

	keyFile = options.get("keyFile")

	if (keyFile != null) {
		if (!fileExists(keyFile)) {
			error("File $keyFile does not exist.")
		}
	}

	inSingularity() {
		runOnSlave(label: "frontend") {
			String tmpDir = Paths.get(steps.pwd(tmp: true), "jenkins_docu_" + randomUUID().toString()).toString()

			upstreamBranch = options.get("upstreamBranch")
			repositoryUrl = options.get("repositoryUrl")
			folders = options.get("folders")

			jesh("git clone --branch $upstreamBranch $repositoryUrl $tmpDir")
			dir(tmpDir) {
				jesh("git rm -r --ignore-unmatch .")
			}
			for (String folder in folders) {
				jesh("cp -a $folder $tmpDir")
			}
			String indexTemplate = libraryResource 'org/electronicvisions/documentationIndex'
			String index = fillTemplate(indexTemplate, [folders: folders])
			writeFile(file: Paths.get(tmpDir, "index.html").toString(), text: index, encoding: "UTF-8")
			List<String> env_options = keyFile != null ? ["GIT_SSH_COMMAND=ssh -i $keyFile"] : []
			withEnv(env_options) {
				dir(tmpDir) {
					jesh("git add .")
					jesh("git commit --no-edit -m 'Automatic deployment for Jenkins Build ${env.BUILD_NUMBER}'")
					jesh("git push origin $upstreamBranch")
				}
			}

			jesh("rm -rf $tmpDir")
		}
	}
}
