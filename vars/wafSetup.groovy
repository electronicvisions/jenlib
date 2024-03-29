/**
 * Pipeline step for checking out a list of waf projects, respecting
 * any gerrit changesets given in the current environment. Note that
 * Gerrit changes in upstream jobs will not be passed on to downstream
 * builds and are currently not honoured within `wafSetup`.
 *
 * @param options Map of options for setting up the waf projects:
 *                <ul>
 *                    <li><b>projects</b> (mandatory): List of projects to be built
 *                    <li><b>setupOptions</b> (optional): Options passed to the <code>waf setup</code> call.
 *                                                        Defaults to <code>"--clone-depth=1"</code>
 *                    <li><b>noExtraStage</b> (optional): Don't wrap the waf setup in a dedicated stage.
 *                                                        Defaults to <code>false</code>.
 *                    <li><b>ignoreGerritChanges</b> (optional): Don't checkout any gerrit changeset, even if the build
 *                                                               has been triggered by one. This option is intended to
 *                                                               be used for "latest" builds, where HEAD should be
 *                                                               built. Defaults to <code>false</code>.
 *                </ul>
 */
def call(Map<String, Object> options = [:]) {
	boolean noExtraStage = options.get("noExtraStage", false)

	if (noExtraStage) {
		return impl(options)
	}

	stage("Waf Setup") {
		return impl(options)
	}
}

private void impl(Map<String, Object> options = [:]) {
	if (!options.containsKey("projects")) {
		throw new IllegalArgumentException("No projects given to set up.")
	}

	if (!(options.get("projects") instanceof List)) {
		throw new IllegalArgumentException("Projects have to be a list.")
	}

	List<String> projects = (List<String>) options.get("projects")
	final String setupOptions = options.get("setupOptions", "--clone-depth=2")
	final boolean ignoreGerritChanges = options.get("ignoreGerritChanges", false)

	List<String> checkoutCommand = []
	checkoutCommand.add("waf")
	checkoutCommand.add("setup")
	checkoutCommand.add(([""] + projects).join(" --project ").trim())
	checkoutCommand.add(setupOptions)

	if (!ignoreGerritChanges) {
		if (env.GERRIT_CHANGE_NUMBER) {
			checkoutCommand.add("--gerrit-changes=${GERRIT_CHANGE_NUMBER}")
			checkoutCommand.add("--gerrit-url=ssh://${getGerritUsername()}@${GERRIT_HOST}:${GERRIT_PORT}")
		}
	}

	withWaf() {
		runOnSlave(label: "frontend") {
			jesh(checkoutCommand.join(" "))
		}
	}
}
