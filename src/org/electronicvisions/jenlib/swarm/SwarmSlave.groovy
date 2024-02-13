package org.electronicvisions.jenlib.swarm

/**
 * Abstraction for the Jenkins Swarm Slave client.
 *
 * @see <a href="https://wiki.jenkins.io/display/JENKINS/Swarm+Plugin">[jenkins.io]</a>
 */
abstract class SwarmSlave {

	/**
	 * Pipeline context
	 */
	protected final def steps

	/**
	 * Configuration of this swarm slave
	 */
	protected final SwarmSlaveConfig config

	/**
	 * Constructor for {@link SwarmSlave}
	 *
	 * @param steps : Pipeline steps, to be passed as {@code this} from the calling pipeline
	 * @param config : Swarm slave configuration to be used for this slave
	 */
	SwarmSlave(steps, SwarmSlaveConfig config) {
		this.steps = steps
		this.config = config
	}

	/**
	 * Start the swarm slave.
	 */
	abstract void startSlave()

	/**
	 * Stop the swarm slave.
	 */
	abstract void stopSlave()

	/**
	 * Build the swarm plugin start command based on this slave's configuration
	 *
	 * @return Complete slave startup command
	 */
	protected String buildSlaveStartCommand() {
		List<String> args = new ArrayList<>()

		args.add("set -exuo pipefail;")
		args.add("jarfile=\$(mktemp --suffix=.jar);")
		args.add("fsroot=\$(mktemp -d);")
		args.add('trap \\"rm -rf -- \\${jarfile} \\${fsroot}\\" EXIT;')

		args.add("curl ${config.jenkinsWebAddress}/swarm/swarm-client.jar > \\\${jarfile};")

		if (config.javaBinary != null) {
			args.add("\"${config.javaBinary.toString()}\"")
		} else {
			args.add("java")
		}

		if (config.loggingConfig != null) {
			args.add("-Djava.util.logging.config.file=\"${config.loggingConfig.toString()}\"")
		}

		args.add("-jar")
		args.add("\\\${jarfile}")

		args.add("-master")
		args.add(config.jenkinsWebAddress)

		if (config.slaveName != null) {
			args.add("-name")
			args.add("\"${config.slaveName}\"")
			args.add("-disableClientsUniqueId")
		}

		if (config.mode != null) {
			args.add("-mode")
			args.add("\"${config.mode.toString()}\"")
		}

		if (config.jenkinsUsername != null) {
			args.add("-username")
			args.add("\"${config.jenkinsUsername}\"")
		}

		if (config.jenkinsKeyfile != null) {
			args.add("-passwordFile")
			args.add("\"${config.jenkinsKeyfile.toString()}\"")
		}

		if (config.numExecutors >= 0) {
			args.add("-executors")
			args.add("\"${config.numExecutors.toString()}\"")
		}

		if (config.jenkinsJnlpPort >= 0) {
			args.add("-tunnel")
			args.add("${config.jenkinsHostname}:${config.jenkinsJnlpPort}")
		}

		args.add("-fsroot")
		if (config.fsroot != null) {
			args.add("\"${config.fsroot.toString()}\"")
		} else {
			args.add('\\${fsroot}')
		}

		return "bash -c \"${args.join(" ").trim()}\""
	}
}
