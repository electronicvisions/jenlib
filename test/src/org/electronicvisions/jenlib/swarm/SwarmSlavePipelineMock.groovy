package org.electronicvisions.jenlib.swarm

import static junit.framework.TestCase.assertFalse
import static junit.framework.TestCase.assertTrue


abstract class SwarmSlavePipelineMock {

	List<String> setParameters

	public Map env = [
			JOB_NAME: "my_job",
			BUILD_NUMBER: 42,
	]

	SwarmSlavePipelineMock(List<String> setParameters) {
		this.setParameters = setParameters
	}

	void sh(String command) {
		if (command.contains("java -jar")) {  // This is a slave startup command!

			/**
			 * Ensure all parameters were set somehow.
			 */
			for (String key in setParameters) {
				assertTrue(command.contains(SwarmSlaveTest.DEFAULT_PARAMETERS.get(key).toString()))
			}
		}
	}
}
