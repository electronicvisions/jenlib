package org.electronicvisions.swarm

class SSHSwarmSlaveTest extends SwarmSlaveTest {
	static List<String> getMandatorySlaveParameters() {
		return ["slaveJar"]
	}

	static List<String> getProhibitedSlaveParameters() {
		return []
	}

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
