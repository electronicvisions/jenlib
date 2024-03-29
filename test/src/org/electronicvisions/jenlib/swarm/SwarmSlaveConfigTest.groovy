package org.electronicvisions.jenlib.swarm

class SwarmSlaveConfigTest extends GroovyTestCase {
	Random random_generator

	SwarmSlaveConfigTest() {
		this.random_generator = new Random(1234)
	}

	void testGetJavaBinary() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertNull(config.javaBinary)

		config.javaHome = "/some/path"
		assertEquals(config.javaBinary, "/some/path/bin/java")

		config.javaHome = "/some/path/"
		assertEquals(config.javaBinary, "/some/path/bin/java")
	}

	void testGetSetJavaHome() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertNull(config.javaHome)

		config.javaHome = "/some/path"
		assertEquals(config.javaHome, "/some/path")

		shouldFail {
			config.javaHome = "relative/path"
		}
	}

	void testGetSetLoggingConfig() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertNull(config.loggingConfig)

		config.loggingConfig = "/some/path/logging.properties"
		assertEquals(config.loggingConfig, "/some/path/logging.properties")

		shouldFail {
			config.loggingConfig = "relative/path/logging.properties"
		}
	}

	void testGetSetJenkinsHostname() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertNull(config.jenkinsHostname)

		config.jenkinsHostname = "valid.host.name"
		assertEquals(config.jenkinsHostname, "valid.host.name")

		shouldFail {
			config.jenkinsHostname = "invalid hostname"
		}
	}

	void testGetSetJenkinsJnlpPort() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertEquals(config.jenkinsJnlpPort, -1)

		int testPort

		// General case
		testPort = Math.min(Math.abs(random_generator.nextInt()), (int) Math.pow(2, 16) - 1)
		config.jenkinsJnlpPort = testPort
		assertEquals(config.jenkinsJnlpPort, testPort)

		// Valid edge cases
		testPort = (int) Math.pow(2, 16) - 1
		config.jenkinsJnlpPort = testPort
		assertEquals(config.jenkinsJnlpPort, testPort)

		testPort = 1
		config.jenkinsJnlpPort = testPort
		assertEquals(config.jenkinsJnlpPort, testPort)

		// Invalid edge cases
		shouldFail {
			config.jenkinsJnlpPort = (int) Math.pow(2, 16)
		}
		shouldFail {
			config.jenkinsJnlpPort = 0
		}

		// Negative port numbers
		shouldFail {
			config.jenkinsJnlpPort = -Math.abs(random_generator.nextInt())
		}
	}

	void testGetSetJenkinsKeyfile() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertNull(config.jenkinsKeyfile)

		config.jenkinsKeyfile = "/some/path"
		assertEquals(config.jenkinsKeyfile, "/some/path")

		shouldFail {
			config.jenkinsKeyfile = "relative/path"
		}
	}

	void testGetSetJenkinsUsername() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertNull(config.jenkinsUsername)

		config.jenkinsUsername = "valid_username"
		assertEquals(config.jenkinsUsername, "valid_username")

		shouldFail {
			config.jenkinsUsername = "invalid username"
		}
	}

	void testGetSetJenkinsWebPort() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertEquals(config.jenkinsWebPort, -1)

		int testPort

		// General case
		testPort = Math.min(Math.abs(random_generator.nextInt()), (int) Math.pow(2, 16) - 1)
		config.jenkinsWebPort = testPort
		assertEquals(config.jenkinsWebPort, testPort)

		// Valid edge cases
		testPort = (int) Math.pow(2, 16) - 1
		config.jenkinsWebPort = testPort
		assertEquals(config.jenkinsWebPort, testPort)

		testPort = 1
		config.jenkinsWebPort = testPort
		assertEquals(config.jenkinsWebPort, testPort)

		// Invalid edge cases
		shouldFail {
			config.jenkinsWebPort = (int) Math.pow(2, 16)
		}
		shouldFail {
			config.jenkinsWebPort = 0
		}

		// Negative port numbers
		shouldFail {
			config.jenkinsWebPort = -Math.abs(random_generator.nextInt())
		}
	}

	void testGetSetJenkinsWebProtocol() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertNull(config.jenkinsWebProtocol)

		config.jenkinsWebProtocol = SwarmSlaveConfig.WebProtocol.HTTP
		assertToString(config.jenkinsWebProtocol, "http://")

		config.jenkinsWebProtocol = SwarmSlaveConfig.WebProtocol.HTTPS
		assertToString(config.jenkinsWebProtocol, "https://")
	}

	void testGetSetMode() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertNull(config.mode)

		config.mode = SwarmSlaveConfig.SlaveMode.NORMAL
		assertToString(config.mode, "normal")

		config.mode = SwarmSlaveConfig.SlaveMode.EXCLUSIVE
		assertToString(config.mode, "exclusive")
	}

	void testGetSetSlaveName() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertNull(config.slaveName)

		config.slaveName = "some_slave_name"
		assertEquals(config.slaveName, "some_slave_name")
	}

	void testGetSetNumExecutors() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertEquals(config.numExecutors, -1)

		int testNumber

		testNumber = Math.abs(random_generator.nextInt())
		config.numExecutors = testNumber
		assertEquals(config.numExecutors, testNumber)

		shouldFail {
			config.numExecutors = -Math.abs(random_generator.nextInt())
		}
	}

	void testGetSetWorkspace() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()
		assertNull(config.fsroot)

		config.fsroot = "/some/path/"
		assertEquals(config.fsroot, "/some/path/")

		shouldFail {
			config.fsroot = "relative/path/"
		}
	}

	void testGetWebAddress() {
		SwarmSlaveConfig config = new SwarmSlaveConfig()

		shouldFail {
			String address = config.jenkinsWebAddress
		}

		config.jenkinsHostname = "localhost"
		config.jenkinsWebPort = 1234
		config.jenkinsWebProtocol = SwarmSlaveConfig.WebProtocol.HTTPS

		assertEquals(config.jenkinsWebAddress, "https://localhost:1234")
	}
}
