package org.electronicvisions

import static java.util.UUID.randomUUID


/**
 * Simple wrapper for Singularity containers in Jenkins Pipelines
 *
 * The class can be used in two different ways:
 * <ul>
 * 	<li> Manual Resource Management (works in Jenkins' Groovy dialect)
 * 		<pre>
 * 			container = new SingularityInstance(this, "/my/container")
 * 			container.exec("hostname")
 * 			container.stop()
 * 		</pre>
 * 	<li> Managed Resource (doesn't work in Jenkins' Groovy dialect [2018-07-25])
 * 		<pre>
 * 			new SingularityInstance(this, "/my/container").withCloseable { container ->
 * 				container.exec("hostname")
 * 			}
 * 		</pre>
 * </ul>
 */
class SingularityInstance implements Serializable, Closeable {

	/**
	 * Unique identifier of a single instance
	 */
	public final String instance_id

	/**
	 * Path to the singularity container in use
	 */
	public final String image

	/**
	 * Container's app to be used when executing commands
	 */
	public final String app

	/**
	 * Additional singularity startup parameters
	 */
	public final String singularity_args

	/**
	 * State of the container instance
	 */
	public boolean running = false

	/**
	 * Pipeline context
	 */
	def steps

	/**
	 * Constructor for {@link SingularityInstance}
	 *
	 * @param steps: Pipeline steps, to be passed as {@code this} from the calling pipeline
	 * @param image: Path to a singularity container
	 * @param app: App to be run
	 * @param singularity_args: Additional arguments to the singularity instance creation
	 */
	SingularityInstance(steps, String image, String app, String singularity_args = "") {
		this.steps = steps
		this.image = image
		this.app = app
		this.singularity_args = singularity_args

		// Singularity does not like "-" in container names
		instance_id = "jenkins_" + randomUUID().toString().replace("-", "_")
	}

	/**
	 * Start the singularity container.
	 *
	 * This has to be called on the node the actual instance is supposed to run.
	 * Make sure to call {@link #stop() at some point to prevent unused container instances.}
	 *
	 */
	void start() throws IllegalStateException {
		if (running) {
			throw new IllegalStateException("Container is already running.")
		}
		steps.sh "singularity instance.start ${singularity_args} ${image} ${instance_id}"
		running = true
	}

	/**
	 * Stop the singularity container.
	 */
	void stop() throws IllegalStateException {
		if (!running) {
			throw new IllegalStateException("Container is not running.")
		}
		steps.sh "singularity instance.stop -f ${instance_id}"
		running = false
	}

	/**
	 * Execute some command within the running container. Start it if necessary.
	 *
	 * @param command: Command to be executed
	 */
	void exec(String command) {
		if (!running) {
			start()
		}
		steps.sh "singularity exec --app ${app} instance://${instance_id} ${command}"
	}

	/**
	 * List all singularity instances on the current host
	 */
	void list_instances() {
		steps.sh "singularity instance.list || exit 0"  // Don't error if no instances are found
	}

	/**
	 * Stop the instance if it is still running when the resource is closed.
	 */
	void close() {
		if (running) {
			stop()
		}
	}
}
