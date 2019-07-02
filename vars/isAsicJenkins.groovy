/**
 * Check whether we are running on KIP ASIC Jenkins Server or the internal Visionary Jenkins.
 *
 * @return `true` if running on ASIC
 */
boolean call() {
	return "129.206.177.132" == Jenkins.getInstance().getComputer('').getHostName()
}
