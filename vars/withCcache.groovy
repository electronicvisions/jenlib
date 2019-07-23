/**
 * Run a section of code using ccache.
 *
 * @param options Map of (environment) options passed to ccache:
 *                <ul>
 *                    <li><b>ccachePath</b> (optional), defaults to <code>/usr/lib/ccache</code></li>
 *                    <li><b>ccacheDir</b> (optional), defaults to <code>/scratch/jenkins/ccache/$JOB_BASE_NAME</code></li>
 *                    <li><b>ccacheTmpdir</b> (optional), defaults to <code>/scratch/jenkins/ccache/$JOB_BASE_NAME</code></li>
 *                    <li><b>ccacheBasedir</b> (optional), defaults to <code>$WORKSPACE</code></li>
 *                    <li><b>ccacheMaxsize</b> (optional), defaults to <code>25.0G</code></li>
 *                    <li><b>ccacheNoHashDir</b> (optional), defaults to <code>true</code>. Has to be boolean.</li>
 *                </ul>
 * @param content Closure to be run with ccache
 */

void call(Map options = [:], Closure content) {

	ccachePath = options.get("ccachePath", "/usr/lib/ccache")
	ccacheDir = options.get("ccacheDir", "/scratch/jenkins/ccache/$JOB_BASE_NAME")
	ccacheTmpdir = options.get("ccacheTmpdir", "/scratch/jenkins/$JOB_BASE_NAME")
	ccacheBasedir = options.get("ccacheBasedir", "$WORKSPACE")
	ccacheMaxsize = options.get("ccacheMaxsize", "25.0G")
	ccacheNoHashDir = options.get("ccacheNoHashDir", true)

	if (!(ccacheNoHashDir instanceof Boolean)) {
		throw new IllegalArgumentException("ccacheNoHashDir has to be boolean.")
	}

	withEnv(["SINGULARITYENV_PREPEND_PATH+OLDPATH=$ccachePath",
	         "PATH+OLDPATH=$ccachePath",
	         "CCACHE_DIR=$ccacheDir",
	         "CCACHE_TEMPDIR=$ccacheTmpdir",
	         "CCACHE_BASEDIR=$ccacheBasedir",
	         "CCACHE_MAXSIZE=$ccacheMaxsize",
	         "CCACHE_NOHASHDIR=" + (ccacheNoHashDir ? "yes" : "no")]) {
		content()
	}
}