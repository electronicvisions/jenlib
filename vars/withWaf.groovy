import org.electronicvisions.Waf

/**
 * Provide the waf build tool.
 *
 * Example:
 * 	<pre>
 * 	    withWaf() {
 * 	        sish "waf setup --project some_project"
 * 	    }
 *	</pre>
 *
 * @param options Map of options to provide to Waf class.
 */
def call(Map<String, Object> options = [:], Closure content) {
	waf = new Waf(this, options)
	runOnSlave(label: "frontend") {
		waf.build()
	}
	withEnv(["PATH+WAF=${waf.path}", "SINGULARITYENV_PREPEND_PATH+WAF=${waf.path}"]) {
		content()
	}
}
