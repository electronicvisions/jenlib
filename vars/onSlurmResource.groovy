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
 *                   Double-dashes are added internally.
 * @param content Content to be executed
 */
def call(LinkedHashMap<String, String> slurm_args, Closure content) {
	Map<String, String> slurmArgsInternal = slurm_args.clone()

	SwarmSlaveConfig commonConfig = new SwarmSlaveConfig()
	commonConfig.javaHome = "/wang/environment/software/jessie/jdk/21.0.6+8"
	commonConfig.loggingConfig = "/jenkins/home/vis_jenkins/swarm_integration/logging.properties"
	commonConfig.jenkinsWebPort = 8080
	commonConfig.jenkinsWebProtocol = SwarmSlaveConfig.WebProtocol.HTTP
	commonConfig.mode = SwarmSlaveConfig.SlaveMode.EXCLUSIVE
	commonConfig.numExecutors = 1

	SwarmSlaveConfig configVisions = commonConfig.clone()
	configVisions.jenkinsHostname = "jenviz.skynet.kip.uni-heidelberg.de"
	configVisions.jenkinsUsername = "vis_jenkins"
	configVisions.jenkinsKeyfile = "/jenkins/home/vis_jenkins/swarm_integration/passfile.key"

	SwarmSlaveConfig configAsic = commonConfig.clone()
	configAsic.jenkinsHostname = "jenkins.kip.uni-heidelberg.de"
	configAsic.jenkinsUsername = "jenkins"
	configAsic.jenkinsKeyfile = "/einc/prod/users/jenkins/swarm_integration/passfile.key"

	SlurmSwarmSlave slave = new SlurmSwarmSlave(this, isAsicJenkins() ? configAsic : configVisions, slurmArgsInternal)

	// Slurm controller has to be accessed from a frontend
	runOnSlave(label: "slurm_frontend") {
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
		runOnSlave(label: "slurm_frontend") {
			slave.stopSlave()
		}

		// Archive slurm slave logs
		runOnSlave(label: "lightweight") {
			archiveArtifacts(allowEmptyArchive: true, artifacts: "slurm-${slave.jobID}.*")
		}
	}
}
