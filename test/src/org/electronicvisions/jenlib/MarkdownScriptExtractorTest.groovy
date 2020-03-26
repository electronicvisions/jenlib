package org.electronicvisions.jenlib


class MarkdownScriptExtractorTest extends GroovyTestCase {
	static class MockedPipelineContext {
		static String readFile(String path) {
			return new File(path).text
		}
	}

	public static File testResourceRoot
	public static MockedPipelineContext pipeline = new MockedPipelineContext();

	void setUp() {
		File testResourceRoot = new File("test/resources/").getCanonicalFile()
		assertTrue(testResourceRoot.isDirectory())
		this.testResourceRoot = testResourceRoot
	}

	void testGetBlocks() {
		String testFile = new File(testResourceRoot, "MarkdownScriptExtractorTest.md").toString()
		MarkdownScriptExtractor extractor = new MarkdownScriptExtractor(pipeline, testFile)
		List<String> expectation = ["pwd",
		                            "echo \"Hello world\"\n\npwd"]
		assertEquals(expectation, extractor.getBlocks("shell"))
	}
}
