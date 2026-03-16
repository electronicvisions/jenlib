package org.electronicvisions.jenlib.swarm

class SlurmSwarmSlaveTest extends SwarmSlaveTest {

	@Override
	List<String> getMandatorySlaveParameters() {
		return ["jenkinsHostname", "jenkinsWebProtocol", "jenkinsWebPort"]
	}

	@Override
	List<String> getProhibitedSlaveParameters() {
		return ["slaveName"]
	}

	@Override
	SwarmSlave generateSwarmSlave(List<String> configuredParameters, SwarmSlaveConfig config) {
		return new SlurmSwarmSlave(new SlurmSwarmSlavePipelineMock(configuredParameters), config, [foo: "bar"])
	}

	void testSlurmArgumentHandling() {
		// Short names are prohibited
		shouldFail(IllegalArgumentException) {
			SwarmSlaveConfig config = new SwarmSlaveConfig()
			config.jenkinsHostname = DEFAULT_PARAMETERS["jenkinsHostname"]
			config.jenkinsWebProtocol = DEFAULT_PARAMETERS["jenkinsWebProtocol"]
			config.jenkinsWebPort = DEFAULT_PARAMETERS["jenkinsWebPort"]
			SwarmSlave slave = new SlurmSwarmSlave(new SlurmSwarmSlavePipelineMock(), config, [p: "batch"])
			slave.startSlave()
		}
	}

	void testSlurmJobIdParsing() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		config.jenkinsHostname = DEFAULT_PARAMETERS["jenkinsHostname"]
		config.jenkinsWebProtocol = DEFAULT_PARAMETERS["jenkinsWebProtocol"]
		config.jenkinsWebPort = DEFAULT_PARAMETERS["jenkinsWebPort"]

		SwarmSlave slave = new SlurmSwarmSlave(new SlurmSwarmSlavePipelineMock(), config, [:])
		slave.startSlave()
		assertEquals(slave.jobID, SlurmSwarmSlavePipelineMock.MOCKED_SLURM_ID)
	}

	void testSingleNodeOnly() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		config.jenkinsHostname = DEFAULT_PARAMETERS["jenkinsHostname"]
		config.jenkinsWebProtocol = DEFAULT_PARAMETERS["jenkinsWebProtocol"]
		config.jenkinsWebPort = DEFAULT_PARAMETERS["jenkinsWebPort"]

		shouldFail(IllegalArgumentException) {
			SwarmSlave slave = new SlurmSwarmSlave(new SlurmSwarmSlavePipelineMock(), config,
			                                       [nodes: 2])
			slave.startSlave()
		}
	}
}
