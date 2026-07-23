import static java.util.UUID.randomUUID

/**
 * Starts a Quiggeldy-Server and sets enviroment variables to enable execution
 * on the server.
 *
 * @param content Content to be executed on the Quiggeldy-Server.
 */
def call(Map<String, Object> options = [:], Closure content) {
	final String quiggeldyDir = "${steps.pwd(tmp: true)}/quiggeldy_${randomUUID().toString()}"
	Integer quiggeldyPid = null
	Integer quiggeldyPort = null
	String quiggeldyHostname = null
	try {
		dir(quiggeldyDir) {
			stage("Start quiggeldy server"){
				(quiggeldyPid, quiggeldyPort, quiggeldyHostname) = startQuiggeldy()
			}
		}

		final String username = jesh(
				script: "whoami",
				returnStdout: true
			).trim()
		withEnv(
			["ENABLE_QUIGGELDY=1",
			"QUIGGELDY_PORT=${quiggeldyPort}",
			"QUIGGELDY_IP=${quiggeldyHostname}",
			"QUIGGELDY_USER_NO_MUNGE=${username}"
			]) {
			content()
		}
	}
	// Stop Quiggeldy-Server and archive server logs.
	finally {
		if (quiggeldyPid != null){
			jesh("kill -9 ${quiggeldyPid} || true")

			dir(quiggeldyDir) {
				archiveArtifacts("quiggeldy_server.log")
			}
		}
	}
}

/**
 * Start a Quiggeldy-Server instance.
 *
 * @param enableZeroMockMode Enable the ZeroMockMode for the server.
 *		  The server does not run anything on hardware and returns empty results.
 */
private Map<String, Integer> startQuiggeldy() {
	final Map<String, Object> containerOptions = [app: "dls-core"]

	// Build quiggeldy binary.
	wafDefaultPipeline(
		projects: ["hxcomm"],
		container: containerOptions,
		wafTargetOptions: ["--target=quiggeldy"],
		notificationChannel: "#jenkins-trashbin",
		testRunners: [:],
		enableClangFormat: false,
		enableCppcheck: false,
		enableClangTidy: false,
		enableDoxygenCheck: false,
	)

	final int port = getFreePort()
	Integer pid = null
	String hostname = null
	inSingularity(containerOptions) {
		withModules(modules: ["localdir"]) {
			pid = jesh(
				script: "quiggeldy --listen-port ${port} " +
				"--no-munge --no-allocate-license " +
				"&> quiggeldy_server.log & echo \$!",
				returnStdout: true).trim().toInteger()
			hostname = jesh(
				script: "hostname -s",
				returnStdout: true).trim()
		}
	}

	// Wait until the server is up by checking if the assigned port is used.
	timeout(time: 1, unit: "MINUTES") {
		while (jesh(script: "nc -z localhost ${port}", returnStatus: true)) {
			sleep(10)
		}
	}

	return [pid, port, hostname]
}

/**
 * Get a free port on the local host.
 */
private int getFreePort() {
	return jesh(script: "python -c 'import socket; " +
						"s=socket.socket(); " +
						"s.bind((\"\", 0)); " +
						"print(s.getsockname()[1]); " +
						"s.close()'",
				returnStdout: true).trim().toInteger()
}
