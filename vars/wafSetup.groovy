/**
 * Pipeline step for checking out a list of waf projects, respecting
 * any gerrit changesets given in the current environment.
 *
 * @param options Map of options for setting up the waf projects:
 *                <ul>
 *                    <li><b>projects</b> (mandatory): List of projects to be built
 *                    <li><b>wafOptions</b> (optional): Options passed to <code>withWaf</code>
 *                    <li><b>setupOptions</b> (optional): Options passed to the <code>waf setup</code> call.
 *                                                        Defaults to <code>"--clone-depth=1"</code>
 *                    <li><b>noExtraStage</b> (optional): Don't wrap the waf setup in a dedicated stage.
 *                                                        Defaults to <code>false</code>.
 *                </ul>
 */
def call(Map<String, Object> options = [:]) {
	boolean noExtraStage = options.get("noExtraStage", false)

	if (noExtraStage) {
		return impl(options)
	}

	stage('Waf Setup') {
		return impl(options)
	}
}

def impl(Map<String, Object> options = [:]) {
	if (!options.containsKey("projects")) {
		throw new IllegalArgumentException("No projects given to set up.")
	}

	if (!(options.get("projects") instanceof List)) {
		throw new IllegalArgumentException("Projects have to be a list.")
	}

	List<String> projects = options.get("projects") as List<String>

	projects.add(0, "")
	String setupOptions = options.get("setupOptions", "--clone-depth=1")
	String projectCommand = projects.join(" --project ").trim()

	withWaf(options.get("wafOptions", [:])) {
		runOnSlave(label: "frontend") {
			if (env.GERRIT_CHANGE_NUMBER) {
				jesh("waf setup ${projectCommand} ${setupOptions} " +
				     "--gerrit-changes=${GERRIT_CHANGE_NUMBER} " +
				     "--gerrit-url=ssh://${getGerritUsername()}@${GERRIT_HOST}:${GERRIT_PORT}")
			} else {
				jesh("waf setup ${projectCommand} ${setupOptions}")
			}
		}
	}
}
