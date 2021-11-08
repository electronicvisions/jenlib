package org.electronicvisions.jenlib.swarm

class SSHSwarmSlavePipelineMock extends SwarmSlavePipelineMock {
	SSHSwarmSlavePipelineMock(List<String> set_parameters = ["jenkinsHostname", "jenkinsWebProtocol", "jenkinsWebPort"]) {
		super(set_parameters)
	}
}
