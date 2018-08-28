/**
 * Send a notification that this job has failed.
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

	if (!nonGerritOnly | !(boolean) env.GERRIT_CHANGE_NUMBER) {
		mattermostSend(channel: mattermostChannel,
		               text: "@channel Jenkins build `${env.JOB_NAME}` has failed!",
		               message: "${env.BUILD_URL}",
		               endpoint: "https://brainscales-r.kip.uni-heidelberg.de:6443/hooks/qrn4j3tx8jfe3dio6esut65tpr")
	}
}
