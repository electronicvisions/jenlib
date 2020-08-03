/**
 * Based on a plain path (e.g. <code>/my/directory</code>), find the first non-existing directory postfixed by an
 * integer and create it (e.g. <code>/my/directory_7</code>).
 *
 * @param notIncrementedPath Folder path to be enumerated, e.g. <code>/my/directory</code>.
 * @return Absolute path to the enumerated directory that has been created, e.g. <code>/my/directory_7</code>.
 */
String call(String notIncrementedPath) {
	// Make sure the parent directory exists, create it if not
	jesh("mkdir -p `dirname \"${notIncrementedPath}\"`")

	if (jesh(script: "[ -w `dirname \"${notIncrementedPath}\"` ]", returnStatus: true) != 0) {
		throw new IOException("Parent directory of ${notIncrementedPath} is not writeable!")
	}

	final String incrementedPath = jesh(returnStdout: true,
	                                    script: "num=1 && " +
	                                            "until mkdir \"${notIncrementedPath}_\$num\" 2>/dev/null; " +
	                                            "do let num++; done && " +
	                                            "readlink -f \"${notIncrementedPath}_\$num\"").trim()

	echo("[jenlib] Created enumerated directory: '${incrementedPath}'")
	return incrementedPath
}
