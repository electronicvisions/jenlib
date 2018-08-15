package org.electronicvisions.swarm

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
		if (!config.slaveJar) {
			throw new IllegalArgumentException("Config does not define a jar file.")
		}

		List<String> args = new ArrayList<>()

		if (config.javaBinary != null) {
			args.add("\"${config.javaBinary.toString()}\"")
		} else {
			args.add("java")
		}

		args.add("-jar")
		args.add("\"${config.slaveJar.toString()}\"")

		if (config.slaveName != null) {
			args.add("-name")
			args.add("\"${config.slaveName}\"")
			args.add("-disableClientsUniqueId")
		}

		if (config.mode != null) {
			args.add("-mode")
			args.add("\"${config.mode.toString()}\"")
		}

		if ((config.jenkinsWebProtocol != null) && (config.jenkinsHostname != null) && (config.jenkinsWebPort >= 0)) {
			args.add("-master")
			args.add("${config.jenkinsWebProtocol.toString()}${config.jenkinsHostname}:${config.jenkinsWebPort}")
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

		if ((config.jenkinsHostname != null) | (config.jenkinsJnlpPort >= 0)) {
			args.add("-tunnel")
			if ((config.jenkinsHostname != null) && (config.jenkinsJnlpPort >= 0)) {
				args.add("${config.jenkinsHostname}:${config.jenkinsJnlpPort}")
			} else if (config.jenkinsHostname != null) {
				args.add("${config.jenkinsHostname}:")
			} else {  // only jenkinsJnlpPort
				args.add(":${config.jenkinsJnlpPort}")
			}
		}

		if (config.workspace != null) {
			args.add("-fsroot")
			args.add("\"${config.workspace.toString()}\"")
		}

		return args.join(" ").trim()
	}
}
