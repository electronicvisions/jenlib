package org.electronicvisions.jenlib.swarm

class SlurmSwarmSlaveTest extends SwarmSlaveTest {

	static List<String> getMandatorySlaveParameters() {
		return ["slaveJar"]
	}

	static List<String> getProhibitedSlaveParameters() {
		return ["slaveName"]
	}

	SwarmSlave generateSwarmSlave(List<String> configuredParameters, SwarmSlaveConfig config) {
		return new SlurmSwarmSlave(new SlurmSwarmSlavePipelineMock(configuredParameters), config, [partition: "compile"])
	}

	void testSlurmArgumentHandling() {
		// Short names are prohibited
		shouldFail(IllegalArgumentException) {
			SwarmSlaveConfig config = new SwarmSlaveConfig()
			config.slaveJar = "/some/path.jar"
			SwarmSlave slave = new SlurmSwarmSlave(new SlurmSwarmSlavePipelineMock(), config, [p: "compile"])
			slave.startSlave()
		}

		// Partition is mandatory
		shouldFail(MissingPropertyException) {
			SwarmSlaveConfig config = new SwarmSlaveConfig()
			config.slaveJar = "/some/path.jar"
			SwarmSlave slave = new SlurmSwarmSlave(new SlurmSwarmSlavePipelineMock(), config, [gres: "B201330"])
			slave.startSlave()
		}
	}

	void testSlurmJobIdParsing() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		config.slaveJar = "/some/path.jar"

		SwarmSlave slave = new SlurmSwarmSlave(new SlurmSwarmSlavePipelineMock(), config, [partition: "jenkins"])
		slave.startSlave()
		assertEquals(slave.jobID, SlurmSwarmSlavePipelineMock.MOCKED_SLURM_ID)
	}

	void testSingleNodeOnly() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		config.slaveJar = "/some/path.jar"

		shouldFail(IllegalArgumentException) {
			SwarmSlave slave = new SlurmSwarmSlave(new SlurmSwarmSlavePipelineMock(), config,
			                                       [partition: "jenkins", nodes: 2])
			slave.startSlave()
		}
	}
}
