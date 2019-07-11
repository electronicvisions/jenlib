/**
 * Send a notification that this job has failed.
 *
 * A notification is <b>not sent</b> if any of these apply:
 * <ul>
 *  <li>The change has been triggered by a gerrit event that was not a merge</li>
 *  <li>The change has been triggered manually</li>
 * </ul>
 *
 * @param options Map of options. Currently supported: String mattermostChannel (mandatory).
 */
def call(Map<String, Object> options = [:]) {

	if (!("mattermostChannel" in options.keySet())) {
		throw new IllegalArgumentException("mattermostChannel is a mandatory argument.")
	}

	String mattermostChannel = options.get("mattermostChannel")

	List<Boolean> noSendReasons = [isTriggeredByGerrit() && (env.GERRIT_EVENT_TYPE != "change-merged"),
	                               isTriggeredByUserAction()]

	if (noSendReasons.any()) {
		return
	}

	mattermostSend(channel: mattermostChannel,
	               text: "@channel Jenkins build `${env.JOB_NAME}` has failed!",
	               message: "${env.BUILD_URL}",
	               failOnError: true,
	               endpoint: "https://chat.bioai.eu/hooks/qrn4j3tx8jfe3dio6esut65tpr")
}
