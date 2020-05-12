/**
 * Evaluate the default container path to be used.
 *
 * This usually evaluates to {@code /containers/stable/latest}, this value can however be overwritten by a single line
 * in the commit message of gerrit-triggered commits starting with {@code In-Container: /some/other/image}.
 * Both, the default path as well as the commit-message based parameter can be overwritten by the build parameter
 * {@code OVERWRITE_DEFAULT_CONTAINER_IMAGE}. It is automatically added within this step.
 *
 * Multiple such lines will result in an {@code IllegalArgumentException}.
 */
String call() {
	return getDefaultFixturePath(defaultPathCanonical: "/containers/stable/latest",
	                             commitKey: "In-Container",
	                             parameterName: "OVERWRITE_DEFAULT_CONTAINER_IMAGE")
}
