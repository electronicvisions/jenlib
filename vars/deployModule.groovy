import java.nio.file.Paths
import java.time.Instant
import java.text.SimpleDateFormat

/**
 * Deploy content to module. Save container name to module's help.
 *
 * The deployed module's name has to be given by a 'name'.
 * The deployed module's version defaults to an enumerated date string (e.g. '2019-08-15-1').
 * The latter may be overwritten by the 'version' parameter.
 * Newly deployed module is set as new default version of the module. This can be adjusted with
 * the parameter 'setAsDefault' which defaults to true.
 *
 * Example:
 * 	<pre>
 * 	    inSingularity() {
 * 	        deployModule(name: "ppu",
 * 	                     source: "install",
 * 	                     version: "c7311p24",
 * 	                     setAsDefault: true)
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
	String targetRoot = Paths.get(options.get("targetRoot", "/wang/environment/software/container")).toString()

	String source = Paths.get(options.get("source")).toString()
	if (source == null) {
		throw new IllegalStateException("No source directory specified to install.")
	}

	String moduleName = options.get("name")
	if (moduleName == null) {
		throw new IllegalStateException("No module name specified.")
	}

	String version = options.get("version")

	String targetDir = Paths.get(targetRoot, moduleName).toString()
	String moduleDir = Paths.get(moduleRoot, moduleName).toString()

	String moduleFileTemplate = libraryResource 'org/electronicvisions/modulefile'
	String moduleVersionTemplate = libraryResource 'org/electronicvisions/.version'

	runOnSlave(label: "frontend") {
		if (version == null) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd")
			String date = formatter.format(Date.from(Instant.now()))

			final String enumeratedTarget = createEnumeratedDirectory(Paths.get(targetDir, date).toString())
			version = jesh(script: "basename \"${enumeratedTarget}\"", returnStdout: true).trim()
		}

		// Create module directories, if they do not exist
		jesh "mkdir -p ${targetDir}"
		jesh "mkdir -p ${Paths.get(targetDir, version).toString()}"
		jesh "mkdir -p ${moduleDir}"
		jesh "cp -a $source ${Paths.get(targetDir, version).toString()}"

		// Construct module file and default module version
		String moduleFileContent = fillTemplate(moduleFileTemplate, [CONTAINER: container,
		                                                             MODULEDIR: Paths.get(targetDir, version).toString(),
		                                                             MODNAME  : moduleName,
		                                                             VERSION  : version])

		dir(moduleDir) {
			if (options.get("setAsDefault", true)) {
				String moduleVersionContent = fillTemplate(moduleVersionTemplate, [VERSION: version])
				writeFile(file: ".version", text: moduleVersionContent)
			}
			writeFile(file: version, text: moduleFileContent)
		}
	}

	return "${Paths.get(moduleName, version).toString()}"
}
