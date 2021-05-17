import org.electronicvisions.jenlib.MarkdownScriptExtractor

import static java.util.UUID.randomUUID

/**
 * Generate a collection of pipeline stages from a given Markdown file.
 *
 * The content of each code block is executed in a single shell step.
 *
 * @param options Keys:
 *     <ul>
 *         <li><b>markdownFilePath:</b> Path to the evaluated Markdown file.</li>
 *         <li><b>blockType:</b> Block type to be extracted. Since all content is executed in a shell step, you'll most
 *                               probably want to use "shell" or "bash" here.</li>
 *     </ul>
 */
def call(Map<String, Object> options) {
	String markdownFilePath = options.get("markdownFilePath")
	if (markdownFilePath == null) {
		throw new IllegalArgumentException("'markdownFilePath is a mandatory argument.")
	}

	String blockType = options.get("blockType")
	if (blockType == null) {
		throw new IllegalArgumentException("'blockType' is a mandatory argument.")
	}

	runOnSlave(label: "frontend") {
		MarkdownScriptExtractor extractor = new MarkdownScriptExtractor(this, markdownFilePath)

		extractor.getBlocks(blockType).eachWithIndex { script, id ->
			stage("${blockType} block, #${id}") {
				// Write the extracted block to a temporary file, so we don't have to worry about escaping anything
				String tempFile = "${pwd(tmp: true)}/jenlib_${randomUUID().toString()}.sh"
				writeFile(file: tempFile, text: script)
				jesh("cat ${tempFile} | bash")
			}
		}
	}
}
