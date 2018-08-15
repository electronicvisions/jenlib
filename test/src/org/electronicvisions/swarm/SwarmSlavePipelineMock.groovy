package org.electronicvisions.swarm

import static junit.framework.TestCase.assertFalse
import static junit.framework.TestCase.assertTrue


abstract class SwarmSlavePipelineMock {

	List<String> setParameters

	SwarmSlavePipelineMock(List<String> setParameters) {
		this.setParameters = setParameters
	}

	void sh(String command) {
		if (command.contains("java -jar")) {  // This is a slave startup command!

			/**
			 * Ensure all parameters were set somehow.
			 * There is one special case: the master configuration is only applied when all three related
			 * parameters are given.
			 */
			if (setParameters.contains("jenkinsWebProtocol") &&
			    setParameters.contains("jenkinsHostname") &&
			    setParameters.contains("jenkinsWebPort")) {
				// Nothing special here: Everything should be in the command
				for (String key in setParameters) {
					assertTrue(command.contains(SwarmSlaveTest.DEFAULT_PARAMETERS.get(key).toString()))
				}
			} else {
				// Master configuration must not be in command
				for (String key in setParameters) {
					if (!["jenkinsWebProtocol", "jenkinsHostname", "jenkinsWebPort"].contains(key)) {
						assertTrue(command.contains(SwarmSlaveTest.DEFAULT_PARAMETERS.get(key).toString()))
					} else {
						assertFalse(command.contains("-master"))
					}
				}
			}
		}
	}
}
