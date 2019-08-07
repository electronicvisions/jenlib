import java.nio.file.Paths
import java.time.Instant
import java.text.SimpleDateFormat

/**
 * Deploy content to module. Save container name to module's help.
 *
 * If `deployModule` is used in a Job triggered by a non-merge gerrit event, the module's name will be postfixed
 * by "_testing" and the version identifier will encode the triggering patch set.
 * Testing modules are purged depending on the given list of `cleanupEvents` (gerrit event types). Defaults
 * are {@code ["change-merged", "change-abandoned"]}
 *
 * Example:
 * 	<pre>
 * 	    inSingularity() {
 * 	        deployModule([name: "ppu",
 * 	                      source: "install",
 * 	                      cleanupEvents: ["change-merged", "change-abandoned"]])
 * 	    }
 * 	</pre>
 *
 * @param options Map of options of module deployment.
 * @return Deployed module name
 */
String call(Map<String, String> options = [:]) {
	String container = jesh(returnStdout: true, script: "echo \$SINGULARITY_CONTAINER").trim()
	if (container == "") {
		throw new IllegalStateException("Module deployment only works in the container.")
	}

	String moduleRoot = Paths.get(options.get("moduleRoot", "/wang/environment/modules")).toString()
	String targetRoot = Paths.get(options.get("targetRoot", "/wang/environment/software/jessie")).toString()

	String source = Paths.get(options.get("source")).toString()
	if (source == null) {
		throw new IllegalStateException("No source directory specified to install.")
	}

	String moduleName = options.get("name")
	if (moduleName == null) {
		throw new IllegalStateException("No module name specified.")
	}

	// List of gerrit events that trigger a cleanup of all previous modules of the respective changeset
	List<String> cleanupEvents = (List<String>) options.get("cleanupEvents",
	                                                        ["change-merged", "change-abandoned"])

	String changeIdentifier = ""
	if (isTriggeredByGerrit()) {
		runOnSlave(label: "frontend") {
			// Cleanup of old changesets
			if (env.GERRIT_EVENT_TYPE in cleanupEvents) {
				// remove Modulefiles
				int modFileError = jesh(script: "rm -rf ${moduleRoot}/${moduleName}_testing/*-c${env.GERRIT_CHANGE_NUMBER}p*",
				                        returnStatus: true)

				// remove deployed data
				int modDataError = jesh(script: "rm -rf ${targetRoot}/${moduleName}_testing/*-c${env.GERRIT_CHANGE_NUMBER}p*",
				                        returnStatus: true)

				if (modFileError || modDataError) {
					throw new RuntimeException("Could not completely remove old module '${moduleName}'")
				}
			}

			if (env.GERRIT_EVENT_TYPE != "change-merged") {
				changeIdentifier = "-c${env.GERRIT_CHANGE_NUMBER}p${env.GERRIT_PATCHSET_NUMBER}"
			}
		}
	}


	// Postpend module name by "_testing" for non-merge gerrit changes
	if (isTriggeredByGerrit() && (env.GERRIT_EVENT_TYPE != "change-merged")) {
		moduleName = moduleName + "_testing"
	}

	String targetDir = Paths.get(targetRoot, moduleName).toString()
	String moduleDir = Paths.get(moduleRoot, moduleName).toString()

	String moduleFileTemplate = libraryResource 'org/electronicvisions/modulefile'
	String moduleVersionTemplate = libraryResource 'org/electronicvisions/.version'

	// Version identifier for the deployed module
	String version = ""

	runOnSlave(label: "frontend") {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd")
		String date = formatter.format(Date.from(Instant.now()))

		// Version name without increment
		String rawVersion = "$date$changeIdentifier"

		String num = jesh(returnStdout: true,
		                  script: "num=1 && " +
		                          "while [[ -e \"$targetDir/$rawVersion-\$num\" ]]; " +
		                          "do let num++; done && " +
		                          "echo \$num").trim()

		version = "$rawVersion-$num"

		// Create module directories, if they do not exist
		jesh "mkdir -p ${targetDir}"
		jesh "mkdir -p ${moduleDir}"
		jesh "cp -a $source ${Paths.get(targetDir, version).toString()}"

		// Construct module file and default module version
		String moduleFileContent = fillTemplate(moduleFileTemplate, [CONTAINER: container,
		                                                             MODULEDIR: Paths.get(targetDir, version).toString(),
		                                                             MODNAME  : moduleName,
		                                                             VERSION  : version])
		String moduleVersionContent = fillTemplate(moduleVersionTemplate, [VERSION: version])

		dir(moduleDir) {
			writeFile(file: ".version", text: moduleVersionContent)
			writeFile(file: version, text: moduleFileContent)
		}
	}

	return "${Paths.get(moduleName, version).toString()}"
}
