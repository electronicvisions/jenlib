package org.electronicvisions.jenlib

import java.nio.file.Paths

import static java.util.UUID.randomUUID

/**
 * Simple wrapper for the Waf build tool in Jenkins Pipelines
 *
 * The class can be used as follows:
 * Manual Resource Management (works in Jenkins' Groovy dialect)
 * <pre>
 * 	waf = new Waf(this, [gerrit_changes: "GERRIT_CHANGES", gerrit_host: "GERRIT_HOST", gerrit_port: "GERRIT_PORT", gerrit_user: "GERRIT_USER"])
 * 	waf.build()
 *	withEnv(["PATH+WAF=" + waf.path]) {
 *		jesh "waf do_something"
 *	}
 *	waf.clean()
 * </pre>
 */
class Waf implements Serializable {

	/**
	 * Gerrit changeset in use
	 */
	public final String gerrit_changes

	/**
	 * Gerrit host
	 */
	public final String gerrit_host

	/**
	 * Gerrit port
	 */
	public final int gerrit_port

	/**
	 * Hidden Waf build directory
	 */
	public final String waf_dir

	/**
	 * Path to waf executable
	 */
	public final String path

	/**
	 * State of Waf instance
	 */
	public boolean built = false

	/**
	 * Enable debug output during waf build
	 */
	private boolean debug = false

	/**
	 * Pipeline context
	 */
	def steps

	/**
	 * Constructor for {@link Waf}
	 *
	 * @param steps Pipeline steps, to be passed as {@code this} from the calling pipeline
	 * @param options Map of options for waf build
	 */
	Waf(steps, Map<String, Object> options = [:]) {
		this.steps = steps
		this.debug = options.get('debug', false)

		this.gerrit_changes = options.get('gerrit_changes', steps.env.GERRIT_CHANGE_NUMBER)
		this.gerrit_host = options.get('gerrit_host', steps.env.GERRIT_HOST)

		if (steps.env.GERRIT_PORT != null) { // needed because parseInt can't parse 'null'
			this.gerrit_port = options.get('gerrit_port', Integer.parseInt(steps.env.GERRIT_PORT))
		} else {
			this.gerrit_port = options.get('gerrit_port', 22) // default to 22 for ssh, if neither in env nor set.
		}

		waf_dir = Paths.get(steps.pwd([waf_dir: true]), "jenkins_waf_" + randomUUID().toString()).toString()
		path = Paths.get(waf_dir.toString(), "bin").toString()
	}

	/**
	 * Build the Waf executable.
	 */
	void build() throws IllegalStateException {
		if (built) {
			throw new IllegalStateException("Waf was already built.")
		}
		debugShell "mkdir ${waf_dir}"
		debugShell "mkdir ${path}"
		debugShell "cd ${waf_dir} && " +
		           "git clone git@gitviz.kip.uni-heidelberg.de:waf.git -b symwaf2ic symwaf2ic"
		debugShell "cd ${waf_dir}/symwaf2ic && " +
		           "make"
		if (gerrit_changes != null) {
			if (gerrit_host != null) {
				debugShell "cd ${waf_dir} && " +
				           "./symwaf2ic/waf setup --project waf@symwaf2ic " +
				           "--clone-depth 1 " +
				           "--gerrit-changes=${gerrit_changes} " +
				           "--gerrit-url=ssh://${gerrit_host}:${gerrit_port}"
			} else {
				debugShell "cd ${waf_dir} && " +
				           "./symwaf2ic/waf setup --project waf@symwaf2ic " +
				           "--clone-depth 1 " +
				           "--gerrit-changes=${gerrit_changes}"
			}
			debugShell "cd ${waf_dir}/waf && " +
			           "make"
			debugShell "cd ${waf_dir} && " +
			           "cp waf/waf bin/"
		} else {
			debugShell "cd ${waf_dir}/symwaf2ic && " +
			           "make"
			debugShell "cd ${waf_dir} && " +
			           "cp symwaf2ic/waf bin/"
		}
		built = true
	}

	/**
	 * Clean the Waf build.
	 */
	void clean() throws IllegalStateException {
		if (!built) {
			throw new IllegalStateException("Waf was not yet built.")
		}
		if (Paths.get(waf_dir).getNameCount() != 0) {
			debugShell "rm -rf ${waf_dir}"
		}
		built = false
	}

	/**
	 * Shell wrapper that cuts all stdout if {@link Waf#debug} is <code>false</code>
	 */
	private void debugShell(String command) {
		if (debug) {
			steps.jesh command
		} else {
			command.replace("&&", "> /dev/null &&")
			command += "> /dev/null"
			steps.jesh "${command}"
		}
	}
}