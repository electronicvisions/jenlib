/**
 * Send a notification that this job has failed.
 *
 * A notification is sent if any of these apply:
 * <ul>
 *  <li>{@code nonGerritOnly} is {@code false}</li>
 *  <li>The change has been triggered by a merge-event in gerrit</li>
 * </ul>
 *
 * Otherwise (this especially includes non-merge gerrit events!), no notification is sent.
 *
 * @param options Map of options. Currently supported: String mattermostChannel, boolean nonGerritOnly
 *                mattermostChannel is mandatory.
 */
def call(Map<String, Object> options = [:]) {

	if (!("mattermostChannel" in options.keySet())) {
		throw new IllegalArgumentException("mattermostChannel is a mandatory argument.")
	}

	String mattermostChannel = options.get("mattermostChannel")
	boolean nonGerritOnly = options.get("nonGerritOnly", true)

	if (!nonGerritOnly | (isGerritTriggered() == (env.GERRIT_EVENT_TYPE == "change-merged"))) {
		mattermostSend(channel: mattermostChannel,
		               text: "@channel Jenkins build `${env.JOB_NAME}` has failed!",
		               message: "${env.BUILD_URL}",
		               failOnError: true,
		               endpoint: "https://chat.bioai.eu/hooks/qrn4j3tx8jfe3dio6esut65tpr")
	}
}
