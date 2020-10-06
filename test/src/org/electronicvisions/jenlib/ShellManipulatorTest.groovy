package org.electronicvisions.jenlib

import groovy.json.JsonSlurperClassic

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class ShellManipulatorTest extends GroovyTestCase {

	class EnvMock extends LinkedHashMap<String, String> {
		String put(String key, String value) {
			// Jenkins environment can only contain String-types (e.g. null -> 'null')
			return super.put(key, value.toString())
		}
	}

	@SuppressWarnings("unused")
	class MockedPipelineScript {

		public EnvMock env = [WORKSPACE: new File(".").getCanonicalPath()]
		public List<String> parallelBranchIds = []

		void jesh(String command) {
			Process proc = ["bash", "-c", command].execute()
			proc.waitFor()
			assertEquals(proc.exitValue(), 0)
		}

		void writeFile(Map<String, String> input) {
			Path targetFile = Paths.get(input.get("file"))
			Files.write(targetFile, input.get("text").getBytes())
		}

		List<String> getCurrentParallelBranchIds() {
			return parallelBranchIds
		}
	}

	void testToString() {
		ShellManipulator manipulator = ShellManipulator.fromEnvironment(new MockedPipelineScript())

		manipulator.add("foo", "bar")
		manipulator.add("foo", "baz")

		List<List<String>> manipulations = (List) new JsonSlurperClassic().parseText(manipulator.toString())
		assertEquals(manipulations[0], ["foo", "baz"]) // latest-added manipulation comes first
		assertEquals(manipulations[1], ["foo", "bar"]) // first-added manipulation comes second
	}

	void testFromString() {
		ShellManipulator manipulator = ShellManipulator.fromEnvironment(new MockedPipelineScript())
		manipulator.fromString('[["foo", "bar"]]')
		assertEquals(manipulator.manipulations[0][0], "foo")
		assertEquals(manipulator.manipulations[0][1], "bar")
	}

	void testAdd() {
		MockedPipelineScript pipeline = new MockedPipelineScript()

		ShellManipulator manipulator = ShellManipulator.fromEnvironment(pipeline)
		manipulator.add("foo", "bar")
		assertEquals(manipulator.manipulations[0][0], "foo")
		assertEquals(manipulator.manipulations[0][1], "bar")

		// Env should have changed
		assertTrue(ShellManipulator.fromEnvironment(pipeline).toString().contains("foo"))
		assertTrue(ShellManipulator.fromEnvironment(pipeline).toString().contains("bar"))

		manipulator.restore()
		assertEquals(ShellManipulator.fromEnvironment(pipeline).toString(), "[]")
	}

	void testConstructAndCleanup() {
		ShellManipulator manipulator = ShellManipulator.fromEnvironment(new MockedPipelineScript())
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
		assertEquals(manipulator.manipulations.size(), 0)
	}

	void testEmptyConstruction() {
		ShellManipulator manipulator = ShellManipulator.fromEnvironment(new MockedPipelineScript())
		String masterFileName = manipulator.constructScriptStack("foobar")
		String masterFileContent = new String(Files.readAllBytes(Paths.get(masterFileName)))
		assertEquals(masterFileContent, "foobar")
		manipulator.restore()
	}

	void testNoMultipleConstruction() {
		ShellManipulator manipulator = ShellManipulator.fromEnvironment(new MockedPipelineScript())
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

		ShellManipulator manipulator = ShellManipulator.fromEnvironment(pipeline)

		shouldFail(InternalError) {
			manipulator.constructScriptStack("foobar")
		}

		manipulator.restore()
	}

	void testParallelBranches() {
		MockedPipelineScript pipeline = new MockedPipelineScript()
		ShellManipulator manipulator = ShellManipulator.fromEnvironment(pipeline)

		// Sequential code path
		manipulator.add("foo", "")
		assertEquals("foo", manipulator.manipulations[0][0])

		// Single parallel branch
		pipeline.parallelBranchIds = ["branchId"]
		manipulator = ShellManipulator.fromEnvironment(pipeline)
		assertEquals("foo", manipulator.manipulations[0][0])
		manipulator.add("bar", "")
		assertEquals(2, manipulator.manipulations.size())
		assertEquals("bar", manipulator.manipulations[0][0])
		assertEquals("foo", manipulator.manipulations[1][0])
		manipulator.restore()
		assertEquals(1, manipulator.manipulations.size())

		// Back to sequential code path
		pipeline.parallelBranchIds = []
		manipulator = ShellManipulator.fromEnvironment(pipeline)
		assertEquals(1, manipulator.manipulations.size())
		assertEquals("foo", manipulator.manipulations[0][0])

		// Nested parallel branches
		pipeline.parallelBranchIds = ["innerBranchId", "middleBranchId", "outerBranchId"]
		manipulator = ShellManipulator.fromEnvironment(pipeline)
		assertEquals("foo", manipulator.manipulations[0][0])
		manipulator.add("baz", "")
		assertEquals(2, manipulator.manipulations.size())
		assertEquals("baz", manipulator.manipulations[0][0])
		assertEquals("foo", manipulator.manipulations[1][0])
		manipulator.restore()
		assertEquals(1, manipulator.manipulations.size())
	}
}
