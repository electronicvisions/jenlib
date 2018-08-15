package org.electronicvisions.swarm

import java.nio.file.Paths

/**
 * Full configuration of a single {@link SwarmSlave}.
 */
class SwarmSlaveConfig {
	/**
	 * Jenkins slave mode
	 */
	enum SlaveMode {
		NORMAL("normal"),
		EXCLUSIVE("exclusive")

		private final String text

		SlaveMode(final String text) {
			this.text = text
		}

		@Override
		String toString() { return text }
	}

	/**
	 * Protocol to be used for accessing the Jenkins Web UI.
	 */
	enum WebProtocol {
		HTTP("http://"),
		HTTPS("https://")

		private final String text

		WebProtocol(final String text) {
			this.text = text
		}

		@Override
		String toString() { return text }
	}

	/**
	 * Internal parameter for the absolute path to the Java binary to be used.
	 * Set by {@link SwarmSlaveConfig#setJavaHome}.
	 */
	private String javaBinary

	/**
	 * Absolute path to the {@code JAVA_HOME} to be used for running the jenkins slave.
	 */
	private String javaHome

	/**
	 * Hostname of the jenkins instance.
	 */
	private String jenkinsHostname

	/**
	 * JNLP Port of the jenkins instance.
	 */
	private int jenkinsJnlpPort = -1

	/**
	 * Absolute path to a file that contains the user's password for the Web UI.
	 * Used for on-demand creation of Build Executors.
	 */
	private String jenkinsKeyfile

	/**
	 * Web UI user for creating on-demand Build Executors.
	 */
	private String jenkinsUsername

	/**
	 * Port at which the Jenkins web server is accessible at {@link SwarmSlaveConfig#jenkinsHostname}.
	 */
	private int jenkinsWebPort = -1

	/**
	 * Protocol with which the Jenkins web server is accessible at {@link SwarmSlaveConfig#jenkinsHostname}.
	 */
	private WebProtocol jenkinsWebProtocol

	/**
	 * Slave mode the newly spawned slave is supposed to use.
	 */
	private SlaveMode mode

	/**
	 * Created slave's name.
	 */
	private String slaveName

	/**
	 * Number of Executors to provide on this slave.
	 */
	private int numExecutors = -1

	/**
	 * Absolute path to the {@code .jar} file of the swarm plugin.
	 */
	private String slaveJar

	/**
	 * Absolute path to the workspace-root.
	 */
	private String workspace

