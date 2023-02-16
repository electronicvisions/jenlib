package org.electronicvisions.jenlib.swarm

/**
 * {@link SwarmSlave} implementation for starting slaves on Slurm resources.
 *
 * Jenkins slaves are started on {@code sbatch} jobs on {@code exclusive} nodes.
 */
class SlurmSwarmSlave extends SwarmSlave {

	/**
	 * Slurm job id of the slave.
	 */
	private int jobID

	/**
	 * User-supplied arguments for specifying the Slurm resource to be allocated.
	 */
	private Map<String, Object> slurm_args

	/**
	 * Constructor for {@link SlurmSwarmSlave}.
	 *
	 * @param steps Pipeline steps, to be passed as {@code this} from the calling pipeline
	 * @param config Swarm slave configuration to be used for this slave
	 * @param slurm_args Slurm arguments: Key-value pairs, where the keys represent the slurm argument, the value the value to be set.
	 *                                    Keys have to be full-length ('partition', not 'p'). Double-dashes are added internally.
	 *                                    The 'partition' key is mandatory.
	 */
	SlurmSwarmSlave(steps, SwarmSlaveConfig config, Map<String, Object> slurm_args) {
		super(steps, config)

		this.slurm_args = slurm_args
	}

	/**
	 * Getter for {@link SlurmSwarmSlave#jobID}
	 */
	int getJobID() {
		return jobID
	}

	/**
	 * Verify the configuration and start the slave.
	 * The queued slurm job id will be saved to {@link SlurmSwarmSlave#jobID}.
	 */
	void startSlave() {
		if (this.config.slaveName != null) {
			throw new IllegalArgumentException("Cannot control slave name within Slurm environments.")
		}
		this.config.setSlaveName('slurm_$SLURM_JOB_ID')

		checkSlurmArguments(slurm_args)

		String stdout = steps.sh(script: "echo '${buildSlaveStartScript()}' | sbatch --parsable ${buildSlurmArguments()}",
		                         returnStdout: true)

		jobID = extractJobID(stdout)
		steps.echo "Submitted job for jenkins slave at slurm ID $jobID."
	}

	/**
	 * Stop the swarm slave.
	 */
	void stopSlave() {
		if (jobID == 0) {
			throw new IllegalStateException("Cannot stop slave, job ID unknown. Did you start it?")
		}

		steps.sh("scancel $jobID")
	}

	/**
	 * Extract the slurm job from the result of a {@code sbatch --parsable} call.
	 *
	 * @param sbatchStdout {@code sbatch} output
	 * @return Slurm job id
	 */
	private static int extractJobID(String sbatchStdout) {
		return Integer.parseInt(sbatchStdout.trim())
	}

	/**
	 * Construct the arguments for slurm {@code srun} or {@code sbatch} calls from the user input.
	 *
	 * @return Arguments for the slurm call
	 */
	private String buildSlurmArguments() {
		List<String> args = new ArrayList<>()

		// Make sure exactly one node is allocated
		args.add("--nodes=1-1")

		// Add a default job name, user supplied options will override
		args.add("--job-name=\"${this.steps.env.JOB_NAME}#${this.steps.env.BUILD_NUMBER}\"")

		for (String key in slurm_args.keySet()) {
			args.add("--$key")
			args.add(slurm_args.get(key).toString())
		}

		return args.join(" ").trim()
	}

	/**
	 * Construct the slave startup script for {@code sbatch}.
	 *
	 * @return Jenkins slave startup script in a single string
	 */
	private String buildSlaveStartScript() {
		// Signals are only passed to the SBATCH shell by default.
		// Since we run a single command, we can use 'exec' to replace the SBATCH shell by the slave-starting shell.
		return "#!/bin/sh\n" +
		       "exec ${buildSlaveStartCommand()}"
	}

	/**
	 *  Verify that the specified slurm arguments are sane.
	 *
	 * @param arguments Slurm arguments to be verified
	 */
	private static void checkSlurmArguments(Map<String, Object> arguments) {
		for (String key in arguments.keySet()) {
			if (key.length() < 2) {
				throw new IllegalArgumentException('Only fully-named argument identifiers (--$key) are supported.')
			}
		}

		if (!arguments.containsKey("partition")) {
			throw new MissingPropertyException("Slurm partition has to be specified.")
		}

		if (arguments.containsKey("nodes")) {
			throw new IllegalArgumentException("Slurm nodes are fixed to 1.")
		}
	}
}
