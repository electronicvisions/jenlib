import org.electronicvisions.jenlib.SharedWorkspace

/**
 * Ensure that some code runs on the specified slave.
 *
 * In contrast to using {@code node ( )} directly, no new executor is allocated in case we are already on the given node.
 *
 * {@code runOnSlave} additionally enforces a build-unique shared workspace between all nodes.
 *
 * @param target_node Target node definition. One of 'name'|'label'
 * @param content Code to be executed on the defined node
 */
def call(LinkedHashMap<String, String> target_node, Closure content) {
	String selector = target_node.keySet()[0]

	// Sanity check of supplied user options
	if ((target_node.keySet().size() != 1) | !(selector in ["name", "label"])) {
		throw new IllegalArgumentException("Exactly one of ('name'|'label') expected as node identifier.")
	}

	String sharedWorkspace = SharedWorkspace.getWorkspace(this)
	String newPwd = sharedWorkspace

	// If we already are in a jenlib-workspace, don't change the dir
	if (env.WORKSPACE == sharedWorkspace) {
		newPwd = pwd()
	} else if (env.WORKSPACE != null) {
		throw new IllegalStateException("Cannot use 'runOnSlave' inside plain 'node' steps!")
	}

	if ((selector == "name") && (target_node.get(selector) != env.NODE_NAME)) {
		node(target_node.get(selector)) {
			// Do not use 'ws' here, it locks each workspace for single-use per node
			// We override 'env.WORKSPACE' for compatibility with scripts using '$WORKSPACE'
			dir(newPwd) {
				withEnv(["WORKSPACE=${sharedWorkspace}"]) {
					content()
				}
			}
		}
	} else if ((selector == "label") && !(target_node.get(selector) in env.NODE_LABELS?.split("\\s"))) {
		node(label: target_node.get(selector)) {
			// Do not use 'ws' here, it locks each workspace for single-use per node
			// We override 'env.WORKSPACE' for compatibility with scripts using '$WORKSPACE'
			dir(newPwd) {
				withEnv(["WORKSPACE=${sharedWorkspace}"]) {
					content()
				}
			}
		}
	} else {
		// Do not use 'ws' here, it locks each workspace for single-use per node
		// We override 'env.WORKSPACE' for compatibility with scripts using '$WORKSPACE'
		dir(newPwd) {
			withEnv(["WORKSPACE=${sharedWorkspace}"]) {
				content()
			}
		}
	}
}
