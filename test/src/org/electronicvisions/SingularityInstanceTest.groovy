package org.electronicvisions


class SingularityInstanceTest extends GroovyTestCase {

	/**
	 * Mock for the pipeline.
	 * In this case, we only need Jenkins' {@code sh} step to execute in a local shell.
	 */
	class MockedPipelineScript {

		/**
		 * Last process' standard output. Used to get the result of a {@code sh} step.
		 */
		public String stdout_lastrun

		@SuppressWarnings("GroovyUnusedDeclaration")
		void sh(String command) {

			def out = new ByteArrayOutputStream()
			def err = new ByteArrayOutputStream()

			Process proc = ["bash", "-c", command].execute()

			proc.consumeProcessOutput(out, err)

			proc.waitFor()
			assertEquals(err as String, 0, proc.exitValue())
			stdout_lastrun = out as String
		}
	}

	/**
	 * Test that containers start, can execute commands within the container and stops.
	 *
	 * @throws Exception
	 */
	void testBasicFunctionality() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		SingularityInstance container = new SingularityInstance(pipeline,
		                                                        "/containers/jenkins/softies_darling",
		                                                        "visionary-defaults")

		container.list_instances()
		assertFalse(pipeline.stdout_lastrun.contains(container.instance_id))

		container.start()
		container.list_instances()
		assertTrue(pipeline.stdout_lastrun.contains(container.instance_id))

		container.exec("env")
		assertTrue(pipeline.stdout_lastrun.contains("SINGULARITY_CONTAINER"))

		container.stop()
		container.list_instances()
		assertFalse(pipeline.stdout_lastrun.contains(container.instance_id))
	}

	/**
	 * Test that the container can be used as a managed resource
	 *
	 * @throws Exception
	 */
	void testManagedResource() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()
		String instance_id = ""

		new SingularityInstance(pipeline,
		                        "/containers/jenkins/softies_darling",
		                        "visionary-defaults").withCloseable { SingularityInstance container ->
			container.exec("env")
			assertTrue(pipeline.stdout_lastrun.contains("SINGULARITY_CONTAINER"))

			// Save instance id to ensure it gets closed
			instance_id = container.instance_id
		}

		// Make sure the container gets closed
		new SingularityInstance(pipeline, "", "").list_instances()
		assertFalse(pipeline.stdout_lastrun.contains(instance_id))
	}

	/**
	 * Test that the container automatically starts with the first executed command
	 *
	 * @throws Exception
	 */
	void testAutostart() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		SingularityInstance container = new SingularityInstance(pipeline,
		                                                        "/containers/jenkins/softies_darling",
		                                                        "visionary-defaults")

		container.list_instances()
		assertFalse(pipeline.stdout_lastrun.contains(container.instance_id))

		container.exec("env")
		assertTrue(pipeline.stdout_lastrun.contains("SINGULARITY_CONTAINER"))

		container.stop()
		container.list_instances()
		assertFalse(pipeline.stdout_lastrun.contains(container.instance_id))
	}

	/**
	 * Test that the same container cannot be started twice
	 *
	 * @throws Exception
	 */
	void testRunningStart() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		SingularityInstance container = new SingularityInstance(pipeline,
		                                                        "/containers/jenkins/softies_darling",
		                                                        "visionary-defaults")

		container.start()

		// Instance already started
		shouldFail(IllegalStateException) {
			container.start()
		}

		// Ensure cleanup
		container.stop()
		container.list_instances()
		assertFalse(pipeline.stdout_lastrun.contains(container.instance_id))
	}

	/**
	 * Test that a stopped container cannot be stopped again.
	 *
	 * @throws Exception
	 */
	void testStoppedStop() throws Exception {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		SingularityInstance container = new SingularityInstance(pipeline,
		                                                        "/containers/jenkins/softies_darling",
		                                                        "visionary-defaults")

		// Instance was never started, cannot stop
		shouldFail(IllegalStateException) {
			container.stop()
		}

		container.start()
		container.stop()

		// Instance was already stopped
		shouldFail(IllegalStateException) {
			container.stop()
		}

		// Ensure cleanup
		container.list_instances()
		assertFalse(pipeline.stdout_lastrun.contains(container.instance_id))
	}
}
