package org.electronicvisions.swarm

import static junit.framework.TestCase.assertTrue

class SlurmSwarmSlavePipelineMock extends SwarmSlavePipelineMock {

	static final int MOCKED_SLURM_ID = 12345

	SlurmSwarmSlavePipelineMock(List<String> set_parameters = ["slaveJar"]) {
		super(set_parameters)
	}

	void sh(String command) {
		super.sh(command)

		if (command.contains("scancel")) {
			assertTrue(command.contains(MOCKED_SLURM_ID.toString()))
		}
	}

	def sh(Map<String, String> options) {
		String command = new String()

		if (options.containsKey("script")) {
			command = options.get("script")
			sh(command)
		} else {
			throw new IllegalArgumentException("Script parameter is mandatory.")
		}

		if (options.get("returnStdout")) {
			if (command.matches("[\\s\\S]*sbatch.*--parsable[\\s\\S]*")) {
				return MOCKED_SLURM_ID
			} else if (command.matches("[\\s\\S]*srun.*--test-only[\\s\\S]*")) {
				if (command.contains("--nodes")) {
					int num_nodes = Integer.parseInt(command.find("(?<=--nodes\\s)\\d+"))
					if (num_nodes > 1) {
						return "srun: Job 1654343 to start at 2018-08-15T11:33:50 using 4 processors on HBPHost[9-12]"
					}
				}
				return "srun: Job 1654346 to start at 2018-08-15T11:34:34 using 1 processors on AMTHost12"
			}
			return ""
		}
	}

	static void echo(String input) {}
}
