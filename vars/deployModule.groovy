import java.nio.file.Paths
import java.time.Instant
import java.text.SimpleDateFormat

/**
 * Deploy content to module. Save container name to module's help.
 *
 * Example:
 * 	<pre>
 * 	    inSingularity() {
 * 	        deployModule([name: "ppu", source: "install"])
 * 	    }
 * 	</pre>
 *
 * @param options Map of options of module deployment.
 */
def call(Map<String, String> options = [:]) {
	String container = sish(returnStdout: true, script: "echo \$SINGULARITY_CONTAINER").trim()
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

	String targetDir = Paths.get(targetRoot, moduleName).toString()
	String moduleDir = Paths.get(moduleRoot, moduleName).toString()

	String moduleFileTemplate = libraryResource 'org/electronicvisions/modulefile'
	String moduleVersionTemplate = libraryResource 'org/electronicvisions/.version'

	runOnSlave(label: "frontend") {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd")
		String date = formatter.format(Date.from(Instant.now()))

		String num = sish(returnStdout: true,
		                  script: "num=1 && while [[ -e '$targetDir/$date-\$num' ]] ; do let num++; done && echo \$num").trim()

		String version = "$date-$num"

		// Create module directories, if they do not exist
		sish "mkdir -p ${targetDir}"
		sish "mkdir -p ${moduleDir}"
		sish "cp -a $source ${Paths.get(targetDir, version).toString()}"

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
}
