package org.electronicvisions

import groovy.json.JsonSlurperClassic

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class ShellManipulatorTest extends GroovyTestCase {

	@SuppressWarnings("unused")
	class MockedPipelineScript {

		public Map env = [WORKSPACE: new File(".").getCanonicalPath()]

		void jesh(String command) {
			Process proc = ["bash", "-c", command].execute()
			proc.waitFor()
			assertEquals(proc.exitValue(), 0)
		}

		void writeFile(Map<String, String> input) {
			Path targetFile = Paths.get(input.get("file"))
			Files.write(targetFile, input.get("text").getBytes())
		}
	}

	void testToString() {
		ShellManipulator manipulator = new ShellManipulator(new MockedPipelineScript())

		manipulator.add("foo", "bar")
		manipulator.add("foo", "baz")

		List<List<String>> manipulations = (List) new JsonSlurperClassic().parseText(manipulator.toString())
		assertEquals(manipulations[0], ["foo", "baz"]) // latest-added manipulation comes first
		assertEquals(manipulations[1], ["foo", "bar"]) // first-added manipulation comes second
	}

	void testFromString() {
		ShellManipulator manipulator = new ShellManipulator(new MockedPipelineScript())
		manipulator.fromString('[["foo", "bar"]]')
		assertEquals(manipulator.manipulations[0][0], "foo")
		assertEquals(manipulator.manipulations[0][1], "bar")
	}

	void testAdd() {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		ShellManipulator manipulator = new ShellManipulator(pipeline)
		manipulator.add("foo", "bar")
		assertEquals(manipulator.manipulations[0][0], "foo")
		assertEquals(manipulator.manipulations[0][1], "bar")

		// Env should have changed
		assertTrue(new ShellManipulator(pipeline).toString().contains("foo"))
		assertTrue(new ShellManipulator(pipeline).toString().contains("bar"))

		manipulator.restore()
		assertEquals(new ShellManipulator(pipeline).toString(), "[]")
	}

	void testConstructAndCleanup() {
		ShellManipulator manipulator = new ShellManipulator(new MockedPipelineScript())
		manipulator.add("foo", "bar")

		String masterFileName = manipulator.constructScriptStack("baz")

		// Read back the master file
		String masterFileContent = new String(Files.readAllBytes(Paths.get(masterFileName)))

		// Extract the command file name (and thereby check the format)
		Pattern commandFileNamePattern = Pattern.compile("foo bash (.*) bar")
		Matcher m = commandFileNamePattern.matcher(masterFileContent)
		assertTrue("Master shell script has the wrong format.", m.matches())
		String commandFileName = m.group(1)

		// Read back the command file and check its contents
		String commandFileContent = new String(Files.readAllBytes(Paths.get(commandFileName)))
		assertEquals(commandFileContent, "baz")

		// Cleanup
		manipulator.restore()
		assertFalse(new File(masterFileName).exists())
		assertFalse(new File(commandFileName).exists())
	}

	void testEmptyConstruction() {
		ShellManipulator manipulator = new ShellManipulator(new MockedPipelineScript())
		String masterFileName = manipulator.constructScriptStack("foobar")
		String masterFileContent = new String(Files.readAllBytes(Paths.get(masterFileName)))
		assertEquals(masterFileContent, "foobar")
		manipulator.restore()
	}

	void testNoMultipleConstruction() {
		ShellManipulator manipulator = new ShellManipulator(new MockedPipelineScript())
		manipulator.add("foo", "")
		manipulator.constructScriptStack("bar")

		shouldFail(IllegalStateException) {
			manipulator.constructScriptStack("baz")
		}

		manipulator.restore()
	}

	void testNoRelativeTemporaryPaths() {
		MockedPipelineScript pipeline = new MockedPipelineScript()
		pipeline.env.WORKSPACE = "relative/"

		ShellManipulator manipulator = new ShellManipulator(pipeline)

		shouldFail(InternalError) {
			manipulator.constructScriptStack("foobar")
		}

		manipulator.restore()
	}
}
