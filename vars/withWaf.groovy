import org.electronicvisions.jenlib.Waf

/**
 * Provide the waf build tool.
 *
 * Example:
 * 	<pre>
 * 	    withWaf() {
 * 	        jesh "waf setup --project some_project"
 * 	    }
 *	</pre>
 *
 * @param options Map of options to provide to Waf class.
 */
def call(Closure content) {
	if (env.JENLIB_SEQUENTIAL_BRANCH_WITH_WAF?.toBoolean()) {
		// Waf built during sequential branches may be used in parallel branches,
		// because the environment is inherited.
		content()
		return
	}

	Waf waf
	runOnSlave(label: "frontend") {
		waf = new Waf(this, false)
		waf.build()
	}
	withEnv(["PATH+WAF=${waf.path}", "SINGULARITYENV_PREPEND_PATH+WAF=${waf.path}"]) {
		if (!inParallelBranch()) {
			// If we are in the sequential branch, it's OK to enable the waf cache.
			// We do not enable the cache in parallel branches, because other parallel branches may not
			// share the environment.
			env.JENLIB_SEQUENTIAL_BRANCH_WITH_WAF = true
		}
		try {
			content()
		} finally {
			if (!inParallelBranch()) {
				env.JENLIB_SEQUENTIAL_BRANCH_WITH_WAF = false
			}
		}
	}
}

/**
 * Compute if we currently are within a parallel branch.
 */
private boolean inParallelBranch(){
	return getCurrentParallelBranchIds().size() != 0
}