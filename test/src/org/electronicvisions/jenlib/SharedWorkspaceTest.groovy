package org.electronicvisions.jenlib


class SharedWorkspaceTest extends GroovyTestCase {
	static class MockedBuild {
		static Object rawBuild = MockedBuild

		static String getDisplayName() {
			return "specialproject"
		}

		static Object getParent() {
			return MockedBuild
		}

		static boolean isBuilding() {
			return true
		}

		static String getExternalizableId() {
			return "some/nested/specialproject#123"
		}

		static def fromExternalizableId(String id) {
			if (!(id ==~ /^.+#\d+$/)) {
				throw new IllegalArgumentException()
			}
			if (id == getExternalizableId()) {
				return MockedBuild
			}
			return null
		}
	}

	void setUp() {
		// Make sure the original build getter works, this will always be null in
		// unittest environments, so we can't test much here.
		SharedWorkspace.getBuild("foobar#42")

		// Patch it to use MockedBuild
		SharedWorkspace.metaClass.static.getBuild = { String id -> return MockedBuild.fromExternalizableId(id) }
	}

	class MockedPipelineScript {
		static currentBuild = MockedBuild
		private final boolean emptyDirlist

		MockedPipelineScript(boolean emptyDirlist) {
			this.emptyDirlist = emptyDirlist
		}

		def jesh(Map<String, Object> options) {
			String command = options["script"]

			if (command.startsWith("rm -rf")) {
				assertFalse("Nothing should be removed when dirlist is empty", emptyDirlist)
				assertFalse("specialproject is removed!", command.contains("specialproject"))
				assertTrue("DELETE_ME* is not removed!", command.contains("DELETE_ME"))
				return
			}

			if (options.get("returnStdout", false)) {
				if (command.startsWith("ls -1")) {
					if (emptyDirlist) {
						return ""
					}

					return "DELETE_ME\n" +                   // wrong general format    => delete
					       "DELETE_ME.foo\n" +               // not a build number      => delete
					       "DELETE_ME.REVMRVRFX01FIzQy\n" +  // correct but not special => delete
					       "specialproject.c29tZS9uZXN0ZWQvc3BlY2lhbHByb2plY3QjMTIz\n" +
					       "specialproject.c29tZS9uZXN0ZWQvc3BlY2lhbHByb2plY3QjMTIz@tmp\n " +
					       "specialproject.c29tZS9uZXN0ZWQvc3BlY2lhbHByb2plY3QjMTIz__tmp"
				}
			}

			throw new Error("Unrecognized jesh command! ${options}")
		}

		def jesh(String command) {
			return jesh(script: command)
		}

		void echo(String message) {
			println(message)
		}

		boolean isAsicJenkins() {
			return false
		}
	}

	void testGetWorkspace() {
		MockedPipelineScript pipeline = new MockedPipelineScript(false)
		File workspace = new File(SharedWorkspace.getWorkspace(pipeline))
		assertEquals("specialproject.c29tZS9uZXN0ZWQvc3BlY2lhbHByb2plY3QjMTIz",
		             workspace.getName())
	}

	void testCleanup() {
		MockedPipelineScript pipeline = new MockedPipelineScript(false)
		SharedWorkspace.cleanup(pipeline)
	}

	void testEmptyCleanup() {
		MockedPipelineScript pipeline = new MockedPipelineScript(true)
		SharedWorkspace.cleanup(pipeline)
	}
}
