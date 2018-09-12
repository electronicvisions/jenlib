package org.electronicvisions

class WafTest extends GroovyTestCase {
	/**
	 * Mock for the pipeline.
	 */
	class MockedPipelineScript {

		/**
		 * Last process' standard output. Used to get the result of a {@code sh} step.
		 */
		public String stdout_lastrun = new String()
		public String stdout_accumulated = new String()
		/**
		 * Provide some necessary environment variables.
		 */
		public Map env = [:]

		@SuppressWarnings("GroovyUnusedDeclaration")
		void sish(String command) {

			def out = new ByteArrayOutputStream()
			def err = new ByteArrayOutputStream()

			Process proc = ["bash", "-c", command].execute()

			proc.consumeProcessOutput(out, err)

			proc.waitFor()
			assertEquals(err as String, 0, proc.exitValue())
			stdout_lastrun = out as String
			stdout_accumulated += out as String
		}

		@SuppressWarnings("GroovyUnusedDeclaration")
		String pwd(Map options) {
			return System.getProperty("user.dir").toString()
		}
	}

	/**
	 * Test that waf build of symwaf2ic branch succeeds.
	 *
	 * @throws Exception
	 */
	void testMaster() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		Waf waf = new Waf(pipeline)
		waf.build()

		pipeline.sish "cd ${waf.waf_dir} && ${waf.path}/waf --help"

		assertTrue(pipeline.stdout_lastrun.contains("waf [commands] [options]"))

		waf.clean()
	}

	/**
	 * Test that waf build with gerrit changeset specified succeeds.
	 *
	 * @throws Exception
	 */
	void testGerritCS() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		pipeline.env.GERRIT_PORT = "29418"
		pipeline.env.GERRIT_HOST = "brainscales-r.kip.uni-heidelberg.de"
		pipeline.env.GERRIT_CHANGE_NUMBER = "3981"

		Waf waf = new Waf(pipeline, [debug: true])
		waf.build()

		assertTrue(pipeline.stdout_accumulated.contains("Change cross ar to gcc-ar to enable finding lto plugins"))

		pipeline.sish "cd ${waf.waf_dir} && ${waf.path}/waf --help"

		assertTrue(pipeline.stdout_lastrun.contains("waf [commands] [options]"))

		waf.clean()
	}

	/**
	 * Test that waf build with gerrit changeset without gerrit host and port specified succeeds.
	 *
	 * @throws Exception
	 */
	void testGerritCSwithoutPortHost() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		pipeline.env.GERRIT_CHANGE_NUMBER = "3981"

		Waf waf = new Waf(pipeline, [debug: true])
		waf.build()

		assertTrue(pipeline.stdout_accumulated.contains("Change cross ar to gcc-ar to enable finding lto plugins"))

		pipeline.sish "cd ${waf.waf_dir} && ${waf.path}/waf --help"

		assertTrue(pipeline.stdout_lastrun.contains("waf [commands] [options]"))

		waf.clean()
	}

	/**
	 * Test that the same waf instance cannot be built twice without clean.
	 *
	 * @throws Exception
	 */
	void testBuildTwiceWithoutClean() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		Waf waf = new Waf(pipeline)
		waf.build()

		shouldFail(IllegalStateException) {
			waf.build()
		}

		waf.clean()
	}

	/**
	 * Test that waf can be built again after clean.
	 *
	 * @throws Exception
	 */
	void testBuildAfterCleanAfterBuild() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		Waf waf = new Waf(pipeline)
		waf.build()
		waf.clean()
		waf.build()

		waf.clean()
	}

	/**
	 * Test that waf can't be cleaned, if not yet built.
	 *
	 * @throws Exception
	 */
	void testCleanBeforeBuild() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		Waf waf = new Waf(pipeline)
		shouldFail(IllegalStateException) {
			waf.clean()
		}
	}

	/**
	 * Test 'debug' switch:
	 *   If set, there must be no output.
	 *   If unset, we expect waf's make output.
	 *
	 * @throws Exception
	 */
	void testDebugOutput() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		Waf releaseWaf = new Waf(pipeline)
		releaseWaf.build()
		assertFalse(pipeline.stdout_accumulated.length() > 0)

		Waf debugWaf = new Waf(pipeline, [debug: true])
		debugWaf.build()
		assertTrue(pipeline.stdout_accumulated.contains("adding waflib"))
	}
}
