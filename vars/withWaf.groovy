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
def call(Map<String, Object> options = [:], Closure content) {
	Waf waf
	runOnSlave(label: "frontend") {
		waf = new Waf(this, options)
		waf.build()
	}
	withEnv(["PATH+WAF=${waf.path}", "SINGULARITYENV_PREPEND_PATH+WAF=${waf.path}"]) {
		content()
	}
}
