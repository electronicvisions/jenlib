/**
 * Encode a given String in Base64.
 *
 * @param messae The String to encode.
 */
def static call(String message) {
	return Base64.getMimeEncoder().encodeToString(message.getBytes("UTF-8"))
}
