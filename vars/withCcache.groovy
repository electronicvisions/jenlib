import org.electronicvisions.jenlib.SharedWorkspace

/**
 * Run a section of code using ccache.
 *
 * @param options Map of (environment) options passed to ccache:
 *                <ul>
 *                    <li><b>ccachePath</b> (optional), defaults to <code>/usr/lib/ccache</code></li>
 *                    <li><b>ccacheDir</b> (optional), defaults to <code>/scratch/jenkins/ccache/$JOB_BASE_NAME</code></li>
 *                    <li><b>ccacheTmpdir</b> (optional), defaults to <code>/scratch/jenkins/ccache/$JOB_BASE_NAME</code></li>
 *                    <li><b>ccacheBasedir</b> (optional), defaults to <code>$WORKSPACE</code> if set, else to the shared workspace.</li>
 *                    <li><b>ccacheMaxsize</b> (optional), defaults to <code>5.0G</code></li>
 *                    <li><b>ccacheNoHashDir</b> (optional), defaults to <code>true</code>. Has to be boolean.</li>
 *                    <li><b>printStats</b> (optional), defaults to <code>true</code>. Print ccache states before/after content execution.</li>
 *                </ul>
 * @param content Closure to be run with ccache
 */

void call(Map options = [:], Closure content) {

	/*
	 * Heuristic for finding the default ccache base dir relative to which all paths are modified.
	 * If this code runs within a node allocation (and $WORKSPACE is therefore set), the current workspace
	 * is used.
	 * If we run outside a node allocation, the {@link SharedWorkspace} is used.
	 * */
	String defaultBasedir
	try {
		defaultBasedir = $WORKSPACE
	} catch (MissingPropertyException ignored) {
		defaultBasedir = SharedWorkspace.getWorkspace(this)
	}

	String ccachePath = options.get("ccachePath", "/usr/lib/ccache")
	String ccacheDir = options.get("ccacheDir",
	                               isAsicJenkins() ? "/jenkins/ccache_s5/$JOB_BASE_NAME" :
	                                                 "/jenkins/ccache_f9/$JOB_BASE_NAME")
	String ccacheTmpdir = options.get("ccacheTmpdir", "$ccacheDir/tmp")
	String ccacheBasedir = options.get("ccacheBasedir", defaultBasedir)
	String ccacheMaxsize = options.get("ccacheMaxsize", "5.0G")
	def ccacheNoHashDir = options.get("ccacheNoHashDir", true)
	boolean printStats = options.get("printStats", true)

	if (!(ccacheNoHashDir instanceof Boolean)) {
		throw new IllegalArgumentException("ccacheNoHashDir has to be boolean.")
	}

	withEnv(["SINGULARITYENV_PREPEND_PATH+OLDPATH=$ccachePath",
	         "PATH+OLDPATH=$ccachePath",
	         "CCACHE_DIR=$ccacheDir",
	         "CCACHE_NODEBUG=true",
	         "CCACHE_TEMPDIR=$ccacheTmpdir",
	         "CCACHE_BASEDIR=$ccacheBasedir",
	         "CCACHE_MAXSIZE=$ccacheMaxsize",
	         "CCACHE_NOHASHDIR=" + (ccacheNoHashDir ? "yes" : "no")]) {

		if(printStats) {
			runOnSlave(label: "frontend && singularity") {
				inSingularity(app: "dev-tools") {
					// we need some stable environment with ccache binaries to read out stats
					jesh("ccache -p")  // --show config does not work for all versions of ccache
					jesh("ccache --show-stats")
				}
			}
		}

		content()

		if(printStats) {
			runOnSlave(label: "frontend && singularity") {
				inSingularity(app: "dev-tools") {
					jesh("ccache --show-stats")
				}
			}
		}
	}
}
