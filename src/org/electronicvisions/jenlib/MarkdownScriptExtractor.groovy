package org.electronicvisions.jenlib

import com.cloudbees.groovy.cps.NonCPS

import java.util.regex.Matcher
import java.util.regex.Pattern;

/**
 * Extractor for code blocks in Markdown files.
 */
class MarkdownScriptExtractor {

	/**
	 * Path to the Markdown file to be passed.
	 */
	private final String markdownFilePath;

	/**
	 * Pipeline context to be used.
	 */
	private final def pipelineContext;

	/**
	 * Constructor for {@link MarkdownScriptExtractor}.
	 * @param pipelineContext Pipeline context to be used.
	 * @param markdownFilePath Path to the Markdown file to be parsed
	 */
	MarkdownScriptExtractor(pipelineContext, String markdownFilePath) {
		this.pipelineContext = pipelineContext
		this.markdownFilePath = markdownFilePath
	}

	/**
	 * Extract all code blocks of a given type.
	 * @param blockType Type/language of code blocks to be extracted (e.g. "shell", "cpp", ...)
	 * @return List of all code blocks found.
	 */
	List<String> getBlocks(String blockType) {
		final String markdown = pipelineContext.readFile(markdownFilePath)
		return markdown2blocks(markdown, blockType)
	}

	@NonCPS
	private static List<String> markdown2blocks(String markdown, String blockType) {
		final String regex = "^```${blockType}\\s*\\R(?<content>(?:.*\\R)*?)```\\s*\$"
		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE)
		final Matcher matcher = pattern.matcher(markdown);

		List<String> result = new ArrayList<String>()
		while (matcher.find()) {
			String blockContent = matcher.group("content")
			// Remove trailing newlines
			blockContent = blockContent.replaceAll("(?s)\\R+\$", "")
			result.add(blockContent)
		}

		return result
	}
}
