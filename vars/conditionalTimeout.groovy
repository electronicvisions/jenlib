/**
 * Run a closure conditionally in a timeout step.
 *
 * @param options Map of options:
 *                <ul>
 *                    <li><b>enable</b> (mandatory): Enable the timeout
 *                    <li><b><i>others</i></b> (mandatory): See https://www.jenkins.io/doc/pipeline/steps/workflow-basic-steps/#timeout-enforce-time-limit.
 *                </ul>
 * @param content Code to be run
 */
def call(Map<String, Object> options, Closure content) {
	Map<String, Object> internalOptions = options.clone()

	Boolean enable = internalOptions.get("enable")
	if (enable == null) {
		throw new IllegalArgumentException("'enable' parameter is mandatory.")
	}
	internalOptions.remove("enable")

	if (enable) {
		timeout(internalOptions) {
			content()
		}
	} else {
		content()
	}
}
