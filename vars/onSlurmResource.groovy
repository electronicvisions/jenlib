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

	// Visionary Jenkins Setup
	SwarmSlaveConfig config = new SwarmSlaveConfig()
	config.javaHome = "/wang/environment/software/jessie/jdk/8u92"
	config.jenkinsHostname = "jenviz.skynet.kip.uni-heidelberg.de"
	config.jenkinsJnlpPort = 8079
	config.jenkinsKeyfile = "/wang/users/vis_jenkins/swarm_integration/passfile.key"
	config.jenkinsUsername = "vis_jenkins"
	config.jenkinsWebPort = 8080
	config.jenkinsWebProtocol = SwarmSlaveConfig.WebProtocol.HTTP
	config.mode = SwarmSlaveConfig.SlaveMode.EXCLUSIVE
	config.slaveJar = "/wang/users/vis_jenkins/swarm_integration/swarm-client-latest.jar"
	config.numExecutors = 1

	// Workspace is overwritten to be the same as on the frontend below
	config.fsroot = "/jenkins/jenviz_`hostname`/fsroot"

	// Working directory on the frontend
	String frontendPwd

	// Workspace on the frontend
	String frontendWorkspace

	SlurmSwarmSlave slave = new SlurmSwarmSlave(this, config, slurm_args)

	// Slurm controller has to be accessed from a frontend
	runOnSlave(label: "frontend") {
		frontendPwd = pwd()
		frontendWorkspace = WORKSPACE
		sh "stat ${frontendPwd} > /dev/null"        // Flush NFS attribute cache
		sh "stat ${frontendWorkspace} > /dev/null"  // Flush NFS attribute cache
		slave.startSlave()
	}

	try {
		// Run the content on the upcoming node as soon as it is available
		node("slurm_${slave.jobID.toString()}") {
			ws(frontendWorkspace) {
				dir(frontendPwd) {
					content()
				}
			}
		}
	} catch (Throwable anything) {
		throw anything
	} finally {
		// Slurm controller has to be accessed from a frontend
		runOnSlave(label: "frontend") {
			slave.stopSlave()
		}
	}
}
