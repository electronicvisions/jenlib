/**
 * Decode a given String from Base64.
 *
 * @param input The Base64-encoded string to decode
 */
def static call(String input) {
	return new String(Base64.getMimeDecoder().decode(input))
}
