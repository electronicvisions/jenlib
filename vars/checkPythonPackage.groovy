/**
 * Default static code analyses for python packages.
 *
 * @param options : Hashmap of supplied options, at least 'pkg: "something"' has to be present.
 */
def call(LinkedHashMap options) {
	if (!options.containsKey("pkg")) {
		throw new IllegalArgumentException("No package given.")
	}

	File package_dir = new File(options.get("pkg") as String)
	String package_name = package_dir.getName()
	String package_parentdir = package_dir.getParent()

	stage("Python Checking: ${package_name}") {

		// Get most recent code formatting guidelines
		runOnSlave(label: "frontend") {
			dir("code-format") {
				withWaf {
					try {
						jesh "waf setup --project code-format " +
						     "--gerrit-changes=${GERRIT_CHANGE_NUMBER} " +
						     "--gerrit-url=ssh://${getGerritUsername()}@$GERRIT_HOST:$GERRIT_PORT"
					} catch (MissingPropertyException ignored) {
						jesh "waf setup --project code-format"
					}
				}
			}
		}

		// PEP8
		jesh("pycodestyle --config=./code-format/code-format/pycodestyle ${package_dir.toString()} " +
		     "> pep8_report_${package_name}.txt || exit 0")
		warnings canComputeNew: false,
		         unstableTotalAll: '0',
		         parserConfigurations: [[parserName: 'Pep8', pattern: "pep8_report_${package_name}.txt"]]

		// PyLint
		withEnv(["PYTHONPATH+WHATEVER=${package_parentdir}"]) {
			// Linting
			jesh("pylint --rcfile ./code-format/code-format/pylintrc ${package_name} " +
			     "> pylint_report_${package_name}.txt|| exit 0")

			// Python3 compatibility, don't check absolute-import future statements, we require absolute imports anyways
			jesh("pylint --py3k --disable=no-absolute-import --rcfile " +
			     "./code-format/code-format/pylintrc ${package_name} " +
			     ">> pylint_report_${package_name}.txt|| exit 0")
		}
		warnings canComputeNew: false,
		         unstableTotalAll: '0',
		         parserConfigurations: [[parserName: 'PyLint', pattern: "pylint_report_${package_name}.txt"]]
	}
}