	/**
	 * Getter for {@link SwarmSlaveConfig#javaBinary}
	 */
	String getJavaBinary() {
		return javaBinary
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#javaHome}
	 */
	String getJavaHome() {
		return javaHome
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#javaHome}
	 */
	void setJavaHome(String javaHome) {
		validateFileString(javaHome)
		this.javaHome = javaHome
		this.javaBinary = Paths.get(javaHome.toString(), "bin", "java").toString()
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#jenkinsHostname}
	 */
	String getJenkinsHostname() {
		return jenkinsHostname
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#jenkinsHostname}
	 */
	void setJenkinsHostname(String jenkinsHostname) {
		validateHostname(jenkinsHostname)
		this.jenkinsHostname = jenkinsHostname
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#jenkinsJnlpPort}
	 */
	int getJenkinsJnlpPort() {
		return jenkinsJnlpPort
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#jenkinsJnlpPort}
	 */
	void setJenkinsJnlpPort(int jenkinsJnlpPort) {
		validatePortNumber(jenkinsJnlpPort)
		this.jenkinsJnlpPort = jenkinsJnlpPort
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#jenkinsKeyfile}
	 */
	String getJenkinsKeyfile() {
		return jenkinsKeyfile
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#jenkinsKeyfile}
	 */
	void setJenkinsKeyfile(String jenkinsKeyfile) {
		validateFileString(jenkinsKeyfile)
		this.jenkinsKeyfile = jenkinsKeyfile
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#jenkinsUsername}
	 */
	String getJenkinsUsername() {
		return jenkinsUsername
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#jenkinsUsername}
	 */
	void setJenkinsUsername(String jenkinsUsername) {
		validateUsername(jenkinsUsername)
		this.jenkinsUsername = jenkinsUsername
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#jenkinsWebPort}
	 */
	int getJenkinsWebPort() {
		return jenkinsWebPort
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#jenkinsWebPort}
	 */
	void setJenkinsWebPort(int jenkinsWebPort) {
		validatePortNumber(jenkinsWebPort)
		this.jenkinsWebPort = jenkinsWebPort
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#jenkinsWebProtocol}
	 */
	WebProtocol getJenkinsWebProtocol() {
		return jenkinsWebProtocol
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#jenkinsWebProtocol}
	 */
	void setJenkinsWebProtocol(WebProtocol jenkinsWebProtocol) {
		this.jenkinsWebProtocol = jenkinsWebProtocol
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#mode}
	 */
	SlaveMode getMode() {
		return mode
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#mode}
	 */
	void setMode(SlaveMode mode) {
		this.mode = mode
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#slaveName}
	 */
	String getSlaveName() {
		return slaveName
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#slaveName}
	 */
	void setSlaveName(String slaveName) {
		this.slaveName = slaveName
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#numExecutors}
	 */
	int getNumExecutors() {
		return numExecutors
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#numExecutors}
	 */
	void setNumExecutors(int numExecutors) {
		validateExecutorCount(numExecutors)
		this.numExecutors = numExecutors
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#slaveJar}
	 */
	String getSlaveJar() {
		return slaveJar
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#slaveJar}
	 */
	void setSlaveJar(String slaveJar) {
		validateFileString(slaveJar)
		this.slaveJar = slaveJar
	}

	/**
	 * Getter for {@link SwarmSlaveConfig#workspace}
	 */
	String getWorkspace() {
		return workspace
	}

	/**
	 * Setter for {@link SwarmSlaveConfig#workspace}
	 */
	void setWorkspace(String workspace) {
		validateFileString(workspace)
		this.workspace = workspace
	}

	/**
	 * Validator for file names. We require all paths to be absolute.
	 *
	 * @param pathString Path to be verified
	 */
	static void validateFileString(String pathString) {
		if (!pathString.startsWith("/")) {
			throw new IllegalArgumentException("Configuration paths have to be absolute.")
		}
	}

	/**
	 * Validator for host names. IP addresses are not supported.
	 *
	 * @see <a href="https://stackoverflow.com/a/3824105">[stackoverflow.com]</a>
	 * @param hostname Hostname to be verified
	 */
	static void validateHostname(String hostname) {
		String validHostname = "^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*\$"
		if (!hostname.matches(validHostname)) {
			throw new IllegalArgumentException("Invalid hostname specified.")
		}
	}

	/**
	 * Validator for user names.
	 *
	 * @see <a href="https://manpages.debian.org/stretch/passwd/useradd.8.de.html">[Debian: man useradd]</p>
	 * @param username Username to be verified
	 */
	static void validateUsername(String username) {
		String validUsername = "^[a-z_][a-z0-9_-]*[\$]?\$"
		if (!username.matches(validUsername)) {
			throw new IllegalArgumentException("Invalid username specified.")
		}
	}

	/**
	 * Validator for port numbers.
	 *
	 * @param port Integer to be verified.
	 */
	static void validatePortNumber(int port) {
		if ((port <= 0) | (port > 65535)) {
			throw new IllegalArgumentException("Invalid port specified.")
		}
	}

	/**
	 * Validator for executor counts.
	 *
	 * @param count Integer to be verified.
	 */
	static void validateExecutorCount(int count) {
		if (count < 0) {
			throw new IllegalArgumentException("Invalid number of executors specified.")
		}
	}
}
