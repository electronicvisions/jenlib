import static java.util.UUID.randomUUID
import java.nio.file.Paths

/**
 * Check source files of commit or whole repository for compliance with clang-format formatting.
 *
 * Example:
 * 	<pre>
 * 	    inSingularity() {
 * 	        checkClangFormat([folder: "haldls"])
 * 	    }
 * 	</pre>
 *
 * @param options Map of options of clang-format check.
 *                <ul>
 *                    <li><b>folder</b> (mandatory): Location of repository.
 *                    <li><b>reference</b> (optional): Reference from which to clang-format.
 *                                                     Defaults to HEAD.
 *                    <li><b>fullDiff</b> (optional): Whether to git clang-format from reference to reference~1 or the full repository.
 *                                                    Defaults to <code>false</code>.
 *                </ul>
 */
def call(Map<String, Object> options = [:]) {
	if (jesh(returnStatus: true, script: "which git-clang-format")) {
		throw new IllegalStateException("Git clang-format program not found.")
	}

	String folder = Paths.get(options.get("folder")).toString()
	if (folder == null) {
		throw new IllegalStateException("No directory specified to check.")
	}

	String reference = options.get("reference", "HEAD")
	Boolean fullDiff = options.get("fullDiff", false)

	// Get most recent code formatting guidelines
	runOnSlave(label: "frontend") {
		String tmpDir = Paths.get(steps.pwd(tmp: true), "code_format_" + randomUUID().toString()).toString()
		dir (tmpDir) {
			wafSetup(projects: ["code-format"], noExtraStage: true)
		}
		jesh "ln -s ${Paths.get(tmpDir, "code-format", "clang-format").toString()} .clang-format"

		// clang-format
		dir(folder) {
			Boolean hasParent = (Integer.parseInt(jesh(returnStdout: true, script: "git rev-list --count $reference").trim()) > 1)
			String extensions = "c,h,m,mm,cc,cp,cpp,c++,cxx,hh,hpp,hxx,tcc,cu,proto,protodevel,java,js,ts,cs"

			String diff
			if (fullDiff) {
				String currentBranch = jesh(returnStdout: true, script: "git rev-parse --abbrev-ref $reference").trim()

				jesh "git checkout --orphan empty"
				jesh "git read-tree --empty"
				jesh "git commit --allow-empty -m 'Empty'"
				jesh "git add ."
				jesh "git stash"
				jesh "git checkout $currentBranch"

				// exit 0 if exit 1, because git-clang-format since clang 14 introduced a bug (fixed in clang 20.1), where they
				// exit 1 if there are any ignored files, since also then the new and old tree
				// differ and they branch to "there's a difference", which requires exit 1.
				diff = jesh(
				    returnStdout: true,
				    script: "git clang-format --extensions $extensions --diff --style=file empty $reference; code=\$?; [ \$code -eq 1 ] && exit 0 || exit \$code")

				jesh "git branch -D empty"
			} else if (hasParent) {
				// exit 0 if exit 1, because git-clang-format since clang 14 introduced a bug (fixed in clang 20.1), where they
				// exit 1 if there are any ignored files, since also then the new and old tree
				// differ and they branch to "there's a difference", which requires exit 1.
				diff = jesh(
				    returnStdout: true,
				    script: "git clang-format --extensions $extensions --diff --style=file $reference~1; code=\$?; [ \$code -eq 1 ] && exit 0 || exit \$code")
			} else {
				throw new IllegalStateException("Clang-Format non-full-diff checking requires parent commit.")
			}

			if (!(diff.contains("clang-format did not modify any files") ||
			    diff.contains("no modified files to format") || diff == "")) {
				unstable("clang-format check failed, marking build 'UNSTABLE'.")
			}

			filename = "clang-format-diff-" + folder.replaceAll('/','-')
			writeFile(file: "${filename}", text: diff)
			archiveArtifacts("${filename}")

		}
		jesh "rm .clang-format"
		jesh "rm -rf $tmpDir"
	}
}
