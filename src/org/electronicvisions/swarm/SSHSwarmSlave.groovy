package org.electronicvisions.swarm

/**
 * {@link SwarmSlave} implementation for starting slaves on SSH-accessible remote machines.
 *
 * Slaves are started as SystemD user services.
 */
class SSHSwarmSlave extends SwarmSlave {
	/**
	 * Username for login on the remote machine
	 */
	private final String username

	/**
	 * Remote host the slave is started on
	 */
	private final String host

	/**
	 * SystemD unit name for the slave.
	 * Since only one swarm client can be started per host, no randomization is needed.
	 */
	private final String systemdUnitName = "jenkins-swarm-slave"

	/**
	 * State of the swarm slave
	 */
	private boolean running = false

	/**
	 * Constructor for {@link SSHSwarmSlave}
	 *
	 * @param steps Pipeline steps, to be passed as {@code this} from the calling pipeline
	 * @param config Swarm slave configuration to be used for this slave
	 * @param username SSH username
	 * @param host SSH host the slave is to be started on
	 */
	SSHSwarmSlave(steps, SwarmSlaveConfig config, String username, String host) {
		super(steps, config)

		SwarmSlaveConfig.validateUsername(username)
		this.username = username

		SwarmSlaveConfig.validateHostname(host)
		this.host = host
	}

	/**
	 * Start the swarm slave.
	 * If the SSH command returns 0, {@link SSHSwarmSlave#running} is set {@code true}.
	 */
	void startSlave() {
		if (running) {
			throw new IllegalStateException("Swarm client is already running.")
		}
		this.steps.sh "ssh $username@$host '$systemdServiceStartCommand'"
		running = true
	}

	/**
	 * Stop the swarm slave.
	 * If the SSH command returns 0, {@link SSHSwarmSlave#running} is set {@code false}.
	 */
	void stopSlave() {
		if (!running) {
			throw new IllegalStateException("Swarm client is not running.")
		}
		this.steps.sh "ssh $username@$host '$systemdServiceStopCommand'"
		running = false
	}

	/**
	 * Build the SystemD service startup command.
	 *
	 * @return {@code systemd-run} command for starting the slave
	 */
	private String getSystemdServiceStartCommand() {
		return "systemd-run " +
		       (config.javaHome ? "--setenv=JAVA_HOME=${config.javaHome} " : "") +
		       "--user " +
		       "--unit=$systemdUnitName " +
		       "${buildSlaveStartCommand()}"
	}

	/**
	 * Build the SystemD service stop command.
	 *
	 * @return {@code systemctl} command for stopping the slave
	 */
	private String getSystemdServiceStopCommand() {
		return "systemctl kill --user systemdUnitName && " +

		       /**
		        *  Java exits with 143 when {@code SIGTERM} is received.
		        *  As it does not seem possible to implement the solution proposed in the link below
		        *  when using systemctl-run, we have to reset the service after stopping.
		        *
		        * @see <a href="https://stegard.net/2016/08/gracefully-killing-a-java-process-managed-by-systemd/">[stegard.net]</a>
		        */
		       "systemctl reset-failed --user $systemdUnitName"
	}
}
