package org.electronicvisions.jenlib.swarm

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
			}
			return ""
		}
	}

	static void echo(String input) {}
}
