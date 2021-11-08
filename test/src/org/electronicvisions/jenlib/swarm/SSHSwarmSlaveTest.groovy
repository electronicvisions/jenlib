package org.electronicvisions.jenlib.swarm

class SSHSwarmSlaveTest extends SwarmSlaveTest {
	@Override
	List<String> getMandatorySlaveParameters() {
		return ["jenkinsHostname", "jenkinsWebProtocol", "jenkinsWebPort"]
	}

	@Override
	List<String> getProhibitedSlaveParameters() {
		return []
	}

	@Override
	SwarmSlave generateSwarmSlave(List<String> configuredParameters, SwarmSlaveConfig config) {
		return new SSHSwarmSlave(new SSHSwarmSlavePipelineMock(configuredParameters), config, "user", "hostname")
	}

	void testDoNotStartTwice() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		config.jenkinsHostname = DEFAULT_PARAMETERS["jenkinsHostname"]
		config.jenkinsWebProtocol = DEFAULT_PARAMETERS["jenkinsWebProtocol"]
		config.jenkinsWebPort = DEFAULT_PARAMETERS["jenkinsWebPort"]

		SwarmSlave slave = new SSHSwarmSlave(new SSHSwarmSlavePipelineMock(), config, "user", "hostname")
		slave.startSlave()
		shouldFail(IllegalStateException) {
			slave.startSlave()
		}

		slave.stopSlave()
		slave.startSlave()
	}
}
