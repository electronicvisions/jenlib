import org.electronicvisions.jenlib.swarm.SlurmSwarmSlave
import org.electronicvisions.jenlib.swarm.SwarmSlaveConfig

/**
 * Run a block of commands on a machine allocated via slurm.
 * This function spawns a jenkins slave on the allocated node and runs all given commands on that slave.
 *
 * Only slurm allocations for single hosts are supported.
 *
 * For most efficient resource usage, make sure that this command is not run within an execution slot.
 *
 * @param slurm_args Map of arguments passed to {@code sbatch}.
 *                   Keys are full-length (double-dash) argument keys (e.g. 'partition'), values the respective values.
 *                   Double-dashes are added internally. The 'partition' argument is mandatory.
 * @param content Content to be executed
 */
def call(LinkedHashMap<String, String> slurm_args, Closure content) {
	Map<String, String> slurmArgsInternal = slurm_args.clone()

	// Visionary Jenkins Setup
	SwarmSlaveConfig config = new SwarmSlaveConfig()
	config.javaHome = "/wang/environment/software/jessie/jdk/21.0.6+8"
	config.loggingConfig = "/jenkins/home/vis_jenkins/swarm_integration/logging.properties"
	config.jenkinsHostname = "jenviz.skynet.kip.uni-heidelberg.de"
	config.jenkinsJnlpPort = 8079
	config.jenkinsKeyfile = "/jenkins/home/vis_jenkins/swarm_integration/passfile.key"
	config.jenkinsUsername = "vis_jenkins"
	config.jenkinsWebPort = 8080
	config.jenkinsWebProtocol = SwarmSlaveConfig.WebProtocol.HTTP
	config.mode = SwarmSlaveConfig.SlaveMode.EXCLUSIVE
	config.numExecutors = 1

	SlurmSwarmSlave slave = new SlurmSwarmSlave(this, config, slurmArgsInternal)

	// Slurm controller has to be accessed from a frontend
	runOnSlave(label: "frontend") {
		slave.startSlave()
		jesh("stat ${WORKSPACE} > /dev/null")  // Flush NFS attribute cache
	}

	try {
		// Run the content on the upcoming node as soon as it is available
		runOnSlave(name: "slurm_${slave.jobID.toString()}") {
			content()
		}
	} finally {
		// Slurm controller has to be accessed from a frontend
		runOnSlave(label: "frontend") {
			slave.stopSlave()
		}

		// Archive slurm slave logs
		runOnSlave(label: "frontend") {
			archiveArtifacts(allowEmptyArchive: true, artifacts: "slurm-${slave.jobID}.*")
		}
	}
}
