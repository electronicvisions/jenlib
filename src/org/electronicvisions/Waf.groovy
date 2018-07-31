package org.electronicvisions

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
 *		sh "waf do_something"
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

		String tmp_changes
		try {
			tmp_changes = options.get('gerrit_changes', steps.env.GERRIT_CHANGE_NUMBER)
		} catch (MissingPropertyException ignored) {
			tmp_changes = options.get('gerrit_changes')
		}
		this.gerrit_changes = tmp_changes

		String tmp_host
		try {
			tmp_host = options.get('gerrit_host', steps.env.GERRIT_HOST)
		} catch (MissingPropertyException ignored) {
			tmp_host = options.get('gerrit_host')
		}
		this.gerrit_host = tmp_host

		int tmp_port
		try {
			tmp_port = options.get('gerrit_port', Integer.parseInt(steps.env.GERRIT_PORT))
		} catch (MissingPropertyException ignored) {
			tmp_port = options.get('gerrit_port', 22) // default to 22 for ssh, if neither in env nor set.
		}
		this.gerrit_port = tmp_port

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
		steps.sh "mkdir ${waf_dir}"
		steps.sh "mkdir ${path}"
		steps.sh "cd ${waf_dir} && " +
			"git clone git@gitviz.kip.uni-heidelberg.de:waf.git -b symwaf2ic symwaf2ic"
		steps.sh "cd ${waf_dir}/symwaf2ic && " +
			"make"
		if (gerrit_changes != null && gerrit_host != null && gerrit_port != null) {
			steps.sh "cd ${waf_dir} && " +
				"./symwaf2ic/waf setup --directory symwaf2ic " +
				"--clone-depth 1 " +
				"--gerrit-changes=${gerrit_changes} " +
				"--gerrit-url=ssh://${gerrit_host}:${gerrit_port}"
		}
		steps.sh "cd ${waf_dir}/symwaf2ic && " +
			"make"
		steps.sh "cd ${waf_dir} && " +
			"cp symwaf2ic/waf bin/"
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
			steps.sh "rm -rf ${waf_dir}"
		}
		built = false
	}
}
