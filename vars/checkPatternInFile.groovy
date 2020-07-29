/**
 * Make sure that some regular expression pattern does occur in a given file.
 * The pattern is passed to {@code grep -P} and may make use of Perl's RegEx dialect.
 *
 * Missing files fail the build.
 *
 * @param regexPattern Pattern to be searched for
 * @param filename File the pattern is searched in
 */
void call(String regexPattern, String filename) {
	if (!fileExists(filename)) {
		error("File $filename does not exist.")
	}

	int grepRetcode = jesh(script: "grep -P '$regexPattern' '$filename'", returnStatus: true)

	switch (grepRetcode) {
		case 0:
			// Pattern found
			break
		case 1:
			// Pattern not found
			unstable("Message '$regexPattern' not found in file: '$filename', marking build 'UNSTABLE'.")
			break
		default:
			error("An unhandled error has occurred during pattern matching.")
			break
	}
}
