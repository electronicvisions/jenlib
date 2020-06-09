package org.electronicvisions.jenlib.swarm

class SSHSwarmSlaveTest extends SwarmSlaveTest {
	@Override
	List<String> getMandatorySlaveParameters() {
		return ["slaveJar"]
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
		config.slaveJar = "/some/path.jar"

		SwarmSlave slave = new SSHSwarmSlave(new SSHSwarmSlavePipelineMock(), config, "user", "hostname")
		slave.startSlave()
		shouldFail(IllegalStateException) {
			slave.startSlave()
		}

		slave.stopSlave()
		slave.startSlave()
	}
}
